requirejs(['db', 'transform'], function (db, transform) {
  let url = new URL(document.location.href);
  let collection = url.searchParams.get('c');
  let docname = url.searchParams.get('d');
  let mode = url.searchParams.get('m');

  if (!collection) {
    collection = 'sequences_statistic';
  }
  if (!docname) {
    docname = 'resultDocument--2019-01-28T14-38-35UTC';
  }

  if (!mode) {
    mode = 'species';
  }

  let chart;
  let results = [];

  nv.addGraph(function () {
    chart = nv.models.sunburstChart();

    chart.groupColorByParent(false);
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
      .datum(results)
      .call(chart);

    nv.utils.windowResize(chart.update);

    return chart;
  });

  // real-time updates
  db.collection(collection).doc(docname)
    .onSnapshot(function (doc) {
      results = transform(doc.data());
      d3.select('#chart')
        .datum(results)
        .call(chart);
    });
});
