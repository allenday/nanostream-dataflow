<template>
    <div>
        <div class="row">
            <div class="col-lg-9">
                <h2>Start pipelines</h2>
            </div>
            <div class="col-lg-3 text-right">
                <button v-on:click="addNewPipelineBlock()">Add</button>
            </div>
        </div>
        <div class="row">
            <div class="col-lg-12">
                <form class="mx-auto">
                    <div ref="container">
                    </div>
                    <div class="submit-block text-center">

                        <button type="button"
                                v-on:click="startPipeline()"
                                v-if="pipelines.length > 0"
                                class="btn btn-primary">
                            Start
                        </button>

                    </div>
                </form>
            </div>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import NewPipelineBlock from './new_pipeline/NewPipelineBlock.vue';
    import api from '../../api.js';
    import config from '../../config.js';


    var g_block_id = 0;

    function _preparePipelineData(pipelines) {
        let pipeline = {
            block_id: ++g_block_id, // identify UI pipeline block
            pipelineName: '',
            inputFolder: '',
            processingMode: 'species',
            // reference_database_location: '', // todo: remove?
            inputDataSubscription: '',
//            outputCollectionNamePrefix: '',
//            documentNamePrefix: '',
//            autostop_minutes: 5, // // todo: remove?
            pipelineAutoStart: true,
            pipelineStartImmediately: false,
            referenceNameList: _makeReferenceNameList()
        };
        pipelines.push(pipeline);


//        {"id":"pipeline-636e8807-158d-4fd8-b061-18864fbe35ab","name":"tets98","inputFolder":"hhh","outputCollectionNamePrefix":"",
// "outputDocumentNamePrefix":"","processingMode":"species","inputDataSubscription":"projects/nanostream-test1/subscriptions/nanostream-20200217t131204214z",
// "referenceNameList":"DB,DB1,DB2,DB3","pipelineAutoStart":true,"pipelineStartImmediately":false,
// "uploadBucketName":"nanostream-test1-upload-bucket","createdAt":"2020-02-17T13:12:09.584Z","jobIds":[],"lockStatus":"UNLOCKED",
// "version":"1"}

        return pipeline;
    }

    function _makeReferenceNameList() {
        let result = [];
        let referenceNames = config.general.referenceNamesList.split(/\s*,\s*/);
        referenceNames.forEach(function (referenceName) {
            result.push({name: referenceName, selected: true })
        });
        return result;
    }


    export default {
        name: 'NewPipeline',
        data() {
            return {
                pipelines: [],
            }
        },

        components: {
            NewPipelineBlock: NewPipelineBlock
        },

        created() {
            this.$vueEventBus.$on('remove-pipeline-block', this.onRemovePipelineBlock)
        },

        beforeDestroy() {
            this.$vueEventBus.$off('remove-pipeline-block')
        },

        mounted() {
            this.addNewPipelineBlock();
        },

        methods: {
            addNewPipelineBlock() {
                console.log('Add new pipeline')

                var ComponentClass = Vue.extend(NewPipelineBlock);
                var instance = new ComponentClass({
                    propsData: {pipeline: _preparePipelineData(this.pipelines)}
                });
//                instance.$slots.default = ['Click me!']
                instance.$mount(); // pass nothing

                this.$refs.container.appendChild(instance.$el)
            },
            onRemovePipelineBlock(pipeline) {
                console.log('on-remove-pipeline-block', pipeline);
                this.pipelines = this.pipelines.filter(function(p) {
                    return p.block_id !== pipeline.block_id;
                });
            },
            _createSubscriptionThanSavePipelineOptions(pipeline) {
                api.createSubscription(config.general.uploadPubSubTopic)
                    .then(
                        successResponse => {
                            if (successResponse.status != 200) {
                                // TODO: show error
                                return null;
                            } else {
                                return successResponse.json();
                            }
                        },
                        failResponse => {
                            // TODO: show error
                            return null;
                        }
                    )
                    .then((data) => {
                        console.log('data from create subscription call', data)
//                        {
//                            "name": "projects/nanostream-test1/subscriptions/p-20200205t132755.495z-44a17051-b2d7-4820-8b53-e9d7c4ebaaf9",
//                            "topic": "projects/nanostream-test1/topics/nanostream-test1-pubsub-topic",
//                            "pushConfig": {},
//                            "ackDeadlineSeconds": 10,
//                            "messageRetentionDuration": "604800s",
//                            "expirationPolicy": {
//                            "ttl": "2678400s"
//                        }
//                        }

                        if (data && data.name) {
                            pipeline.inputDataSubscription = data.name;
                            this._savePipelineOptions(pipeline);
                        } else {
                            // TODO: show error
                            return null;
                        }
                    })
            },
            _savePipelineOptions(pipeline) {
                api.saveNewPipeline(pipeline)
                    .then(
                        successResponse => {
                            if (successResponse.status != 200) {
                                // TODO: show error
                                return null;
                            } else {
                                return successResponse.json();
                            }
                        },
                        failResponse => {
                            // TODO: show error
                            return null;
                        }
                    )
                    .then((data) => {  // TODO: receive some data
                        console.log('data from saveNewPipeline call', data)
                        if (data) {
                            if (this.$route.name != 'pipeline_list') {
                                this.$router.push({name: 'pipeline_list'})
                            }
                        } else {
                            // TODO: show error
                            return null;
                        }
                    })
            },
            startPipeline() {
                console.log('startPipeline');

                let that = this;
                this.pipelines.forEach(function (pipeline) {
                    console.log(pipeline)

                    that._createSubscriptionThanSavePipelineOptions(pipeline);
                })
            },
        }


    };
</script>
<style>
</style>
