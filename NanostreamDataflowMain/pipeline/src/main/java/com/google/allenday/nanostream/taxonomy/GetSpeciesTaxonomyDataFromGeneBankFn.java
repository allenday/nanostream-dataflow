package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.nanostream.db.FirestoreService;
import com.google.allenday.nanostream.genebank.FirestoreGeneCacheDataSource;
import com.google.allenday.nanostream.genebank.GeneBankRepository;
import com.google.allenday.nanostream.genebank.NCBIDataSource;
import com.google.allenday.nanostream.geneinfo.TaxonData;
import com.google.allenday.nanostream.util.HttpHelper;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.io.IOException;
import java.util.Optional;

/**
 *
 */
public class GetSpeciesTaxonomyDataFromGeneBankFn extends DoFn<String, KV<String, TaxonData>> {

    private GeneBankRepository geneBankRepositoryOpt;

    private String firestoreDestCollection;
    private String projectId;

    public GetSpeciesTaxonomyDataFromGeneBankFn(String firestoreDestCollection, String projectId) {
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    @Setup
    public void setup() {
        try {
            geneBankRepositoryOpt = new GeneBankRepository(new NCBIDataSource(new HttpHelper()),
                    new FirestoreGeneCacheDataSource(FirestoreService.initialize(projectId),
                            firestoreDestCollection));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String geneName = c.element();
        c.output(KV.of(geneName, new TaxonData(Optional.ofNullable(geneBankRepositoryOpt).map(bank -> bank.getHierarchyByName(geneName)).orElse(null))));
    }
}
