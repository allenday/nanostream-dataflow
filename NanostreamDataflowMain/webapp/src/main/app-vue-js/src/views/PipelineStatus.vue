<template>
   <!--<div id="pipelinestatus" class="card border-dark">
      	<div class="no-gutters card-body row">  -->
          <div id="pipelinestatus" class="row ">  
        <div class="col-sm">
          <h2 class="d-flex">Pipeline: <span> &nbsp;{{ pipeline.name }}</span></h2>
        </div>
        <div class=" col-sm">
          <h2 class="d-flex">Status:             
            <span v-if="pipeline.started">
              <span>&nbsp;Launched </span>
               <span data-toggle="tooltip" data-placement="top" :title="statusText" v-on:click="$emit('PipelineStatusUpdate')" class="pipeline-control fa fa-stop" ></span> 
            </span> 
            
            <span v-else>
              <span>&nbsp;Stopped </span> 
              <span data-toggle="tooltip" data-placement="top" 
              :title="statusText" v-on:click="$emit('PipelineStatusUpdate')"  class="pipeline-control fa fa-play" aria-hidden="true"></span> 
            </span> 
                        
            </h2>
          <!--<img src="" width="55" height="55">-->
        </div>
      </div>
  <!--</div>-->
</template>

<script>


export default {

  name: 'pipelinestatus',
  props: ["pipeline"],




  computed : {
    statusText : function() {
      return "Current state = " + this.pipeline.status + ",Click to Start/Stop pipeline";
    }

  }

}

</script>
