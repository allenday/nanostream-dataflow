<template>
    <div>
        <base-checkbox class="check-box-center" v-model="pipeline.pipelineAutoStart" v-on:input="changeAutostart(pipeline)"></base-checkbox>
    </div>
</template>
<script>

    import BaseCheckbox from "../../../components/BaseCheckbox.vue"
    import api from '../../../api.js';

    export default {
        name: 'Autostart',

        props: ["pipeline"],

        components: {
            BaseCheckbox,
        },

        methods: {
            changeAutostart(pipeline) {
                console.log('changeAutostart', pipeline.pipelineAutoStart)
                api.updatePipeline({id: pipeline.id, pipelineAutoStart: pipeline.pipelineAutoStart});
            }
        }
    };
</script>
<style>
    .check-box-center {
        margin-bottom:  40px;
    }
</style>
