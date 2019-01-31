requirejs(['db', 'transform'], function (db, transform) {
  let url = new URL(document.location.href);
  let collection = url.searchParams.get('c');
  let docname = url.searchParams.get('d');
  console.log(collection);
  console.log(docname);

  if (!collection) {
    collection = 'sequences_statistic';
  }
  if (!docname) {
    docname = 'resultDocument--2019-01-28T14-38-35UTC';
  }

  let chart;
  let results = [];

  nv.addGraph(function () {
    chart = nv.models.sunburstChart();

    chart.color(['gray']);
    chart.groupColorByParent(false);
    chart.showLabels(true);
    chart.mode('value');
    chart.groupColorByParent(true);
    chart.labelFormat(function (d) {
      console.log(d);
      if (typeof d.children === 'undefined') {
        return '    ' + d.parent.name + " > " + d.name + ' ' + Math.round(100 * d.value) / 100;
      }
      // TODO: don't use collection name here
      if (d.depth === 1 && collection !== 'resistant_sequences_statistic') {
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
