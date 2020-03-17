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
                        <!--<td><router-link :to="{name: 'job_details', params: { job_id: job.id, location: job.location }}">{{ job.name }}</router-link></td>-->
                        <td><a :href="getGcpDataflowJobUrl(job)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;{{ job.name }}</a></td>
                        <td>{{ job.startTime }}</td>
                        <td>{{ job.stopTime }}</td>
                        <td>{{ job.currentState }}</td>
                        <td><a v-if="showStopButton(job)" href="#" v-on:click="stopJob(job.id, job.location)"><i class="fa fa-stop" aria-hidden="true"></i></a></td>
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
    import GcpUrl from '../../gcp_url.util.js';
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

        beforeRouteLeave (to, from, next) {
            this.clearScheduledReloadJobTask();
            next(true);
        },

        methods: {
            async makeRequestUsingLoader(callback) {
                const loader = this.$loading.show();
                try {
                    await callback();
                } catch (error) {
                    loader.hide();
                    this.showError(error);
                } finally {
                    loader.hide();
                }
            },
            async getJobsFirstTime() {
                this.makeRequestUsingLoader(() => this.getJobs());
            },
            getGcpDataflowJobUrl(job) {
                return GcpUrl.getGcpDataflowJobUrl(job);
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
                                    if (JobUtil.isJobStopped(job)) {
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
            clearScheduledReloadJobTask() {
                if (this.reloadJobsTaskId) {
                    console.log('clearScheduledReloadJobTask called')
                    clearTimeout(this.reloadJobsTaskId); // cancel previous timeout request
                    this.reloadJobsTaskId = null;
                }
            },
            reloadJobs: function () {
                this.clearScheduledReloadJobTask();
                if (this.$route.name === 'job_list') { // reload only from the current page
                    this.reloadJobsTaskId = setTimeout(() => {
                        console.log('Call reloadJobs after timeout')
                        this.getJobs();
                    }, 30000);
                }
            },
            stopJob(job_id, location) {
                console.log('stop job', job_id)

                this.makeRequestUsingLoader(() => {
                    return api.stopJob(job_id, location)
                        .then((data) => {
                            console.log('Stop Pipeline response data:', data)
                            this.setStopRequestSent(job_id);
                            this.clearScheduledReloadJobTask();
                            this.getJobs();
                        })
                });
            },
            showStopButton(job) {
                return JobUtil.isJobStarted(job) &&  !job.stopRequestSent;
            },
            setStopRequestSent(job_id) {
                this.jobs.forEach(job => {
                    if (job.id === job_id) {
                        job.stopRequestSent = true;
                    }
                });
            },
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
        }

    }

</script>
