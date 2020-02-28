import config from './config.js';

// const urlPrefix = "http://localhost:8080/api/v1";
const urlPrefix = "https://" + config.firebase.projectId + ".appspot.com/api/v1";
const jobsUrl = '/jobs';
const createSubscriptionURL = '/subscription/create';
const pipelinesUrl = '/pipelines';
const jobInfoUrl = '/jobs/info';
const stopJobUrl = '/jobs/stop';

function encodeURLData(params) {
    const formData = new FormData();
    Object.keys(params).map((key) => {
        formData.append(key, params[key])
    });
    return formData;
}

function _makeRequest(request) {
    return fetch(request)
        .then(async function (response) {
            if (!response.ok) {
                throw await response.json();
            }
            return response.json();
        });
}

export default {

    getJobs() {
        console.log('getJobs called');
        return fetch(urlPrefix + jobsUrl)
            .then((response) => response.json());
    },
    getPipelines() {
        console.log('getPipelines called');
        return fetch(urlPrefix + pipelinesUrl)
            .then((response) => response.json())
            // .catch(error => console.error('get piplines error', error));
    },
    _selectedReferencesToCsvString: function (referenceNameList) {
        let selectedItems = referenceNameList
            .filter(reference => reference.selected)
            .map(reference => reference.name);
        return selectedItems.join(',');
    },
    createNewPipeline(pipeline) {
        console.log('createNewPipeline called', pipeline)

        let reqData = Object.assign({}, pipeline); // clone
        reqData.referenceNameList = this._selectedReferencesToCsvString(pipeline.referenceNameList);
        reqData.uploadBucketName = config.general.uploadBucketName;

        console.log('createNewPipeline params: ', reqData)

        return _makeRequest(new Request(urlPrefix + pipelinesUrl,
            {
                headers: { "Content-Type": "application/json; charset=utf-8" },
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
        return _makeRequest(new Request(urlPrefix + pipelinesUrl + '/' + encodeURI(pipeline.id),
            {
                headers: {"Content-Type": "application/json; charset=utf-8"},
                method: 'DELETE',
                body: JSON.stringify(pipeline)
            }));
    },
    stopJob(job_id, location) {
        console.log('STOP Pipeline called: ' + stopJobUrl + '?jobId=' + job_id + '&location=' + location)
        
        return _makeRequest(new Request(urlPrefix + stopJobUrl + '?jobId=' + job_id + '&location=' + location, {method: 'POST'}))
    },    
    createSubscription(topic) {
        console.log('createSubscription called')

        let reqData = {
            topic: topic
        };

        return fetch(new Request(urlPrefix + createSubscriptionURL,
            {
                method: 'POST',
                body: encodeURLData(reqData)
            }))
            .then((response) => response.json());
    },
    getJobDetails(jobId, location) {

        console.log('getJobDetails called, jobId=' + jobId + '&location=' + location)

        return fetch(urlPrefix + jobInfoUrl + '?jobId=' + jobId + '&location=' + location)
            .then((response) => response.json());
    },
    getPipelineDetails(pipelineId) {

        console.log('getPipelineDetails called, pipelineId=' + pipelineId)

        return fetch(urlPrefix + pipelinesUrl + '/' + encodeURI(pipelineId))
            .then((response) => response.json());
    },
    
}
