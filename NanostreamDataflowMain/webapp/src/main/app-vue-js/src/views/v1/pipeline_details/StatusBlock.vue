<template>
    <div>
        <div class="row ">
            <div class="col-sm">
                <h4 class="d-flex">Pipeline: {{ pipelineDetails.pipelineName }}</h4>
            </div>
            <div class="col-sm">
                <h4 class="d-flex">Status: {{ pipelineDetails.status }}</h4>
            </div>
        </div>
        <div class="row ">
            <div class="col-sm">
                <h5 class="d-flex">Collection prefix: {{ pipelineDetails.outputCollectionNamePrefix }}</h5>
            </div>
            <div class="col-sm">
                <h5 class="d-flex">Processing mode: {{ pipelineDetails.processingMode }}</h5>
            </div>
        </div>
<!--        <div class="row ">-->
<!--            <div><a :href="gcp_bucket_url" target="_blank"><i class="fa fa-sign-in"></i> Go to bucket</a></div>-->
<!--        </div>-->

    </div>
</template>

<script>
    import config from '../../../config.js';
    export default {

        name: 'PipelineDetailsStatusBlock',

        props: ["pipelineDetails"],

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

    }
</script>
