<template>
    <div>
        <div class="form-row">
            <div class="col-md-2">
                <TooltipQuestionCircle title="Reference database name. A name to identify your database"/>
                <input v-model="refDb.name" type="text" class="form-control" placeholder="Enter ref db name" />
            </div>

            <div class="col-md-5">
                <TooltipQuestionCircle title="Reference database path to your FASTA file. Example: gs://<your project id>-reference-db/db1/DB_viruses_12345678.fasta"/>
                <input v-model="refDb.fastaUri" type="text" class="form-control" placeholder="Enter ref db fasta uri" />
            </div>

            <div class="col-md-4">
                <TooltipQuestionCircle title="Path to your taxonomy file. Example: gs://<your project id>-reference-db/taxonomy/species_tree.txt"/>
                <input v-model="refDb.ncbiTreeUri" type="text" class="form-control" placeholder="Enter ref db ncbi tree uri" />
            </div>

            <div class="col-md-1">
                <button type="button"
                        v-on:click="removeRefDbBlock()"
                        class="btn btn-link"><i class="fa fa-minus-circle" aria-hidden="true"></i></button>
            </div>
        </div>
        <error-message v-bind:errMsg="errMsg" />
    </div>
</template>
<script>

    import ErrorMessage from '../ErrorMessage.vue'
    import TooltipQuestionCircle from "../../components/TooltipQuestionCircle";

    export default {
        name: "RefDbBlock",

        props: ["refDb", "referenceDbs"],

        data() {
            return {
                errMsg: {
                    show: false,
                    message: ''
                },
            }
        },

        components: {
            ErrorMessage,
            TooltipQuestionCircle
        },

        methods: {
            removeRefDbBlock() {
                console.log('removeRefDbBlock ', this.$parent.pipeline.referenceDbs.length)
                if (this.$parent.pipeline.referenceDbs.length > 1) {
                    // remove current block from the list on parent
                    this.$parent.onRemoveRefDbBlock(this.refDb);

                    // destroy the vue listeners, etc
                    this.$destroy();

                    // remove the element from the DOM
                    this.$el.parentNode.removeChild(this.$el);


                } else {
                    this.showError("At least one reference database required");
                }
            },
            showError(message) {
                this.errMsg.message = message;
                this.errMsg.show = true;
            },
        }
    };
</script>
<style scoped>
    .form-row {
        margin-bottom: 10px;
    }
</style>
