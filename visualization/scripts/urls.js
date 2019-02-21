const firebaseDocuments = document.querySelector('#firebase_documents')

// create element and render file

function renderUrl(doc){
  let li = document.createElement('li');

  li.setAttribute('doc-id', doc.id);
}

db.collection('new_scanning_species_sequences_statistic').get().then((snapshot) => {
  snapshot.docs.forEach(doc => {
    renderUrl(doc);
  })
})
