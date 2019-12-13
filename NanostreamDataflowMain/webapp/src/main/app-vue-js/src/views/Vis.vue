 <template>   
<div>
    <div v-if="formActive">
        <PipelineForm 
            v-on:Start="startPipeline()"
            v-bind:pipeline="pipeline"
            v-bind:general="general"
        />
                         
    </div>

    <div v-else class="main-area container-fluid d-flex flex-column align-content-md-center ">
            <PipelineStatus v-bind:pipeline="pipeline"  v-on:PipelineStatusUpdate="PipelineStatusUpdate()"/>
       
            <Configurations v-bind:pipeline="pipeline" 
                            v-bind:notifications="notifications"
                            v-bind:general="general"
                            />

          
            <div id="visualization-chart">
                        <div class="row ">
                            <div class="col">
                            <div class="row">
                             <div class="col card-header">
      	                            <div class="row row-default-bg">
                                          <div class="col-lg-6 ">
                                              <h2>Output</h2>
                                            </div>
                                            <div class="col-lg-6 ">
                                                <div id="document-selector-area" class="row row-default-bg">
                                                     <div class="col text-right align-middle">
                                              <h2>Document :</h2>
                                              </div><div class="col align-middle">

                                            	<select    
                                                    
                                                    v-if="document_list.length"                                               
                                                    v-model="general.document_name"
                                                    @change="launch(general.document_name)"  
                                                    name="processing_mode" 
                                             
                                                    class="custom-select" 
                                                    id="processing_mode">
	      	
                                                    <option  v-for="options in document_list" 
                                                        v-bind:value="options.text">                                                         
                                                          {{ options.text }}
                                                    </option>
                                                        
	      	                                    </select >

                                                 <select 
                                                    class="waitingForData custom-select" 
                                                    id="processing_mode" 
                                                    v-else >
                                                        <option>&nbsp;&nbsp;&nbsp;&nbsp;...loading...&nbsp;&nbsp;&nbsp;</option>
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
                            </div>
                            <div class="col d-flex mx-auto">   
                                <!--
                                <div v-if="loading">   
                                    <div class="spinner-border"></div>        
                                    <h2>                                    
                                        <div class="alert-info">Please wait. Data is being processed.</div>
                                    </h2>
                                </div> -->
                                <chart v-bind:records="records" v-bind:loading="loading"></chart>
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
          StopPipelineURL : 'https://upwork-nano-stream.appspot.com/stop?',
          JobsURL : 'https://upwork-nano-stream.appspot.com/jobs',
          formActive: true,

        pipeline: {
                alignment_window : 20,
                update_frequency: 30,
                started : true,
                status: '',
                name: 'id123456'

            },

        notifications : {
            topic: 'notif topic',
            subscriptions : 'notif subscr'
        },    

        general : {
            google_account : 'account@google.com',
            project : '',
            bucket : '',

            document_name : '', //resultDocument--2019-02-13T22-36-47UTC',
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
        document_list: [ ]
      }
  },


 components: {
        Chart,
        PipelineStatus,
        PipelineForm,
        Configurations,
        VisTable
    },


 watch: {
     records() {
         console.log('New records set loaded')
     },

     loading() {
        console.log('======= Loading= ' + this.loading)
     }
 },

 created() {
    this.db = this.FirebaseInit();
    console.log('== db init ==')
  },


 mounted() {
    this.getJobs();
 }, 


 methods: {



     setPipelineStatus : function(response_status) {

         this.pipeline.status = response_status;
         switch(response_status) {

            case 'JOB_STATE_QUEUED':
            case 'JOB_STATE_PENDING':
            case 'JOB_STATE_RUNNING':
                this.pipeline.started = true;
                break;

            default:
                this.pipeline.started = false;
                break;                    
         }
                 
     },

     PipelineStatusUpdate: function() {

         console.log('Pipeline status updated, started : ' + this.pipeline.started)
         if(this.pipeline.started) {
             this.stopPipeline();
         }

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


        this.loading = true;

        this.general.bucket = '';

        console.log('NEW doc name:' + this.document_name)

        let reqData = { 
                pipeline_name : this.pipeline.name,
                collection_name_prefix : this.general.collection_name,
                document_name_prefix : this.general.document_name,
                processing_mode : this.general.ref_db
            };

        console.log('getting data for doc: ' + this.general.document_name)    

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
            .then(() => this.loading = false)
    },


    getJobs: function() {

      //console.log('STOP Pipeline called: ' + this.StopPipelineURL + 'jobId=' + this.job_id + '&location=' + this.location)

        fetch( this.JobsURL )
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
                console.log('JOBS response data:', data)
            })

    },

    stopPipeline:  function() {

        console.log('STOP Pipeline called: ' + this.StopPipelineURL + 'jobId=' + this.job_id + '&location=' + this.location)

        fetch( this.StopPipelineURL + 'jobId=' + this.job_id + '&location=' + this.location)
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
                console.log('Stop Pipeline response data:', data)
//                let pipDataExtra = data.pipelineDescription.displayData;
 //               this.general.bucket = pipDataExtra.find(k => k.key == 'resultBucket').strValue;
            })
   //         .then(this.getRecords)
     

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
                console.log('Current Pipeline State: ' + data.currentState)
                this.setPipelineStatus(data.currentState);
                this.general.bucket = pipDataExtra.find(k => k.key == 'resultBucket').strValue;
            })
            .then(this.getRecords)
            .then(this.getDocs)
     
    },


    getDocs: function() {
        
        console.log('getting docs list')
        this.document_list = []; 
        this.db.collection(this.general.collection_name).get().then( doc => {              
             if(doc.docs) {
                doc.docs.forEach(d => this.document_list.push ({ value: d.id, text: d.id} ))
             }             
        })
    },

    getRecords: function() {

        this.general.collection_name = 'edta_species_sequences_statistic';
        this.general.document_name = this.general.document_name || 'resultDocument--2019-02-13T22-36-47UTC';
      
        let collection = this.$route.params.c || this.general.collection_name,
            docname = this.$route.params.d || this.general.document_name;


        console.log(`Reading firebase: docname = ${docname}, collection = ${collection}`);    
  

        this.db.collection(collection).doc(docname)
            .onSnapshot((doc) => {
                this.records = this.transform(doc.data());
  //              console.log('got data:' + this.records.length + ' records')
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
