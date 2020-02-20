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
        <error-message v-bind:errMsg="errMsg" />
    </div>
</template>

<script>
    import Vue from 'vue'
    import NewPipelineBlock from './new_pipeline/NewPipelineBlock.vue';
    import api from '../../api.js';
    import config from '../../config.js';
    import ErrorMessage from './ErrorMessage.vue'


    let g_block_id = 0;

    function _preparePipelineData(pipelines) {
        let pipeline = {
            block_id: ++g_block_id, // identify UI pipeline block
            pipelineName: '',
            inputFolder: '',
            processingMode: 'species',
            inputDataSubscription: '',
            autoStopDelaySeconds: 300,
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
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },

        components: {
            NewPipelineBlock: NewPipelineBlock,
            ErrorMessage: ErrorMessage,
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

                let ComponentClass = Vue.extend(NewPipelineBlock);
                let instance = new ComponentClass({
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
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
            async startPipeline() {

                async function _createSubscriptionThanSavePipelineOptions(pipeline) {
                    let data = await api.createSubscription(config.general.uploadPubSubTopic);
                    console.log('data from create subscription call', data)
//                        {
//                            "name": "projects/nanostream-test1/subscriptions/p-20200205t132755.495z-44a17051-b2d7-4820-8b53-e9d7c4ebaaf9",
//                            "topic": "projects/nanostream-test1/topics/nanostream-test1-pubsub-topic",
//                            "pushConfig": {},
//                            "ackDeadlineSeconds": 10,
//                            "messageRetentionDuration": "604800s",
//                            "expirationPolicy": {
//                              "ttl": "2678400s"
//                            }
//                        }

                    if (data && data.name) {
                        pipeline.inputDataSubscription = data.name;
                        await _savePipelineOptions(pipeline);
                    } else {
                        console.log('Cannot create subscription: ' + data)
                        throw 'Cannot create subscription: ' + JSON.stringify(data);
                    }
                }

                async function _savePipelineOptions(pipeline) {
                    let data = await api.saveNewPipeline(pipeline);

                    console.log('data from saveNewPipeline call', data)
                    if (!data || data.error) {
                        throw 'Cannot save pipeline options: ' + data.message;
                    }
                }

                console.log('startPipeline');

                let self = this;
                const loader = this.$loading.show();
                let wasError = false;
                try {
                    for (const pipeline of this.pipelines) {
                        await _createSubscriptionThanSavePipelineOptions(pipeline)
                    }
                } catch (error) {
                    wasError = true;
                    loader.hide();
                    self.showError(error)
                } finally {
                    loader.hide();
                    if (!wasError) {
                        // redirect to pipeline list
                        if (self.$route.name != 'pipeline_list') {
                            self.$router.push({name: 'pipeline_list'})
                        }
                    }
                }
            },
        }


    };
</script>
<style>
</style>
