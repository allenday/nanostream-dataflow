<template>
    <div>
        <H1>Available jobs</H1>
        <div class="row ">
            <div class="col-sm" style="overflow-x:auto;">
                <table>
                    <thead>
                    <th>Name</th>
                    <th>Start time</th>
                    <th>Stop time</th>
                    <th>Current state</th>
                    <th>Stop</th>
                    </thead>
                    <tr v-for="job in jobs">
                        <td><router-link :to="{name: 'job_details', params: { job_id: job.id, location: job.location }}">{{ job.name }}</router-link></td>
                        <td>{{ job.startTime }}</td>
                        <td>{{ job.stopTime }}</td>
                        <td>{{ job.currentState }}</td>
                        <td><a v-if="isJobStarted(job)" href="#" v-on:click="stopJob(job.id, job.location)"><i class="fa fa-stop" aria-hidden="true"></i></a></td>
                    </tr>
                </table>
            </div>
        </div>
        <error-message v-bind:errMsg="errMsg" />
    </div>
</template>

<style scoped>
    table, th, td {
        padding: 5px;
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

    import api from '../../api.js';
    import JobUtil from '../../pipeline.util.js'
    import ErrorMessage from './ErrorMessage.vue'


    export default {

        name: 'JobList',

        data() {
            return {
                jobs: [],
                reloadJobsTaskId: null,
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },

        mounted() {
            this.getJobsFirstTime();
        },

        components: {
            ErrorMessage,
        },

        methods: {
            async getJobsFirstTime() {
                const loader = this.$loading.show();
                try {
                    await this.getJobs();
                } catch (error) {
                    loader.hide();
                    this.showError(error);
                } finally {
                    loader.hide();
                }
            },
            getJobs() {
                let that = this;
                return new Promise(function (resolve, reject) {
                    api.getJobs()
                        .then((data) => {
                            if (data && data.jobs) {
                                // console.log(data);
                                that.jobs = data.jobs;
                                that.jobs.forEach(function (job) {
                                    if (job.currentState == "JOB_STATE_CANCELLED" || job.currentState == 'JOB_STATE_FAILED') {
                                        job.stopTime = job.currentStateTime;
                                    } else {
                                        job.stopTime = '';
                                    }
                                })
                            } else {
                                console.error('Missing jobs:', data);
                                that.jobs = [];
                            }
                            that.reloadJobs();
                            resolve();
                        })
                        .catch(error => {
                            reject(error);
                            that.showError(error);
                        });
                });
            },
            reloadJobs: function () {
                if (this.reloadJobsTaskId) {
                    console.log('Call clearTimeout')
                    clearTimeout(this.reloadJobsTaskId); // cancel previous timeout request
                    this.reloadJobsTaskId = null;
                }
                if (this.$route.name === 'job_list') { // reload only from the current page
                    this.reloadJobsTaskId = setTimeout(() => {
                        console.log('Call reloadJobs after timeout')
                        this.getJobs();
                    }, 30000);
                }
            },
            stopJob(job_id, location) {
                console.log('stop job', job_id)
                // todo: delete subscription on stop job 
                api.stopJob(job_id, location)
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
                        console.log('Stop Pipeline response data:', data)
                        this.getJobs();
                    })
            },
            isJobStarted(job) {
                return JobUtil.isJobStarted(job);
            },
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
        }

    }

</script>
