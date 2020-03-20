<template>
    <div>
        <StatusBlock v-bind:pipelineDetails="pipelineDetails"/>
        <ChartBlock v-bind:pipelineDetails="pipelineDetails"/>
        <error-message v-bind:errMsg="errMsg" />

<!--        <div>Pipeline {{ $route.params.job_id }}</div>-->

    </div>
</template>
<script>
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
                reloadPipelineDetailsTaskId: null,
                errMsg: {
                    show: false,
                    message: ''
                },
            }
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
                    } else {
                        console.log('Cannot load pipelines or jobs', pipelines, jobs);
                        that.scheduleGetPipelineDetails();
                    }

                }).catch(error => {
                    that.showError(error);
                });
            },
            clearScheduledReloadPipelineDetailsTask() {
                if (this.reloadPipelineDetailsTaskId) {
                    console.log('clearScheduledReloadPipelineDetailsTask called')
                    clearTimeout(this.reloadPipelineDetailsTaskId); // cancel previous timeout request
                    this.reloadPipelineDetailsTaskId = null;
                }
            },
            scheduleGetPipelineDetails() {
                this.clearScheduledReloadPipelineDetailsTask();
                if (this.$route.name === 'pipeline_details') { // reload only current page
                    this.reloadPipelineDetailsTaskId = setTimeout(() => {
                        console.log('Call getPipelineDetails after timeout')
                        this.getPipelineDetails();
                    }, 30000);
                }
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
