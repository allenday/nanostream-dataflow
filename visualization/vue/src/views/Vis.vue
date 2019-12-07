 <template>   
<div>
    <div v-if="formActive">
        <PipelineForm 
            v-on:Start="startPipeline()"
            v-bind:pipeline="pipeline"
            v-bind:general="general"
        />
                         
    </div>

    <div v-else class="container-fluid d-flex flex-column align-content-md-center ">
            <PipelineStatus v-bind:pipeline="pipeline"  v-on:PipelineStatusUpdate="PipelineStatusUpdate()"/>
       
            <Configurations v-bind:pipeline="pipeline" 
                            v-bind:notifications="notifications"
                            v-bind:general="general"
                            />

          
            <div >

       
                        <div class="row ">
                            <div class="col">
                            <div class="row">
                             <div class="col card-header">
      	                            <div class="row  row-default-bg">
                                          <div class="col-lg-6 ">
                                              <h2>Output</h2>
                                              </div>
                                            <div class="col-lg-6 ">
                                                <div id="document-selector-area" class="row row-default-bg">
                                                     <div class="col text-right align-middle">
                                              <h2>Document :</h2>
                                              </div><div class="col align-middle">

                                            	<select v-model="general.document_name"  name="processing_mode" class="custom-select" id="processing_mode">
	      	
                                                    <option v-for="options in document_list" v-bind:value="options.value">
                                                            
                                                          {{ options.text }}
                                                        </option>
                                                        
	      	                                    </select>

                                                                                    
                                                </div>
                                                </div>
                                              </div>
                                            </div>
                                      </div>
                            </div>
                            <div class="row">
                            <div class="col d-flex diagram-title">
                                            <div class="col-lg-6">
                                              <h2>Diagram {{ diagram_name }}</h2>
                                              </div>
                                            </div>
                            </div><div class="col d-flex mx-auto">              
                                <h2>
                                    <div class="alert alert-info" v-show="loading">Loading...</div>
                                </h2>
                               <chart v-bind:records="records" ></chart>
                            </div>
                            </div>
                            </div>
                        </div>
    
    
    
            <VisTable v-bind:records="records"/>
    </div>

         

    </div>
</template>


<script>


import * as firebase from 'firebase';
import Chart from './Chart.vue';
import PipelineStatus from './PipelineStatus.vue';
import PipelineForm from './PipelineForm.vue';
import Configurations from './Configurations.vue';
import VisTable from './VisTable.vue';
//import d3 from "d3";



