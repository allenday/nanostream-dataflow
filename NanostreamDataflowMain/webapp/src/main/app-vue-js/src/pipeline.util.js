export default {
    isJobStarted(job) {
        // console.log(job)
        return job.currentState === "JOB_STATE_RUNNING";
    },
    isJobStopped(job) {
        return job.currentState === "JOB_STATE_CANCELLED" || job.currentState === 'JOB_STATE_FAILED' || job.currentState === 'JOB_STATE_DRAINED'
    },
    preparePipelines(pipelines, jobs) {
        pipelines = this.substitutePipelineJobIdToAppropriateJobObject(pipelines, jobs);
        pipelines = this.setPipelineStatus(pipelines);
        return pipelines;
    },
    getPipelineStatus(pipeline) {
        
        if (!pipeline.jobs.length) {
            return 'CREATED';
        }
        if (pipeline.jobs.find(job => ['JOB_STATE_QUEUED', 'JOB_STATE_PENDING'].includes(job.currentState))) {
            return 'STARTING';
        }
        if (pipeline.jobs.find(job => ['JOB_STATE_CANCELLING', 'JOB_STATE_DRAINING'].includes(job.currentState))) {
            return 'STOPPING';
        }
        if (pipeline.jobs.find(job => ['JOB_STATE_RUNNING'].includes(job.currentState))) {
            return 'RUNNING';
        }
        return 'STOPPED'
    },
    substitutePipelineJobIdToAppropriateJobObject(pipelines, jobs) {
        let jobMap = this._jobsToMap(jobs);
        pipelines.forEach(pipeline => {
            // console.log('pipeline.jobIds', pipeline.jobIds)
            pipeline.jobs = [];
            pipeline.jobIds.forEach(function (jobId) {
                let job = jobMap.get(jobId);
                if (!job) {
                    job = {
                        id: jobId,
                        currentState: 'NOT_FOUND'
                    }
                }
                pipeline.jobs.push(job);
            });
        });
        return pipelines;
    },
    _jobsToMap(jobs) {
        let jobMap = new Map();
        jobs.forEach(function (job) {
            jobMap.set(job.id, job);
        });
        return jobMap;
    },
    setPipelineStatus(pipelines) {
        pipelines.forEach(pipeline => {
            pipeline.status = this.getPipelineStatus(pipeline);
        });
        return pipelines;
    },
    canRemovePipeline(pipeline) {
        const status = this.getPipelineStatus(pipeline);
        if (['STOPPED', 'CREATED'].includes(status)) {
            return true;
        } else {
            return false;
        }
    },
}