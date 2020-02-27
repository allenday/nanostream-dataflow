<template>

    <div class="topfields mx-auto">
        <div class="float-right">
            <a href="#" v-on:click="removePipelineBlock()">Close</a>
        </div>
        <p class="form-title">Define the following variables to run the pipeline:</p>
        <div class="row topfields-inner ">

            <div class="col-md-6">
                <div class="form-group">
                    <label for="pipeline-name">Pipeline name:</label>
                    <input v-model="pipeline.pipelineName" type="text" class="form-control" id="pipeline-name"
                           placeholder="Enter Pipeline Name" name="pipeline-name">
                </div>

                <div class="form-group">
                    <label for="input-folder">Input folder:</label>&nbsp;
                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                       title="Enter Input folder to process inside upload bucket"><i class="fa fa-question-circle"></i></a>
                    <br>
                    <input v-model="pipeline.inputFolder" type="text" class="form-control" id="input-folder"
                           placeholder="Enter Input folder" name="input-folder">
                </div>

                
                <div class="form-group">
                    <label for="processing_mode">Pipeline type: </label>&nbsp;
                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                       title="choose one of the predefined processing mode. Each mode uses corresponding reference database"><i class="fa fa-question-circle"></i></a>
                    <br>
                    <select v-model="pipeline.processingMode" class="custom-select" id="processing_mode">
                        <option selected value="species">species</option>
                        <option value="resistance_genes">resistance_genes</option>
                    </select>
                </div>

                <div class="form-group">
<!--                    <label for="reference-database-location">Reference database location: </label>&nbsp;-->
<!--                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"-->
<!--                       title="Location of reference database. Example: gs://projectId-reference-db-1/species."><i class="fa fa-question-circle"></i></a>-->
<!--                    <input v-model="pipeline.reference_database_location" type="text" class="form-control" id="reference-database-location"-->
<!--                           placeholder="Enter reference database location">-->
                    <label for="reference-name-list">Reference name list: </label>
                    <ul id="reference-name-list">
                        <li v-for="referenceName in pipeline.referenceNameList">
                            <base-checkbox class="mb-3" v-model="referenceName.selected">
                                {{ referenceName.name }} 
<!--                                <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"-->
<!--                                   title="Allow restart the pipeline after it stopped."><i class="fa fa-question-circle"></i></a>-->
                            </base-checkbox>
                        </li>
                    </ul>
                </div>

            </div>

            <div class="col-md-6">
                <!--<div class="form-group">-->
                    <!--<label for="collection-name">Output Collection name prefix: </label>&nbsp;-->
                    <!--<a class="tooltip-icon" data-toggle="tooltip" data-placement="top"-->
                       <!--title="Firestore database сollection name prefix that will be used for writing results."><i class="fa fa-question-circle"></i></a>-->
                    <!--<input v-model="pipeline.collectionNamePrefix" type="text" class="form-control" id="collection-name"-->
                           <!--placeholder="Enter collection name prefix">-->
                <!--</div>-->

                <div class="form-group">
                    <label for="auto-stop">Auto-stop (seconds): </label>&nbsp;
                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                       title="Number of seconds no new files added to the pipeline to stop."><i class="fa fa-question-circle"></i></a>
                    <input v-model="pipeline.autoStopDelaySeconds" type="text" class="form-control" id="auto-stop"
                           placeholder="Enter number of seconds" name="document-name-prefix">
                </div>

                <div class="form-group">
                    <base-checkbox class="mb-3" v-model="pipeline.pipelineAutoStart">
                        Auto-start
                        <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                           title="Start pipeline when new data added to upload bucket."><i class="fa fa-question-circle"></i></a>
                    </base-checkbox>
                </div>

                <div class="form-group">
                    <base-checkbox class="mb-3" v-model="pipeline.pipelineStartImmediately">
                        Start immediately
                        <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                           title="Start right after START button pressed."><i class="fa fa-question-circle"></i></a>
                    </base-checkbox>
                </div>

            </div>

        </div>
    </div>


</template>

<script>

    import BaseCheckbox from "../../../components/BaseCheckbox.vue"
    import config from '../../../config.js';

    export default {

        name: 'PipelineForm',

        props: ["pipeline"],

        data() {
            return {
                // advanced_enabled: false,
                // custom_selected: false,
                // checkboxes: {
                //     unchecked: false,
                //     checked: true,
                //     uncheckedDisabled: false,
                //     checkedDisabled: true
                // }
                // referenceNamesList: {}
            }
        },
        mounted() {
            console.log(this.pipeline)
        },

        // computed: {
        //     referenceNameList() {
        //         let result = [];
        //         let referenceNames = config.general.referenceNamesList.split(/\s*,\s*/);
        //         referenceNames.forEach(function (referenceName) {
        //             result.push({name: referenceName, checked: true })
        //         });
        //         return result;
        //     }
        // },

        components: {
            BaseCheckbox
        },

        methods: {
            removePipelineBlock() {
                console.log('emit remove-pipeline-block event')
                this.$vueEventBus.$emit("remove-pipeline-block", this.pipeline);

                // destroy the vue listeners, etc
                this.$destroy();

                // remove the element from the DOM
                this.$el.parentNode.removeChild(this.$el);
            }
        },

    }

</script>