export default {
  name: "NanostreamApp",

  data() {
      return {          
          LaunchReqURL : 'https://upwork-nano-stream.appspot.com/launch',
          InfoReqURL : 'https://upwork-nano-stream.appspot.com/info?',
          formActive: true,

        pipeline: {
                alignment_window : 20,
                subscription: 1,
                started : true,
                name: 'id123456'

            },

        notifications : {
            topic: 'notif topic',
            subscriptions : 'notif subscr'
        },    

        general : {
            google_account : 'aaa@bb.cc',
            project : 'Project name',
            bucket : 'bucket NAME',

            document_name : 'resultDocument--2019-02-13T22-36-47UTC',
            collection_name: 'cassava_species_sequences_statistic',
            ref_db : 'species'

        },

        launch_response : '',
        job_id : '',
        location : '',

          sample_response: { "job": 
            { "id": "2019-12-03_02_53_07-7220802469192670302", 
            "projectId": "upwork-nano-stream", 
            "name": "pp_from_ui", 
            "type": "JOB_TYPE_STREAMING", 
            "currentStateTime": "1970-01-01T00:00:00Z", "createTime": "2019-12-03T10:53:09.177929Z", 
            "location": "us-central1", 
            "startTime": "2019-12-03T10:53:09.177929Z" } },
                    
          loading: false,
          records: [], // source data to build diagram
          db: null,
          mode : '',

          diagram_name: 'Diagram-Name',

          document_list: [ 
              {value:1, text: '3_Klebsiella.txt'},
              {value:2, text: '4_Klebsiella.txt'}
              
              ]
      }
  },


 components: {
        Chart,
        PipelineStatus,
        PipelineForm,
        Configurations,
        VisTable
    },

/*
 watch: {
        $route(to, from) {
           this.getRecords();
        }
    },
*/

 watch: {
     records() {

         console.log('New records set loaded')

     }
 },

 created() {
    this.db = this.FirebaseInit();
    console.log('== db init ==')
  },

/*
 mounted() {
    this.getRecords();
 }, 
*/

 methods: {

     PipelineStatusUpdate: function() {

         console.log('Pipeline status updated, started : ' + this.pipeline.started)

     },

    startPipeline: function() {
          console.log('Form Submitted');
          this.formActive = false;
          this.launch();
      },

    FirebaseInit : function() {

        const config = {
            apiKey: "AIzaSyA5_c4nxV9sEew5Uvxc-zvoZi2ofg9sXfk",
            authDomain: "nanostream-dataflow.firebaseapp.com",
            databaseURL: "https://nanostream-dataflow.firebaseio.com",
            projectId: "nanostream-dataflow",
            storageBucket: "nanostream-dataflow.appspot.com",
            messagingSenderId: "500629989505"
        };
  
        if (!firebase.apps.length) {
            firebase.initializeApp(config);
        }

        const db = firebase.firestore();
        db.settings({
            timestampsInSnapshots: true
        });

        return db;
    },

    launch: function(url) {

        let reqData = { 
                pipeline_name : this.pipeline.name,
                collection_name_prefix : this.general.collection_name,
                document_name_prefix : this.general.document_name,
                processing_mode : this.general.ref_db
            };

        fetch( new Request(this.LaunchReqURL, 
            { 
                method: 'POST', 
                body: JSON.stringify(reqData)                
                
                }) )
            .then(                
                successResponse => {
                    if (successResponse.status != 200) {
                        return null;
                    } else {
                        return successResponse.json();
                    }
                },
                failResponse => {
                    return null;
                }
                               
                )
            .then( (data) => {
    //            if(!data) data = vm.sample_response; /// !!!
                this.launch_response = data;
                console.log('data from api call', data)

                this.general.project = data.job.projectId;
                this.job_id = data.job.id;
                this.location = data.job.location;                
            })
            .then(this.getPipelineInfo)
    },


    getPipelineInfo:  function() {

        this.job_id = '2019-12-03_02_53_07-7220802469192670302'; // !!!
                //https://upwork-nano-stream.appspot.com/info?jobId=2019-12-03_02_53_07-7220802469192670302&location=us-central1


        fetch( this.InfoReqURL + 'jobId=' + this.job_id + '&location=' + this.location)
            .then(                
                successResponse => {
                    if (successResponse.status != 200) {
                        return null;
                    } else {
                        return successResponse.json();
                    }
                },
                failResponse => {
                    return null;
                }
                               
                )
            .then( (data) => {
                console.log('Response from Info:',data)
                let pipDataExtra = data.pipelineDescription.displayData;
                this.general.bucket = pipDataExtra.find(k => k.key == 'resultBucket').strValue;
            })
            .then(this.getRecords)
           // .then(this.getRecords)


    },

    getRecords: function() {

        this.general.collection_name = 'edta_species_sequences_statistic';
        this.general.document_name = 'resultDocument--2019-02-13T22-36-47UTC';
      
        let collection = this.$route.params.c || this.general.collection_name,
            docname = this.$route.params.d || this.general.document_name;


        console.log(`Reading firebase: docname = ${docname}, collection = ${collection}`);    
  
        this.db.collection(collection).doc(docname)
            .onSnapshot((doc) => {
                this.records = this.transform(doc.data());
                console.log('got data:' + this.records.length + ' records')

              //  var i = d3.layout.hierarchy({name:'root', children: this.records}, d => d.children)
             // if(this.loading ) this.getRecords();
                this.loading = false;
            });
    },

    findChild : function (node, childName) {
      let i, child;
      for (i = 0; i < node.children.length; i++) {
        child = node.children[i];
        if (child.name === childName) {
          return child;
        }
      }

      return null;
    },



    transform : function (doc) {
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
          taxonomyLevel = this.findChild(currentNode, taxonomyItem);
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
    }  

}


 
};
</script>
