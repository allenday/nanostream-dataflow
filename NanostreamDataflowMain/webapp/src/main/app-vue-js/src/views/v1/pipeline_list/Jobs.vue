<template>
    <div>
        <span v-for="job in jobs">
            <router-link :to="{name: 'job_details', params: { job_id: job.id, location: job.location }}">
                <i v-if="!isJobStarted(job)" class="fa fa-square" aria-hidden="true"></i>
                <i v-if="isJobStarted(job)" class="fa fa-arrow-circle-o-up" aria-hidden="true"></i>
            </router-link>
        </span>


        <!--<ul>-->
            <!--<li v-for="job in jobs">-->
                <!--<router-link :to="{name: 'job_details', params: { job_id: job.id, location: job.location }}">-->
                    <!--<i v-if="!isJobStarted(job)" class="fa fa-circle" aria-hidden="true"></i>-->
                    <!--<i v-if="isJobStarted(job)" class="fa fa-arrow-up" aria-hidden="true"></i>-->
                    <!--&lt;!&ndash;{{ job.name }}&ndash;&gt;-->
                <!--</router-link>-->
            <!--</li>-->
        <!--</ul>-->
<!--        {{ jobs }}-->
    </div>
</template>
<script>
    import PipelineUtil from '../../../pipeline.util.js'
    export default {
        name: 'PipelineListJobs',
        props: ["jobs"],
        // data() {
        // },
        methods: {
            isJobStarted(job) { 
                return PipelineUtil.isJobStarted(job);
            }
        }
    };
</script>
<style>
    span {
        margin-right: 5px;
    }
</style>
