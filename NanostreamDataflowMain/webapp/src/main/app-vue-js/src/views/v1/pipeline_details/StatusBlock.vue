<template>
    <div>
        <div class="row ">
            <div class="col-sm">
                <h4 class="d-flex">Pipeline: {{ pipelineDetails.pipelineName }}</h4>
                <h5 class="d-flex">Status: {{ pipelineDetails.status }}</h5>
                <h5 class="d-flex">Processing mode: {{ pipelineDetails.processingMode }}</h5>
            </div>
            <div class="col-sm">
                <div>
                    <a :href="getFirestoreCollectionUrl" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;Firestore Collection&nbsp;</a>
                    <TooltipQuestionCircle title="External link to Firestore collection with your output data"/>
                </div>

                <div>
                    <a :href="getBucketFolderUrl" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;GCP Upload Bucket&nbsp;</a>
                    <TooltipQuestionCircle title="External link to GCP Upload Bucket with your input data"/>
                </div>

                <div>
                    <a :href="getInputDataSubscriptionUrl" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;GCP Subscription&nbsp;</a>
                    <TooltipQuestionCircle title="External link to GCP Subscription" />
                </div>
                <div>Reference databases:
                    <TooltipQuestionCircle title="Reference databases used in this pipeline" />
                    <dl>
                        <li v-for="referenceDb in pipelineDetails.referenceDbs">
                            <a :href="getRefDbUrl(referenceDb)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;{{ referenceDb.name }}&nbsp;</a>
                            <a :href="getTaxonomyUrl(referenceDb)" target="_blank"><i class="fa fa-sign-in"></i>&nbsp;taxonomy&nbsp;</a>
                        </li>
                    </dl>
                </div>
            </div>
        </div>
        <div class="row ">
            <div class="col-sm">
            </div>
            <div class="col-sm">
            </div>
        </div>

    </div>
</template>

<script>
    import config from '../../../config.js';
    import GcpUrl from '../../../gcp_url.util.js';
    import TooltipQuestionCircle from "../../components/TooltipQuestionCircle";

    export default {

        name: 'PipelineDetailsStatusBlock',
        components: {TooltipQuestionCircle},
        props: ["pipelineDetails"],

        comments: {
            TooltipQuestionCircle,
        },

        computed: {

            getFirestoreCollectionUrl() {
                return GcpUrl.getFirestoreCollectionUrl(this.pipelineDetails);
            },
            getBucketFolderUrl: function () {
                return GcpUrl.getBucketFolderUrl(this.pipelineDetails);
            },
            getInputDataSubscriptionUrl() {
                return GcpUrl.getInputDataSubscriptionUrl(this.pipelineDetails);
            },
        },

        methods: {
            getRefDbUrl(referenceDb) {
                let bucketFile = GcpUrl.removeGsPrefix(referenceDb.fastaUri);
                return GcpUrl.getBucketUrl(bucketFile);
            },
            getTaxonomyUrl(referenceDb) {
                let bucketFile = GcpUrl.removeGsPrefix(referenceDb.ncbiTreeUri);
                return GcpUrl.getBucketUrl(bucketFile);
            },
        },
    }
</script>
