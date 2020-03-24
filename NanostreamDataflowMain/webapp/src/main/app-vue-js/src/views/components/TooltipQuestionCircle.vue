<template>
        <i class="fa fa-question-circle" v-b-tooltip.auto.click.hover.blur.v-secondary :title="title" tabindex="0"></i>
</template>
<script>
    export default {
        name: "TooltipQuestionCircle",

        props: ['title'],
    };
</script>
<style>
</style>
