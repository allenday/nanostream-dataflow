<template>
    <div>
        <H1>Available pipelines</H1>
        <div class="row">
            <div class="col-sm" style="overflow-x:auto;">
                <table>
                    <thead>
                        <th>Name</th>
                        <th>Input folder</th>
                        <th>Output collection name prefix</th>
                        <th>Processing mode</th>
                        <th>Input data subscription</th>
                        <th>Jobs</th>
                        <th>Created at</th>
                        <th>Status</th>
                        <th>Autostart</th>
                        <th>Remove</th>
                    </thead>
                    <tr v-for="pipeline in pipelines">
                        <td><router-link :to="{name: 'pipeline_details', params: { pipeline_id: pipeline.id }}">{{ pipeline.pipelineName }}</router-link></td>
                        <td><a :href="getBucketFolderUrl(pipeline)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;{{ pipeline.inputFolder }}</a></td>
                        <td><a :href="getFirestoreCollectionUrl(pipeline)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;{{ pipeline.outputCollectionNamePrefix }}</a></td>
                        <td>{{ pipeline.processingMode }}</td>
                        <td><a :href="getInputDataSubscriptionUrl(pipeline)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;{{ subscriptionToShort(pipeline.inputDataSubscription) }}</a></td>
                        <td><Jobs v-bind:jobs="pipeline.jobs"/></td>
                        <td>{{ pipeline.createdAt }}</td>
                        <td>{{ pipeline.status }}</td>
                        <td><AutostartCheckbox v-bind:pipeline="pipeline"/></td>
                        <td><button type="button"
                                    v-on:click="removePipelineItem(pipeline)"
                                    class="btn btn-link"><i class="fa fa-minus-circle" aria-hidden="true"></i></button></td>
                    </tr>
                </table>
            </div>
        </div>
        <error-message v-bind:errMsg="errMsg" />
    </div>
</template>

<style scoped>
    table, th, td {
        padding-left: 10px;
    }
    table {
        width: 100%;
        border: 1px solid #ddd;
    }
    tr:nth-child(even) {
        background-color: #fefffa;
    }
</style>


<script>

    import config from '../../config.js';
    import Jobs from './pipeline_list/Jobs.vue';
    import AutostartCheckbox from "./pipeline_list/AutostartCheckbox.vue"
    import api from "../../api";
    import PipelineUtil from "../../pipeline.util";
    import ErrorMessage from './ErrorMessage.vue'


    export default {

        name: 'PipelineList',

        data() {
            return {
                pipelines: [],
                reloadPipelinesTaskId: null,
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },

        components: {
            Jobs,
            AutostartCheckbox,
            ErrorMessage,
        },

        beforeRouteLeave (to, from, next) {
            this.clearScheduledReloadPipelinesTask();
            next(true);
        },

        mounted() {
            this.getPipelinesFirstTime();
        },

        methods: {
            async getPipelinesFirstTime() {
                const loader = this.$loading.show();
                try {
                    await this.getPipelines();
                } catch (error) {
                    loader.hide();
                    this.showError(error);
                } finally {
                    loader.hide();
                }
            },
            getPipelines() {
                let that = this;

                return new Promise(function (resolve, reject) {
                    Promise.all([api.getPipelines(), api.getJobs()]).then(function (responses) {
                        let pipelines;
                        let jobs = [];
                        responses.forEach(function (data) {
                            console.log(data);
                            if (data && data.pipelines) {
                                pipelines = data.pipelines;
                            }
                            if (data && data.jobs) {
                                jobs = data.jobs;
                            }
                        });
                        if (pipelines && jobs) {
                            that.pipelines = PipelineUtil.preparePipelines(pipelines, jobs);
                             console.log(that.pipelines);
                        }
                        that.scheduleReloadPipelines();
                        resolve();
                    })
                    .catch(error => {
                        console.error('getPipelines error:', error)
                        reject(error);
                        that.showError(error);
                    });
                });
            },
            clearScheduledReloadPipelinesTask() {
                if (this.reloadPipelinesTaskId) {
                    console.log('clearScheduledReloadPipelinesTask called')
                    clearTimeout(this.reloadPipelinesTaskId); // cancel previous timeout request
                    this.reloadPipelinesTaskId = null;
                }
            },
            scheduleReloadPipelines() {
                this.clearScheduledReloadPipelinesTask();
                if (this.$route.name === 'pipeline_list') { // reload only from pipeline list page
                    this.reloadPipelinesTaskId = setTimeout(() => {
                        console.log('Call getPipelines after timeout')
                        this.getPipelines();
                    }, 30000);
                }
            },
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
            getFirestoreCollectionUrl(pipeline) {
                return "https://console.firebase.google.com/u/0/project/" + config.firebase.projectId + "/database/firestore/data~2F"
                    + pipeline.outputCollectionNamePrefix + '__statistic__' + pipeline.uploadBucketName;
            },
            getInputDataSubscriptionUrl(pipeline) {
                // projects/nanostream-test1/subscriptions/nanostream-20200212t161049707z => nanostream-20200212t161049707z
                let subscription = pipeline.inputDataSubscription.replace(new RegExp("^.+\/(.*)"), '$1');  // get part after the last slash
                return "https://console.cloud.google.com/cloudpubsub/subscription/detail/" + subscription + "?authuser=0&project=" + config.firebase.projectId;
            },
            getBucketFolderUrl(pipeline) {
                return "https://console.cloud.google.com/storage/browser/" + pipeline.uploadBucketName + "/" + pipeline.inputFolder + "/?authuser=0&project=" + config.firebase.projectId;
            },
            subscriptionToShort(fullSubscripionName) {
                return fullSubscripionName.replace(new RegExp('^.+/(.+)'), '$1')
            },
            getPipelineStatus(pipeline) {
                return PipelineUtil.getPipelineStatus(pipeline);
            },
            async removePipelineItem(pipeline) {
                console.log('removePipelineItem', pipeline)
                const loader = this.$loading.show();
                try {
                    await api.removePipeline(pipeline);
                    await this.getPipelines();
                } catch (error) {
                    console.error('removePipelineItem error', error);
                    loader.hide();
                    this.showError(error);
                } finally {
                    loader.hide();
                }
            }
        }

    }

</script>