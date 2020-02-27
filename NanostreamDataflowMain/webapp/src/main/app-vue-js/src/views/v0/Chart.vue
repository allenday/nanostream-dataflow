<template>
    <div class="d-flex chart-area mx-auto">
        <svg id="chart"        
        :height='height'
        :width='width'></svg>
    </div>
</template>

<script>

import d3 from "d3";
import nv  from "nvd3";


export default {
  props: ["records","mode","loading"],

  data() {
      return {
          chart : null,
          width: 1200,
          height: 800
    }
  },

  watch: {
    records(val) {
        console.log('mode=' + this.mode)
        this.chart ? this.updateChart(val) : this.initChart(val, this.mode);
    },
    loading() {
        console.log('Loading=' + this.loading)
     }
  },


  mounted() {
    this.initChart(this.mode)
  },


  methods: {

   updateChart(val) {

        d3.select('#chart')
        .datum(this.records)
        .call(this.chart)
        .selectAll('.arc-container text').attr('dy',4)

        this.addZoom(this.chart);

       console.log('Update chart called')

   },    

   addZoom(chart) {

        var scaleExtent = 10;
        var d3zoom = d3.behavior.zoom();
        
        function unzoomed() {
            d3zoom.scale(1);
            d3zoom.translate([0,0]);
        };


        function zoomed() {  
            d3.select('#chart').attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
        }

        d3zoom
          .scaleExtent([1, scaleExtent])
          .on('zoom', zoomed);
          
        d3.select('#chart').select('.nvd3').call(d3zoom)//.on("mousedown.zoom", null)//.on('dblclick.zoom', unzoomed);
    },
    


    initChart(val, mode ) {

      console.log('Init chart called')

      nv.addGraph(function () {
            let chart = nv.models.sunburstChart();
            chart.groupColorByParent(false);
            chart.noData("Please wait. Data is being processed.")
            chart.sort((d1, d2) => { return d1.id > d2.id})
            chart.key(d => d.id)
            chart.showLabels(true);
            chart.mode('value');
            chart.groupColorByParent(false);
            chart.labelFormat(function (d) {
                if (typeof d.children === 'undefined') {
                    return '    ' + d.parent.name + " > " + d.name + ' ' + Math.round(100 * d.value) / 100;
                }
                // TODO: don't use collection name here
                if (d.depth === 1 && mode === 'species') {
                    return d.name + ' ' + Math.round(100 * d.value) / 100;
                }
                return null;
            });
            chart.labelThreshold(0.05);
            chart.tooltip.valueFormatter(function (d) {
                return Math.round(100 * d) / 100;
            });

         
            d3.select('#chart')
                .datum(val)
                .call(chart)
         

            nv.utils.windowResize(chart.update);
            return chart;

    },(c) => {this.chart=c;  });

    }


  }
};

</script>