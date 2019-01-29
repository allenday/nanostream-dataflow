package com.theappsolutions.nanostream.taxonomy;

import com.theappsolutions.nanostream.genebank.FirestoreGeneCacheDataSource;
import com.theappsolutions.nanostream.genebank.GeneBankRepository;
import com.theappsolutions.nanostream.genebank.NCBIDataSource;
import com.theappsolutions.nanostream.output.FirestoreService;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class GetSpeciesTaxonomyDataFn extends DoFn<String, KV<String, List<String>>> {

    private GeneBankRepository geneBankRepositoryOpt;

    private String firestoreDatabaseUrl;
    private String firestoreDestCollection;
    private String projectId;

    public GetSpeciesTaxonomyDataFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        this.firestoreDatabaseUrl = firestoreDatabaseUrl;
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    @Setup
    public void setup() {
        try {
            geneBankRepositoryOpt = new GeneBankRepository(new NCBIDataSource(new HttpHelper()),
                    new FirestoreGeneCacheDataSource(FirestoreService.initialize(projectId, firestoreDatabaseUrl),
                            firestoreDestCollection));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String geneName = c.element();
        c.output(KV.of(geneName, Optional.ofNullable(geneBankRepositoryOpt).map(bank -> bank.getHierarchyByName(geneName)).orElse(null)));
    }
}
