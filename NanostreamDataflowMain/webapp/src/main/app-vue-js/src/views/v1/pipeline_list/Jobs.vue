<template>
    <div>
        <span v-for="job in jobs">
            <a :href="getGcpDataflowJobUrl(job)" target="_blank">
                <i v-if="!isJobStarted(job)" class="fa fa-square" aria-hidden="true"></i>
                <i v-if="isJobStarted(job)" class="fa fa-arrow-circle-o-up" aria-hidden="true"></i>
            </a>

            <!--<router-link :to="{name: 'job_details', params: { job_id: job.id, location: job.location }}">-->
                <!--<i v-if="!isJobStarted(job)" class="fa fa-square" aria-hidden="true"></i>-->
                <!--<i v-if="isJobStarted(job)" class="fa fa-arrow-circle-o-up" aria-hidden="true"></i>-->
            <!--</router-link>-->
        </span>
    </div>
</template>
<script>
    import GcpUrl from '../../../gcp_url.util.js';
    import PipelineUtil from '../../../pipeline.util.js'

    export default {

        name: 'PipelineListJobs',

        props: ["jobs"],

        methods: {
            isJobStarted(job) { 
                return PipelineUtil.isJobStarted(job);
            },
            getGcpDataflowJobUrl(job) {
                return GcpUrl.getGcpDataflowJobUrl(job);
            },
        }
    };
</script>
<style>
    span {
        margin-right: 5px;
    }
</style>
