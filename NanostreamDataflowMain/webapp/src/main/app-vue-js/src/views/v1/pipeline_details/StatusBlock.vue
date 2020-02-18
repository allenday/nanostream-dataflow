<template>
    <div>
        <div class="row ">
            <div class="col-sm">
                <h3 class="d-flex">Pipeline: {{ pipelineDetails.pipelineName }}</h3>
            </div>
            <div class="col-sm">
                <h3 class="d-flex">Status: {{ pipelineDetails.status }}</h3>
            </div>
        </div>
        <div class="row ">
            <div class="col-sm">
                <h5 class="d-flex">Collection prefix: {{ pipelineDetails.outputCollectionNamePrefix }}</h5>
            </div>
            <div class="col-sm">
                <h5 class="d-flex">Processing mode: {{ pipelineDetails.processingMode }}</h5>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'PipelineDetailsStatusBlock',
        props: ["pipelineDetails"],

        // computed : {
        //     statusText : function() {
        //         return "Current state = " + this.pipeline.status + ",Click to Start/Stop pipeline";
        //     }
        //
        // }
    }
</script>
