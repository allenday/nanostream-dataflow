define(function () {
  // Initialize Firebase
  const config = {
    apiKey: "AIzaSyDLtxwk4r3ahh-R7aTGIXlMvgrBi5pc_P0",
    authDomain: "nano-stream1.firebaseapp.com",
    databaseURL: "https://nano-stream1.firebaseio.com",
    projectId: "nano-stream1",
    storageBucket: "nano-stream1.appspot.com",
    messagingSenderId: "465460488211"
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});
