<template>

<div class="container">
	  
  <form class="mx-auto"> 
    <div class="topfields mx-auto">
    	<div class="topfields-inner mx-auto">

	    <p class="text-center form-title">Define the following variables to run the pipeline:</p>
	    <div class="form-group">
	      <label for="pipeline-name">Pipeline name:</label>
	      <input v-model="pipeline.name" type="text" class="form-control" id="pipeline-name" placeholder="id123456"  name="pipeline-name">
	    </div>
		
		<div class="form-group">
	      <label for="document-name">Collection name prefix: </label>&nbsp;
	       <a class="tooltip-icon" data-toggle="tooltip" data-placement="top" title="Firestore database сollection name prefix that will be used for writing results."><i class="fa fa-question-circle"></i></a>
	      <input v-model="general.collection_name" type="text" class="form-control" id="collection-name" placeholder="collectionname" >
	    </div>

		<div class="form-group">
	      <label for="document-name">Document name prefix: </label>&nbsp;
	       <a class="tooltip-icon" data-toggle="tooltip" data-placement="top" title="Firestore database document name prefix that will be used for writing statistic results."><i class="fa fa-question-circle"></i></a>
	      <input v-model="general.document_name" type="text" class="form-control" id="document-name" placeholder="directoryname" name="document-name">
	    </div>


		<fieldset>
			<legend>Reference database:</legend>
			
			<div class="custom-control custom-radio">
		  		<input v-model="custom_selected" value="false" type="radio" id="predefined-radio"  class="custom-control-input" >
		  		<label class="custom-control-label" for="predefined-radio">Use predefined:</label>
				  &nbsp;<a class="tooltip-icon" data-toggle="tooltip" data-placement="top" 
				  	title="choose one of the predefined processing mode. Each mode uses corresponding reference database"><i class="fa fa-question-circle"></i></a>
		  	</div>
			<select v-model="general.ref_db" name="processing_mode" class="custom-select" id="processing_mode">
	      		<option selected value="species">species</option>
	      		<option value="resistance_genes">resistance_genes</option>
	      	</select>

		  	<div class="custom-control custom-radio">
		  		<input v-model="custom_selected" value="true" type="radio" id="custom-radio" class="custom-control-input">
		  		<label class="custom-control-label" for="custom-radio">Custom</label>
			</div>
			<div class="form-group">
	      	<input type="text" class="form-control" id="custom-dataset" placeholder="enter path for your dataset" name="custom-dataset">
	    	</div>
		</fieldset>

        
		<fieldset>

			<p class="form-inner-title text-center"><span v-on:click="advanced_enabled=!advanced_enabled" id="advanced-toggler">Advanced </span> 
            <i v-if="advanced_enabled" class="fa fa-angle-up">
            </i>
            <i v-else class="fa fa-angle-down">
            </i>
                
            </p>
                <div v-if="advanced_enabled">
			        <div class="form-group">
	      		        <label for="alignment-window">Alignment window:</label><span><i class="fa fa-question-circle"></i></span>
	      	            <input v-model="pipeline.alignment_window" type="text" class="form-control" id="alignment-window" placeholder="" name="alignment-window">
	    	        </div>
                </div>
		</fieldset>
      
	</div>
</div>

<div class="submit-block text-center">

    <button type="button" v-on:click="$emit('Start')" class="btn btn-primary">Start</button>

</div>   
  </form>
</div>


</template>

<script>


export default {

  name: 'PipelineForm',

  props: ["pipeline", "general"],

  data() {
      return {
		  advanced_enabled: false,
		  custom_selected : false
    }
  }

}

</script>
