<template>
        <i class="fa fa-question-circle" v-b-tooltip.auto.hover.click.v-secondary :title="title"></i>
</template>
<script>
    export default {
        name: "TooltipQuestionCircle",

        props: ['title'],
    };
</script>
<style>
</style>
