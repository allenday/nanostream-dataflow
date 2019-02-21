define(function () {
    const _findChild = function (node, childName) {
      let i, child;
      for (i = 0; i < node.children.length; i++) {
        child = node.children[i];
        if (child.name === childName) {
          return child;
        }
      }

      return null;
    };

    // function iterates over records
    //   assign current node to root
    //   iterate over record's taxonomy
    //     for each taxonomy level - search for existence in current node children
    //       if there is no level - create one
    //       summarize probes for current node and current record
    //       assign level as a current node
    const _transform = function (doc) {
      let i, j;

      let currentNode,
        record,
        taxonomyItem,
        taxonomyColor,
        taxonomyLevel;

      const root = {
        name: 'total',
        children: []
      };

      for (i = 0; i < doc.sequenceRecords.length; i++) {
        currentNode = root;
        record = doc.sequenceRecords[i];
        if (record.name.split('|').length > 3) {
          record.name = record.name.split('|')[3];
        }
        for (j = 0; j < record.taxonomy.length; j++) {
          taxonomyItem = record.taxonomy[j];
          taxonomyColor = record.colors[j];
          taxonomyLevel = _findChild(currentNode, taxonomyItem);
          if (!taxonomyLevel) {
            taxonomyLevel = {
              name: taxonomyItem,
              color: taxonomyColor,
              size: 0,
              children: []
            };
            currentNode.children.push(taxonomyLevel);
          }
          taxonomyLevel.size += record.probe;
          currentNode = taxonomyLevel;
        }
      }

      return root.children;
    };

    return _transform;
  }
);