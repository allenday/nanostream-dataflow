<template>
    <div>
<!--        <StatusBlock v-bind:pipelineDetails="pipelineDetails"/>-->
<!--        <ChartBlock v-bind:pipelineDetails="pipelineDetails"/>-->


<!--        <div>Pipeline {{ $route.params.job_id }}</div>-->
<!--        <div><a :href="gcp_bucket_url" target="_blank"><i class="fa fa-sign-in"></i> Go to bucket</a></div>-->
<!--        {{ jobDetails }}-->
<!--        <ul v-for="jobDetail in jobDetails">-->
<!--            <li>jobDetail</li>-->
<!--        </ul>-->
        <print-object :printable-object="jobDetails"></print-object>

    </div>
</template>
<script>
    import config from '../../config.js';
    import api from '../../api.js';
    // import StatusBlock from './pipeline_details/StatusBlock.vue'
    // import ChartBlock from './pipeline_details/ChartBlock.vue'
    import PrintObject from 'vue-print-object'
    
    export default {
        name: 'JobDetails',
        data() {
            return {
                jobDetails: {},
                // pipelineDetails: {
                //     name: '',
                //     currentState: '',
                //     collectionNamePrefix: '',
                // },
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
            // StatusBlock: StatusBlock,
            // ChartBlock: ChartBlock,
            PrintObject:  PrintObject,
        },
        mounted() {
            this.getJobDetails();
        },
        methods: {
            getJobDetails() {
                api.getJobDetails(this.$route.params.job_id, this.$route.params.location)
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
                        console.log('getJobDetails:', data)
                        this.jobDetails = data;
                    })
            },

        },
    };
</script>
<style>
</style>
