<template>
    <div>
        <StatusBlock v-bind:pipelineDetails="pipelineDetails"/>
        <ChartBlock v-bind:pipelineDetails="pipelineDetails"/>

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

    export default {
        name: 'PipelineDetails',
        data() {
            return {
                pipelineDetails: {},
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
        },
        mounted() {
            this.getPipelineDetails();
        },
        methods: {
            _setGeneralAttributes(data) {
                if (data && data.name) {
                    this.pipelineDetails.name = data.name;
                    // this.pipelineDetails.currentState = data.currentState;
                }
            },
            _setPipelineOptions(data) {
                this.pipelineDetails.collectionNamePrefix = data.outputCollectionNamePrefix
                // if (this._isPipelineOptionsPresent(data)) {
                //     let pipDataExtra = data.pipelineDescription.displayData;
                //     let outputCollectionNamePrefixElement = pipDataExtra.find(k => k.key == 'outputCollectionNamePrefix');
                //     if (outputCollectionNamePrefixElement) {
                //         this.pipelineDetails.collectionNamePrefix = outputCollectionNamePrefixElement.strValue;
                //         console.log('collection_name_prefix', this.pipelineDetails.collectionNamePrefix);
                //     }
                //     // TODO: set other options if required
                //     // let options = data.environment.sdkPipelineOptions.options;
                //     // this.general.ref_db = options.processingMode;
                //     // this.notifications.subscriptions = options.inputDataSubscription;
                //     // this.pipeline.alignment_window = options.alignmentWindow;
                //     // this.pipeline.update_frequency = options.statisticUpdatingDelay;
                //     // this.pipeline.start_time = data.startTime;
                // }
            },
            _isPipelineOptionsPresent(data) {
                return data && data.name && data.environment && data.environment.sdkPipelineOptions && data.environment.sdkPipelineOptions.options &&
                    data.pipelineDescription && data.pipelineDescription.displayData;
            },
            getPipelineDetails() {

                let that = this;
                Promise.all([api.getPipelineDetails(this.$route.params.pipeline_id), api.getJobs()]).then(function(responses) {
                    let pipelines, jobs;
                    responses.forEach(function (data) {
                        console.log('pipeline details & jobs', data);
                        if (data && data.pipeline) {
                            pipelines = [data.pipeline]
                            // pipelines.push(data.pipeline);
                        }
                        if (data && data.jobs) {
                            jobs = data.jobs;
                        }
                    });
                    if (pipelines && jobs) {
                        that.pipelineDetails = PipelineUtil.preparePipelines(pipelines, jobs)[0];
                        // console.log('pipelineDetails', that.pipelineDetails);
                        console.log('emit pipeline-details-received event')
                        that.$vueEventBus.$emit("pipeline-details-received", that.pipelineDetails);
                    } else {
                        console.log('error', pipelines, jobs)
                    }

                    // that.reloadPipelines();
                });






                // api.getPipelineDetails(this.$route.params.pipeline_id)
                //     .then(
                //         successResponse => {
                //             if (successResponse.status != 200) {
                //                 return null;
                //             } else {
                //                 return successResponse.json();
                //             }
                //         },
                //         failResponse => {
                //             return null;
                //         }
                //     )
                //     .then((data) => {
                //         console.log('Response from pipeline details:', data)
                //         this._setGeneralAttributes(data);
                //         this._setPipelineOptions(data);
                //         console.log('emit pipeline-details-received event')
                //         this.$vueEventBus.$emit("pipeline-details-received");
                //     })
            },

        },
    };
</script>
<style>
</style>
