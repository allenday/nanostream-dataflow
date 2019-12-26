
import * as firebase from 'firebase';

function Firebase_init () {

  const config = {
    apiKey: "AIzaSyA5_c4nxV9sEew5Uvxc-zvoZi2ofg9sXfk",
    authDomain: "nanostream-dataflow.firebaseapp.com",
    databaseURL: "https://nanostream-dataflow.firebaseio.com",
    projectId: "nanostream-dataflow",
    storageBucket: "nanostream-dataflow.appspot.com",
    messagingSenderId: "500629989505"
  };
  

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
}


// import Badge from "../components/Badge";

function _findChild (node, childName) {
    let i, child;
    for (i = 0; i < node.children.length; i++) {
      child = node.children[i];
      if (child.name === childName) {
        return child;
      }
    }

    return null;
  };


  // function iterates over records
    //   assign current node to root
    //   iterate over record's taxonomy
    //     for each taxonomy level - search for existence in current node children
    //       if there is no level - create one
    //       summarize probes for current node and current record
    //       assign level as a current node
function _transform (doc) {
        let i, j;
  
        let currentNode,
          record,
          taxonomyItem,
  //        taxonomyColor,
          taxonomyLevel;
  
        const root = {
          name: 'total',
          children: []
        };
  
        for (i = 0; i < doc.sequenceRecords.length; i++) {
          currentNode = root;
          record = doc.sequenceRecords[i];
          if (record.name.split('|').length > 3) {
            record.name = record.name.split('|')[3];
          }
          record.taxonomy.push(record.name);
          for (j = 0; j < record.taxonomy.length; j++) { 
            taxonomyItem = record.taxonomy[j];
  //          taxonomyColor = record.colors[j];
            taxonomyLevel = _findChild(currentNode, taxonomyItem);
            if (!taxonomyLevel) {
              taxonomyLevel = {
                name: taxonomyItem,
                id: (1000 + i) + '-' + taxonomyItem,
  //              color: taxonomyColor,
                size: 0,
                children: []
              };
              currentNode.children.push(taxonomyLevel);
            }
            taxonomyLevel.size += record.probe;
            currentNode = taxonomyLevel;
          }
        }
  
        return [root];
      };


      export default Firebase_init;

/*
export default {
  install(Vue) {
    Vue.component(Badge.name, Badge);
    Vue.component(BaseAlert.name, BaseAlert);
    Vue.component(BaseButton.name, BaseButton);
    Vue.component(BaseInput.name, BaseInput);
    Vue.component(BaseCheckbox.name, BaseCheckbox);
    Vue.component(BasePagination.name, BasePagination);
    Vue.component(BaseProgress.name, BaseProgress);
    Vue.component(BaseRadio.name, BaseRadio);
    Vue.component(BaseSlider.name, BaseSlider);
    Vue.component(BaseSwitch.name, BaseSwitch);
    Vue.component(Card.name, Card);
    Vue.component(Icon.name, Icon);
  }
};
*/