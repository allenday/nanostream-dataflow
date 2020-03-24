<template>
    <div>
        <base-checkbox v-model="pipeline.pipelineAutoStart" v-on:input="changeAutostart(pipeline)"></base-checkbox>
    </div>
</template>
<script>

    import BaseCheckbox from "../../../components/BaseCheckbox.vue"
    import api from '../../../api.js';

    export default {
        name: 'AutostartCheckbox',

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
</style>
