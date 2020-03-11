import config from './config.js';

function _getSubscriptionShortName(pipeline) {
    // projects/nanostream-test1/subscriptions/nanostream-20200212t161049707z => nanostream-20200212t161049707z
    return pipeline.inputDataSubscription.replace(new RegExp("^.+\/(.*)"), '$1'); // get part after the last slash
}

export default {
    getGcpDataflowJobUrl(job) {
        return `https://console.cloud.google.com/dataflow/jobs/${job.location}/${job.id}`;
    },
    getFirestoreCollectionUrl(pipeline) {
        return "https://console.firebase.google.com/u/0/project/" + config.firebase.projectId + "/database/firestore/data~2F"
            + pipeline.outputCollectionNamePrefix + '__statistic__' + pipeline.uploadBucketName;
    },
    getInputDataSubscriptionUrl(pipeline) {
        let subscription = _getSubscriptionShortName(pipeline);
        return "https://console.cloud.google.com/cloudpubsub/subscription/detail/" + subscription + "?authuser=0&project=" + config.firebase.projectId;
    },
    getBucketFolderUrl(pipeline) {
        return "https://console.cloud.google.com/storage/browser/" + pipeline.uploadBucketName + "/" + pipeline.inputFolder + "/?authuser=0&project=" + config.firebase.projectId;
    },
    getBucketUrl(bucketName) {
        return `https://console.cloud.google.com/storage/browser/${bucketName}`;
    },
    removeGsPrefix: function (gsUri) {
        return gsUri.replace(new RegExp('^gs://'), '');
    },
}