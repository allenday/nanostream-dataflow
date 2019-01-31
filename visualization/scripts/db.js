define(function () {
  // Initialize Firebase
  const config = {
    apiKey: "AIzaSyDQcC0LgEKiiYClfUFFomcVKZLoeEp8E78",
    authDomain: "upwork-nano-stream.firebaseapp.com",
    databaseURL: "https://upwork-nano-stream.firebaseio.com",
    projectId: "upwork-nano-stream",
    storageBucket: "upwork-nano-stream.appspot.com",
    messagingSenderId: "500629989505"
  };

  firebase.initializeApp(config);

  const db = firebase.firestore();
  db.settings({
    timestampsInSnapshots: true
  });

  return db;
});