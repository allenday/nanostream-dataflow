<template>
    <div class="d-flex">
        <svg id="chart"        
        :height='height'
        :width='width'></svg>
    </div>
</template>

<script>

import d3 from "d3";


export default {
  props: ["records"],

  computed: {
    recordsSorted: function () {
        return this.records.sort();
  },


  data() {
      return {
          records : this.recordsSorted()
    }
  },

  watch: {
    records(val) {
        this.chart ? this.updateChart(val) : this.initChart(val, this.mode);
    }
  },

 


  methods: {

   updateChart(val) {

        d3.select('#chart')
        .datum(this.records)
        .call(this.chart)
        .selectAll('.arc-container text').attr('dy',4)

       console.log('Updatechart called')

   },    

    initChart(val, mode ) {

      

    }


  }
};

</script>