<template>
<div class="row">
    <div class="col">
      	<div class="row"><div class="col card-header"><h2>Table</h2></div></div>
       	<div class="row card-body">  
	        <div class="col-sm-8">
              <b-table striped bordered hover :items="recordsProcessed"></b-table> 
	        </div>
    	  </div>
    </div>
  </div>
</template>


<script>

import d3 from "d3";

export default {

  props: ["records"],

  computed: {
    recordsProcessed: function () {

      console.log('computed called')

      if(this.records.length) {
        let recs = [], nodes = this.hier(this.records[0], d => d.children);
        for(let i in nodes) {
            let n = nodes[i], p = [];
            while(n.parent) {
               p.push(n.parent.name);
               n = n.parent; 
            }
           p.pop() 
          // p.shift();
           for(let j=1;j<p.length-1;j++) p[j] = '...';
           nodes[i].path = p.reverse().join(' > ') 
        }
   
        nodes.forEach(i => recs.push( { name: i.name || 'TOTAL', hierarchy: i.path || 'root', value: i.value }))

          return recs;
      }
      else{
        return [];
      } 
    }
  },




  data() {
      return {
        data : this.records,
        hier: null
    }
  },

  watch: {
    records(val) {
        this.updateTable(val);
    }
  },

   created: function () { 
        this.hier = d3.layout.hierarchy();
    },


  methods: {

  

   updateTable(val) {

     for(var i in this.records) {
       console.log(this.records[i])
     }

       console.log('UpdateTable called ')

   },    

    

  }
};

</script>