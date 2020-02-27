<template>
    <div id="visualization-chart">
        <div class="row row-default-bg diagram-title">

            <div class="col-sm">
                <h4>Output</h4>
            </div>
            <div class="col-sm">
                <div id="document-selector-area">
                        <h4>Document :</h4>
                    <div>

                        <select
                                v-if="document_list.length"
                                v-model="general.document_name"
                                @change="getRecords(general.document_name)"
                                class="custom-select">

                            <option v-for="options in document_list"
                                    v-bind:value="options.text"
                                    v-bind:selected="options.selected">
                                {{ options.text }}
                            </option>

                        </select>

                        <select
                                class="waitingForData custom-select"
                                v-else>
                            <option>&nbsp;&nbsp;&nbsp;&nbsp;...loading...&nbsp;&nbsp;&nbsp;</option>
                        </select>


                    </div>
                </div>
            </div>


        </div>
        <div class="col d-flex mx-auto">
            <chart v-bind:records="records" v-bind:loading="loading"></chart>
        </div>
        <VisTable v-bind:records="records"/>
    </div>
</template>

<script>
    import Chart from './Chart.vue'
    import VisTable from './VisTable.vue'
    import firebase from 'firebase/app';
    import 'firebase/firestore';

    import config from '../../../config.js';

    export default {
        name: 'PipelineChartBlock',
        props: {"pipelineDetails": Object},
        data() {
            return {

                general: {
                    document_name: '',
                },

                loading: false,
                records: [], // source data to build diagram
                db: null,

                diagram_name: 'Diagram-Name',
                document_list: [],
            }
        },
        watch: {
            pipelineDetails: function (val) {
                console.log('watch pipelineDetails', val)
                this.getDocs();
            },
        },
        components: {
            Chart,
            VisTable,
        },
        created() {
            // console.log('PipelineChartBlock created')
            // this.$vueEventBus.$on('pipeline-details-received', this.onPipelineDetailsReceived)
        },

        beforeDestroy() {
            // this.$vueEventBus.$off('pipeline-details-received')
        },
        mounted() {
            this.db = this.firebaseInit();
        },
        methods: {
            firebaseInit: function () {
                if (!firebase.apps.length) {
                    console.log('Connect to DB with config:', config.firebase)
                    firebase.initializeApp(config.firebase);
                }

                const db = firebase.firestore();
                // const settings = {timestampsInSnapshots: true};
                // db.settings(settings);

                console.log('== db init finished ==')
                return db;
            },
            onPipelineDetailsReceived(value) {
                console.log('onPipelineDetailsReceived called', value)
                this.getDocs();
            },
            getCollectionName: function () {
                console.log('getcollectionName: ', this.pipelineDetails)
                let bucket = config.general.uploadBucketName;
                // Pipeline creates collection using rule:
                // collectionNamePrefix + "__" + "statistic__" + bucket
                let collection_name = 'statistic__' + bucket;
                if (this.pipelineDetails.outputCollectionNamePrefix) {
                    collection_name = this.pipelineDetails.outputCollectionNamePrefix + '__' + collection_name;
                }
                console.log('Collection name is: ' + collection_name);
                return collection_name;


            },
            getDocs: function () {
                let collection_name = this.getCollectionName();
                console.log('Get docs called (collection: ' + collection_name + ')');
                this.document_list = [];

                this.db.collection(collection_name).get().then(doc => {
                    if (doc.docs) {
                        console.log('doc.docs', doc.docs);
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

                let collection = this.getCollectionName();
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
            },

        }


    };
</script>
