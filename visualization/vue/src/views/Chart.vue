<template>
    <div class="d-flex">
        <svg id="chart"        
        :height='height'
        :width='width'></svg>
    </div>
</template>

<script>

import d3 from "d3";
import nv  from "nvd3";


export default {
  props: ["records","mode"],

  data() {
      return {
          chart : null,
          width: 1200,
          height: 800
    }
  },

  watch: {
    records(val) {
        this.chart ? this.updateChart(val) : this.initChart(val, this.mode);
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

       console.log('Update chart called')

   },    

    initChart(val, mode ) {

      console.log('Init chart called')

      nv.addGraph(function () {
            let chart = nv.models.sunburstChart();
            chart.groupColorByParent(false);
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
                .call(chart);


            nv.utils.windowResize(chart.update);
            return chart;
    },(c) => {this.chart=c});

    }


  }
};

</script>