<template>
    <div>
        <StatusBlock v-bind:pipelineDetails="pipelineDetails"/>
        <ChartBlock v-bind:pipelineDetails="pipelineDetails"/>
        <error-message v-bind:errMsg="errMsg" />

<!--        <div>Pipeline {{ $route.params.job_id }}</div>-->
<!--        <div><a :href="gcp_bucket_url" target="_blank"><i class="fa fa-sign-in"></i> Go to bucket</a></div>-->

    </div>
</template>
<script>
    import config from '../../config.js';
    import api from '../../api.js';
    import StatusBlock from './pipeline_details/StatusBlock.vue'
    import ChartBlock from './pipeline_details/ChartBlock.vue'
    import PipelineUtil from '../../pipeline.util.js'
    import ErrorMessage from './ErrorMessage.vue'

    export default {

        name: 'PipelineDetails',

        data() {
            return {
                pipelineDetails: {},
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },

        computed: {

            gcp_bucket_url: function () {
                return "https://console.cloud.google.com/storage/browser/" + config.general.uploadBucketName + '?authuser=2&project=' + config.firebase.projectId;
            },

            // gcp_subscriptions_url: function () {
            //     return 'https://console.cloud.google.com/cloudpubsub/subscription/detail/'
            //         + this.general.project + '-upload-subscription?authuser=2&project=' + this.general.project;
            // },
            //
            // gcp_pipleline_url: function () {
            //     return 'https://console.cloud.google.com/dataflow/jobsDetail/locations/us-central1/jobs/' + this.pipeline.job_id + '?project=' + this.general.project + '&authuser=2'
            // },
        },

        components: {
            StatusBlock: StatusBlock,
            ChartBlock: ChartBlock,
            ErrorMessage: ErrorMessage,
        },

        mounted() {
            this.getPipelineDetails();
        },

        methods: {
            getPipelineDetails() {

                let that = this;
                Promise.all([api.getPipelineDetails(this.$route.params.pipeline_id), api.getJobs()]).then(function(responses) {
                    let pipelines, jobs;
                    responses.forEach(function (data) {
//                        console.log('pipeline details & jobs', data);
                        if (data && data.pipeline) {
                            pipelines = [data.pipeline]
                        }
                        if (data && data.jobs) {
                            jobs = data.jobs;
                        }
                    });
                    if (pipelines && jobs) {
                        that.pipelineDetails = PipelineUtil.preparePipelines(pipelines, jobs)[0];
//                        console.log('pipelineDetails', that.pipelineDetails);
//                        console.log('emit pipeline-details-received event')
//                        that.$vueEventBus.$emit("pipeline-details-received", that.pipelineDetails);
                    } else {
                        console.log('Cannot load pipelines or jobs', pipelines, jobs);
                        that.showError('Cannot load pipelines or jobs');
                    }

                }).catch(error => {
                    that.showError(error);
                });
            },
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
        },
    };
</script>
<style>
</style>
