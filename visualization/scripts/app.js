(function () {
    // Initialize Firebase
    var config = {
        apiKey: "AIzaSyDLtxwk4r3ahh-R7aTGIXlMvgrBi5pc_P0",
        authDomain: "nano-stream1.firebaseapp.com",
        databaseURL: "https://nano-stream1.firebaseio.com",
        projectId: "nano-stream1",
        storageBucket: "nano-stream1.appspot.com",
        messagingSenderId: "465460488211"
    };
    firebase.initializeApp(config);
    var db = firebase.firestore();
    db.settings({
        timestampsInSnapshots: true
    });

    var timerInterval = 1500;

    var donut = donutChart()
        .width(960)
        .height(500)
        .transTime(750) // length of transitions in ms
        .cornerRadius(3) // sets how rounded the corners are on each slice
        .padAngle(0.015) // effectively dictates the gap between slices
        .variable('prob')
        .category('species');

    var _transform = function (doc) {
        console.log(doc);
        timestring = '_ ' + doc.date.toDate().toISOString();
        groupped = doc.sequenceRecords
            .map(record => {
                return {
                    probe: record.probe,
                    name: (record.taxonomy.length) ? record.taxonomy[0] : record.name
                }
            })
            .map(record => {
                return {
                    time: timestring,
                    prob: record.probe,
                    species: record.name
                }
            }).reduce(function (rv, x) {
                rv[x['species']] = rv[x['species']] || { prob: 0 }
                rv[x['species']].species = x.species;
                rv[x['species']].time = x.time;
                rv[x['species']].prob += x.prob;
                // rv[x['species']].err = x.err;

                return rv;
            }, {});

        var keys = Object.keys(groupped);
        var items = keys.map(function (k) { return groupped[k]; })

        return items;
    };

    db.collection("sequences_statistic").doc("resultDocument--2019-01-28T14-38-35UTC").get().then((doc) => {
        results = _transform(doc.data());
        console.log(results);
        donut.data(results);
        d3.select('#chart')
            .call(donut); // draw chart in div
    });
}());
