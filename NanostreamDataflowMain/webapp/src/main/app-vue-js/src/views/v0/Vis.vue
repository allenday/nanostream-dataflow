<template>
    <div>
        <div v-if="formActive">
            <PipelineForm
                    v-on:Start="startPipeline()"
                    v-bind:pipeline="pipeline"
                    v-bind:general="general"
            />

            <div class="row message">
                <div class="col-lg-4">&nbsp;</div>
                <div class="col-lg-4" v-if="this.errorMessage">
                    <h2 class="alert alert-danger" role="alert">{{this.errorMessage}}
                    </h2>
                </div>
                <div class="col-lg-4">&nbsp;</div>
            </div>


        </div>

        <div v-else class="main-area container-fluid d-flex flex-column align-content-md-center ">
            <PipelineStatus v-bind:pipeline="pipeline" v-on:PipelineStatusUpdate="PipelineStatusUpdate()"/>

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
                                            </div>
                                            <div class="col align-middle">

                                                <select

                                                        v-if="document_list.length"
                                                        v-model="general.document_name"
                                                        @change="getRecords(general.document_name)"
                                                        name="processing_mode"

                                                        class="custom-select"
                                                        id="processing_mode">

                                                    <option v-for="options in document_list"
                                                            v-bind:value="options.text"
                                                            v-bind:selected="options.selected">
                                                        {{ options.text }}
                                                    </option>

                                                </select>

                                                <select
                                                        class="waitingForData custom-select"
                                                        id="processing_mode"
                                                        v-else>
                                                    <option>&nbsp;&nbsp;&nbsp;&nbsp;...loading...&nbsp;&nbsp;&nbsp;
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


    import * as firebase from 'firebase/index';
    import config from '../firebase.config.js';

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

                LaunchReqURL: '/launch',
                InfoReqURL: '/info',
                StopPipelineURL: '/stop',
                JobsURL: '/jobs',
                ConfigURL: '/settings',

                formActive: true,

                errorMessage: '',

                pipeline: {
                    alignment_window: 0,
                    update_frequency: 0,
                    started: true,
                    status: '',
                    name: '',
                    start_time: '',
                    job_id: '',
                },

                notifications: {
                    // topic: '',
                    subscriptions: ''
                },

                config: {},

                general: {
                    // google_account: 'account@google.com',
                    project: '',
                    bucket: '',

                    document_name: '',
                    collection_name_prefix: '',
                    document_name_prefix: '',
                    collection_name: '',
                    ref_db: ''

                },

                launch_response: '',

                location: '',

                loading: false,
                records: [], // source data to build diagram
                db: null,
                mode: '',

                diagram_name: 'Diagram-Name',
                document_list: [],
                collection_name: '',
                document_name: '',
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
                console.log('New records set loaded, ' + this.records.length + ' records')
            },

            loading() {
                console.log('======= Loading= ' + this.loading)
            },

            errorMessage() {
                setTimeout(() => {
                    this.errorMessage = ''
                }, 5000);
            }
        },

        created() {
            console.log('External CONFIG', config.config);
            this.urlPrefix = "https://" + config.config.projectId + ".appspot.com";
            // this.urlPrefix = "";
            // this.config = config.config;  // read it later in getFirebaseConfig
        },

        mounted() { // start processing here
            this.getFirebaseConfig().then(
                this.db = this.firebaseInit()
            );
            this.getJobs();
        },


        methods: {

            generateCollectionName: function (bucket) {

                console.log('generateCollectionName bucket = ' + this.general.bucket);

                // collectionNamePrefix + "__" + "statistic__" + bucket
                this.general.collection_name = 'statistic__' + bucket;
                if (this.general.collection_name_prefix) {
                    this.general.collection_name =  this.general.collection_name_prefix + '__' + this.general.collection_name;
                }
                console.log('Collection name is: ' + this.general.collection_name);
                return this.general.collection_name;


            },

            setPipelineStatus: function (response_status) {

                this.pipeline.status = response_status;
                switch (response_status) {

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

            PipelineStatusUpdate: function () {

                console.log('Pipeline status updated, started : ' + this.pipeline.started)
                if (this.pipeline.started) {
                    this.stopPipeline();
                } else {
                    this.runJob();
                }

            },

            startPipeline: function () {

                console.log('Form Submitted');
                this.launch();
            },

            firebaseInit: function () {
                if (!firebase.apps.length) {
                    console.log('Connect to DB with config:', this.config)
                    firebase.initializeApp(this.config);
                }

                const db = firebase.firestore();
                db.settings({
                    timestampsInSnapshots: true
                });
                console.log('== db init finished ==')
                return db;
            },


            getFirebaseConfig: async function () {
                console.log('GET Firebase Config')

                return this.config = config.config; // !!! read from external config file , temp fix !!!
            },

            launch: function () {
                console.log('Launch called')
                this.loading = true;

                this.general.bucket = '';

                let reqData = {
                    pipeline_name: this.pipeline.name,
                    collection_name_prefix: this.general.collection_name_prefix,
                    document_name_prefix: this.general.document_name_prefix,
                    processing_mode: this.general.ref_db
                };

                // console.log('getting data for doc: ' + this.general.document_name)
                console.log('launch params: ', reqData)

                fetch(new Request(this.urlPrefix + this.LaunchReqURL,
                    {
                        method: 'POST',
                        body: this.encodeURLData(reqData)

                    }))
                    .then(
                        successResponse => {
                            if (successResponse.status != 200) {
                                this.errorMessage = "launch:ERROR, Response Status = " + successResponse.status;
                                return null;
                            } else {
                                return successResponse.json();
                            }
                        },
                        failResponse => {
                            this.errorMessage = "launch:ERROR";
                            return null;
                        }
                    )
                    .then((data) => {
                        if (data) {
                            this.launch_response = data;
                            console.log('data from LAUNCH call', data)

                            this.general.project = data.job.projectId;
                            this.pipeline.job_id = data.job.id;
                            this.pipeline.name = data.job.name;
                            this.pipeline.start_time = data.job.startTime;
                            this.location = data.job.location;
                            this.formActive = false;
                            this.getPipelineInfo();
                        } else {
                            return null;
                        }
                    })
                    .then(this.getRecords())
                    .then(() => this.loading = false)
            },


            getJobs: function () {
                console.log('Get jobs called')
                fetch(this.urlPrefix + this.JobsURL)
                    .then(
                        successResponse => {
                            if (successResponse.status != 200) {
                                return null;
                            } else {
                                return successResponse.json();
                            }
                        },
                        failResponse => {
                            this.errorMessage = "getJobs:ERROR";
                            return null;
                        }
                    )
                    .then((data) => {
                        console.log('JOBS response data:', data)
                        if (!data.jobs.length) {
                            this.formActive = true;
                            console.log('Job list is empty');
                            this.errorMessage = "getJobs:ERROR, data:" + JSON.stringify(data);
                        } else {
                            const lastJobCreationTime = d3.max(data.jobs, d => d.createTime),
                                lastJob = data.jobs.find(d => d.createTime == lastJobCreationTime);
                            this.pipeline.job_id = lastJob.id;
                            this.pipeline.name = lastJob.name;
                            this.location = lastJob.location;
                            this.general.project = lastJob.projectId;
                            console.log('last job status=' + lastJob.currentState + ',id:' + lastJob.id)
                            this.getPipelineInfo();
                            this.loading = false;
                            this.formActive = false;
                        }
                    })

            },

            runJob: function () {
                this.formActive = true;
                this.document_list = [];
                this.collection_name = '';
                this.document_name = '';
                console.log('run new job');
            },


            stopPipeline: function () {

                console.log('STOP Pipeline called: ' + this.StopPipelineURL + '?jobId=' + this.pipeline.job_id + '&location=' + this.location)
                fetch(this.urlPrefix + this.StopPipelineURL + '?jobId=' + this.pipeline.job_id + '&location=' + this.location, {method: 'POST'})
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
                    .then((data) => {
                        console.log('Stop Pipeline response data:', data)
                        this.pipeline.started = false;
                    })
                //         .then(this.getRecords)


            },


            encodeURLData: function (params) {

                const formData = new FormData();
                Object.keys(params).map((key) => {
                    formData.append(key, params[key])
                });
                return formData;

                /*return  Object.keys(params).map((key) => {
                    return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
                }).join('&');*/

            },

            reloadPipelineInfo: function () {
                setTimeout(() => {
                    console.log('Call getPipelineInfo after timeout')
                    this.getPipelineInfo();
                }, 5000);
            },

            getPipelineInfo: function () {

                console.log('GetPipelineInfo called, jobId=' + this.pipeline.job_id + '&location=' + this.location)

                fetch(this.urlPrefix + this.InfoReqURL + '?jobId=' + this.pipeline.job_id + '&location=' + this.location)
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
                    .then((data) => {
                        console.log('Response from Info:', data)
                        console.log('Current Pipeline State: ' + data.currentState)
                        this.setPipelineStatus(data.currentState);

                        function all_data_present() {
                            return data && data.environment && data.environment.sdkPipelineOptions && data.environment.sdkPipelineOptions.options &&
                                data.pipelineDescription && data.pipelineDescription.displayData;
                        }

                        if (all_data_present()) {
                            let options = data.environment.sdkPipelineOptions.options;
                            let pipDataExtra = data.pipelineDescription.displayData;
                            // this.general.bucket = pipDataExtra ? pipDataExtra.find(k => k.key == 'resultBucket').strValue : 'undefined'; // if PENDING, bucket is not defined ?
                            let outputCollectionNamePrefixElement = pipDataExtra.find(k => k.key == 'outputCollectionNamePrefix');
                            if (outputCollectionNamePrefixElement) {
                                this.general.collection_name_prefix = outputCollectionNamePrefixElement.strValue;
                                console.log('collection_name_prefix', this.general.collection_name_prefix);
                                this.general.ref_db = options.processingMode;
                                this.notifications.subscriptions = options.inputDataSubscription;
                                this.pipeline.alignment_window = options.alignmentWindow;
                                this.pipeline.update_frequency = options.statisticUpdatingDelay;
                                this.pipeline.start_time = data.startTime;
// TODO: Fix bucket name when appropriate data is available
//                                this.general.bucket = options.resultBucket;
//                                if (this.general.bucket.match(/^gs:/)) {
//                                    this.general.bucket = this.general.bucket.split('/')[2];
//                                    console.log('NEW bucket = ' + this.general.bucket)
//                                    this.generateCollectionName(this.general.bucket);
//                                }
                                this.general.bucket = "fix_bucket_name";
                                this.generateCollectionName(this.general.bucket);

                                this.getDocs();
                            } else {
                                this.reloadPipelineInfo();
                            }
                        } else {
                            this.reloadPipelineInfo();
                        }
                    })
            },


            getDocs: function () {
                console.log('Get docs called (collection: ' + this.general.collection_name + ')');
                this.document_list = [];

                this.db.collection(this.general.collection_name).get().then(doc => {
                    if (doc.docs) {
                        doc.docs.forEach(d => this.document_list.push({
                            selected: this.general.document_name == d.id,
                            value: d.id,
                            text: d.id
                        }));
                        console.log('DOCUMENT-LIST Length:', this.document_list.length);
                        if (this.document_list.length <= 0) {
                            setTimeout(() => {
                                console.log('Try get doc list again');
                                this.getDocs();
                            }, 15000);
                        } else {
                            if (!this.general.document_name) {
                                // select the first document, get data for it
                                this.general.document_name = this.document_list[0].value;
                                this.document_list[0].selected = true;
                                this.getRecords();
                            }

                        }
                    }
                });
            },

            getRecords: function () {
                console.log('Get Records called');

                let collection = this.general.collection_name;
                if (this.document_list.length > 0 && this.general.document_name) {
                    let docname = this.general.document_name;

                    // console.log('Reading firebase', this.document_list[0].value)
                    console.log(`Reading firebase: docname = ${docname}, collection = ${collection}`);

                    this.db.collection(collection).doc(docname)
                        .onSnapshot((doc) => {
                            this.records = this.transform(doc.data());
                            console.log('got data:' + this.records.length + ' records', this.records)
                            this.loading = false;
                            //     this.formActive = false;
                        });
                }
            },

            findChild: function (node, childName) {
                let i, child;
                for (i = 0; i < node.children.length; i++) {
                    child = node.children[i];
                    if (child.name === childName) {
                        return child;
                    }
                }

                return null;
            },


            transform: function (doc) {
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
