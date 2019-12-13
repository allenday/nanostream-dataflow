<template>
<div class="row" id="configurations">
    <div class="col">
      	<div class="row"><div class="col card-header"><h2>Configurations</h2></div></div>
       	<div class="row card-body">  
	        <div class="col-sm-4">
	          <h3>General</h3>
	          <ul>
	            <li><span class="conf_name">Google account</span>: {{ general.google_account }} </li>
	            <li><span class="conf_name">Project</span>: 
					<span v-bind:class="general.project || 'waitingForData'">{{ general.project || '...getting data...'}} </span></li>
	            <li><span class="conf_name">Bucket</span>: 
					<span v-bind:class="general.bucket || 'waitingForData'">{{ general.bucket ||  '...getting data...'}}</span></li>
	            <li><span class="conf_name">Reference database</span>: {{general.ref_db }} </li>
	          </ul>
	          <p v-bind:class="general.bucket || 'hidden'"><a :href="bucket_name_full"><i class="fa fa-sign-in"></i> Go to bucket</a></p> 
	        </div>
	        
	        <div class="col-sm-5">
	          <h3>Notifications</h3>
	          <ul>
	            <li><span class="conf_name">Topic</span>: {{ notifications.topic }} </li>
	            <li><span class="conf_name">Subscription</span>: {{ notifications.subscriptions }}</li>
	          </ul>
	        </div>
	        
	        <div class="col-sm-3">
	          <h3>Pipeline</h3>
	          <ul>
	            <li><span class="conf_name">Alignment window: </span>: {{ pipeline.alignment_window }}s </li>
	            <li><span class="conf_name">Update frequency</span>: {{ pipeline.update_frequency }}s</li>
	          </ul>
	        </div>
    	</div>
    </div>
      </div>
</template>



<script>


export default {

    name: "Configurations",

    props: ["notifications","pipeline","general"],
   
  	computed: {

		  bucket_name_full : function() { 
			  return  "https://console.cloud.google.com/storage/browser/"  
				  + (this.general.bucket && this.general.bucket.split('/')[2] ) 
			}	  	
  	}

}

</script>
