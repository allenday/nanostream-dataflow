import config from './config.js';

// const urlPrefix = "http://localhost:8080/api/v1";
const urlPrefix = "https://" + config.firebase.projectId + ".appspot.com/api/v1";
const jobsURL = '/jobs';
const pipelineListURL = '/pipeline/list';
const pipelineDetailsURL = '/pipeline/details';
const createSubscriptionURL = '/subscription/create';
const optionsReqURL = '/options';
const infoReqURL = '/info';
const stopPipelineURL = '/stop';

function encodeURLData(params) {
    const formData = new FormData();
    Object.keys(params).map((key) => {
        formData.append(key, params[key])
    });
    return formData;
}

export default {

    getJobs() {
        console.log('getJobs called');
        return fetch(urlPrefix + jobsURL)
            .then((response) => response.json());
    },
    getPipelines() {
        console.log('getPipelines called');
        return fetch(urlPrefix + pipelineListURL)
            .then((response) => response.json());
    },
    _selectedReferencesToCsvString: function (referenceNameList) {
        let selectedItems = referenceNameList
            .filter(reference => reference.selected)
            .map(reference => reference.name);
        return selectedItems.join(',');
    },
    saveNewPipeline(pipeline) {
        console.log('saveNewPipeline called', pipeline)

        // let reqData = {
        //     pipeline_name: pipeline.pipelineName,
        //     collectionNamePrefix: pipeline.collectionNamePrefix,
        //     documentNamePrefix: pipeline.documentNamePrefix,
        //     processing_mode: pipeline.processingMode,
        //     inputDataSubscription: pipeline.inputDataSubscription,
        //     input_folder: pipeline.inputFolder,
        //     reference_name_list: this._selectedReferencesToCsvString(pipeline.referenceNameList),
        //     pipeline_autostart: pipeline.pipelineAutoStart,
        //     pipeline_start_immediately: pipeline.pipelineStartImmediately,
        //     upload_bucket_name: config.general.uploadBucketName,
        // };

        let reqData = Object.assign({}, pipeline); // clone
        reqData.referenceNameList = this._selectedReferencesToCsvString(pipeline.referenceNameList);
        reqData.uploadBucketName = config.general.uploadBucketName;

        console.log('saveNewPipeline params: ', reqData)

        return fetch(new Request(urlPrefix + optionsReqURL,
            {
                headers: { "Content-Type": "application/json; charset=utf-8" },
                method: 'POST',
                body: JSON.stringify(reqData)
            }))
            .then((response) => response.json());
    },
    updatePipeline(pipeline) {
        console.log('updatePipeline called', pipeline)
        return fetch(new Request(urlPrefix + optionsReqURL,
            {
                headers: { "Content-Type": "application/json; charset=utf-8" },
                method: 'PUT',
                body: JSON.stringify(pipeline)
            }))
    },
    stopJob(job_id, location) {
        console.log('STOP Pipeline called: ' + stopPipelineURL + '?jobId=' + job_id + '&location=' + location)
        
        return fetch(urlPrefix + stopPipelineURL + '?jobId=' + job_id + '&location=' + location, {method: 'POST'})
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
    deleteSubscription(subscriptionId) {
        // TODO: Implement
    },
    getJobDetails(jobId, location) {

        console.log('getJobDetails called, jobId=' + jobId + '&location=' + location)

        return fetch(urlPrefix + infoReqURL + '?jobId=' + jobId + '&location=' + location)
    },
    getPipelineDetails(pipelineId) {

        console.log('getPipelineDetails called, pipelineId=' + pipelineId)

        return fetch(urlPrefix + pipelineDetailsURL + '?pipelineId=' + pipelineId)
            .then((response) => response.json());
    },
    
}
