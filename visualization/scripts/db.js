define(function () {
  // Initialize Firebase
  const config = {
    apiKey: "AIzaSyA5_c4nxV9sEew5Uvxc-zvoZi2ofg9sXfk",
    authDomain: "nanostream-dataflow.firebaseapp.com",
    databaseURL: "https://nanostream-dataflow.firebaseio.com",
    projectId: "nanostream-dataflow",
    storageBucket: "nanostream-dataflow.appspot.com",
    messagingSenderId: "500629989505"
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});