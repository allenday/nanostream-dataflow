define(function () {
  // Initialize Firebase
  const config = {
<<<<<<< HEAD
    apiKey: "AIzaSyDLtxwk4r3ahh-R7aTGIXlMvgrBi5pc_P0",
    authDomain: "nano-stream1.firebaseapp.com",
    databaseURL: "https://nano-stream1.firebaseio.com",
    projectId: "nano-stream1",
    storageBucket: "nano-stream1.appspot.com",
    messagingSenderId: "465460488211"
=======
    apiKey: "AIzaSyA5_c4nxV9sEew5Uvxc-zvoZi2ofg9sXfk",
    authDomain: "nanostream-dataflow.firebaseapp.com",
    databaseURL: "https://nanostream-dataflow.firebaseio.com",
    projectId: "nanostream-dataflow",
    storageBucket: "nanostream-dataflow.appspot.com",
    messagingSenderId: "500629989505"
>>>>>>> 477fed5379f41580d4bda5ad1fb0441649e965d2
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});
