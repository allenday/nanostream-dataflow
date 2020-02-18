<template>

<header class="row header-global d-flex justify-content-center align-items-center">
    <div class="col-md-6">
        <router-link to="/"><h2>Nanostream</h2></router-link>

    </div>
    <div class="col-md-6 text-right">
        <router-link :to="{name: 'pipeline_list'}">Pipelines</router-link>  |
        <router-link :to="{name: 'job_list'}">Jobs</router-link>  |
        <router-link :to="{name: 'pipeline_new'}">New Pipeline</router-link>
    </div>
</header>

</template>

