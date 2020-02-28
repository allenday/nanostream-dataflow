<template>

    <div class="topfields mx-auto">
        <div class="float-right">

            <button type="button"
                    v-on:click="removePipelineBlock()"
                    class="btn btn-link">Close</button>

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
                       title="Enter Input folder to process inside upload bucket."><i class="fa fa-question-circle"></i></a>
                    <br>
                    <input v-model="pipeline.inputFolder" type="text" class="form-control" id="input-folder"
                           placeholder="Enter Input folder" name="input-folder">
                </div>

                
                <div class="form-group">
                    <label for="processing_mode">Pipeline type: </label>&nbsp;
                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                       title="Choose one of the predefined processing mode."><i class="fa fa-question-circle"></i></a>
                    <br>
                    <select v-model="pipeline.processingMode" class="custom-select" id="processing_mode">
                        <option selected value="species">species</option>
                        <option value="resistance_genes">resistance_genes</option>
                    </select>
                </div>

            </div>

            <div class="col-md-6">

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

            <div class="col-md-12">

                <div class="form-group">
                    <label>Reference databases: </label>&nbsp;
                    <a class="tooltip-icon" data-toggle="tooltip" data-placement="top"
                       title="Reference database parameters. At least one reference database required."><i class="fa fa-question-circle"></i></a>
                    <button type="button"
                            v-on:click="addNewRefDbBlock()"
                            class="btn btn-link"><i class="fa fa-plus-circle" aria-hidden="true"></i></button>

                    <div ref="referencedbs" />

                </div>
            </div>
        </div>
    </div>


</template>

<script>

    import Vue from 'vue'
    import BaseCheckbox from "../../../components/BaseCheckbox.vue"
    import RefDbBlock from "./RefDbBlock.vue"

    import config from '../../../config.js';

    let g_block_id = 0;

    function _prepareRefDb(referenceDbs) {
        let refDb = {
            block_id: ++g_block_id, // identify UI block
            name: '',
            fastaUri: '',
            ncbiTreeUri: '',
        };

        referenceDbs.push(refDb);
        return refDb;

    }

    export default {

        name: 'PipelineForm',

        props: ["pipeline"],

        data() {
            return {
            }
        },

        mounted() {
            console.log('NewPipelineBlock mounted', this.pipeline)
            this.addNewRefDbBlock()
        },

        components: {
            BaseCheckbox,
            RefDbBlock,
        },

        methods: {
            removePipelineBlock() {
                console.log('emit remove-pipeline-block event')
                this.$vueEventBus.$emit("remove-pipeline-block", this.pipeline);

                // destroy the vue listeners, etc
                this.$destroy();

                // remove the element from the DOM
                this.$el.parentNode.removeChild(this.$el);
            },
            addNewRefDbBlock() {
                console.log('Add new ref db block')

                let ComponentClass = Vue.extend(RefDbBlock);
                let instance = new ComponentClass({
                    propsData: {refDb: _prepareRefDb(this.pipeline.referenceDbs), referenceDbs: [this.pipeline.referenceDbs]}
                });
                instance.$parent = this;
                instance.$mount(); // pass nothing

                this.$refs.referencedbs.appendChild(instance.$el)
                
            },
            onRemoveRefDbBlock(refDb) {
                console.log('onRemoveRefDbBlock', refDb);
                this.pipeline.referenceDbs = this.pipeline.referenceDbs.filter(p => p.block_id !== refDb.block_id);
            },

        },

    }

</script>
