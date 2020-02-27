<style>

  @import './nanostream.css'; 
  @import './sunburst.css';
  @import 'assets/vendor/font-awesome/css/font-awesome.min.css';

</style>

<template>
    <div id="app" class="container-fluid main">      
        <router-view name="header"></router-view>

                <router-view/>
        
        <router-view name="footer"></router-view>
    </div>
</template>
<script>
 
</script>
