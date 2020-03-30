import config from './config.js';
import FetchUtil from './fetch.util.js';

const urlPrefix = "http://localhost:8080/api/v1";
// const urlPrefix = "https://" + config.firebase.projectId + ".appspot.com/api/v1";
const jobsUrl = '/jobs';
const pipelinesUrl = '/pipelines';
const jobInfoUrl = '/jobs/info';
const stopJobUrl = '/jobs/stop';

export default {

    getJobs() {
        console.log('getJobs called');

        return FetchUtil.makeRequest(new Request(urlPrefix + jobsUrl,
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'GET',
            }));

    },
    getPipelines() {
        console.log('getPipelines called');

        return FetchUtil.makeRequest(new Request(urlPrefix + pipelinesUrl,
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'GET',
            }));
    },
    createNewPipeline(pipeline) {
        console.log('createNewPipeline called', pipeline)

        let reqData = Object.assign({}, pipeline); // clone
        reqData.uploadBucketName = config.general.uploadBucketName;

        console.log('createNewPipeline params: ', reqData)

        return FetchUtil.makeRequest(new Request(urlPrefix + pipelinesUrl,
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'POST',
                body: JSON.stringify(reqData)
            }));
    },
    updatePipeline(pipeline) {
        console.log('updatePipeline called', pipeline)

        return fetch(new Request(urlPrefix + pipelinesUrl,
            {
                headers: { "Content-Type": "application/json; charset=utf-8" },
                method: 'PUT',
                body: JSON.stringify(pipeline)
            }))
    },
    removePipeline(pipeline) {
        console.log('removePipeline called: ' + urlPrefix + pipelinesUrl + '/' + encodeURI(pipeline.id))

        return FetchUtil.makeRequest(new Request(urlPrefix + pipelinesUrl + '/' + encodeURI(pipeline.id),
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'DELETE',
                body: JSON.stringify(pipeline)
            }));
    },
    stopJob(job_id, location) {
        console.log('stopJob called: ' + stopJobUrl + '?jobId=' + job_id + '&location=' + location)
        
        return FetchUtil.makeRequest(new Request(urlPrefix + stopJobUrl + '?jobId=' + job_id + '&location=' + location,
            {method: 'POST'}))
    },    
    getJobDetails(jobId, location) {
        console.log('getJobDetails called, jobId=' + jobId + '&location=' + location)

        return FetchUtil.makeRequest(new Request(urlPrefix + jobInfoUrl + '?jobId=' + jobId + '&location=' + location,
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'GET',
            }));

    },
    getPipelineDetails(pipelineId) {
        console.log('getPipelineDetails called, pipelineId=' + pipelineId)

        return FetchUtil.makeRequest(new Request(urlPrefix + pipelinesUrl + '/' + encodeURI(pipelineId),
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'GET',
            }));

    },
    
}
