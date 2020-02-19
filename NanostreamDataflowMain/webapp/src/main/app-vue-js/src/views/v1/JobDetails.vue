<template>
    <div>
        <print-object :printable-object="jobDetails"></print-object>
        <error-message v-bind:errMsg="errMsg" />
    </div>
</template>
<script>
    import config from '../../config.js';
    import api from '../../api.js';
    import PrintObject from 'vue-print-object'
    import ErrorMessage from './ErrorMessage.vue'
    
    export default {
        
        name: 'JobDetails',

        data() {
            return {
                jobDetails: {},
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },
//        computed: {
//
//            gcp_bucket_url: function () {
//                return "https://console.cloud.google.com/storage/browser/" + config.general.uploadBucketName + '?authuser=2&project=' + config.firebase.projectId;
//            },
//
//        },
        components: {
            PrintObject,
            ErrorMessage,
        },

        mounted() {
            this.getJobDetails();
        },

        methods: {
            getJobDetails() {
                api.getJobDetails(this.$route.params.job_id, this.$route.params.location)
                    .then((data) => {
                        console.log('getJobDetails:', data)
                        this.jobDetails = data;
                    })
                    .catch(error => {
                        console.error('getJobDetails error:', error);
                        this.showError(error);
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
