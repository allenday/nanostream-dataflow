package com.google.allenday.nanostream.genebank;

import com.google.allenday.nanostream.output.FirestoreService;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;

@DefaultCoder(SerializableCoder.class)
public class FirestoreGeneCacheDataSource implements Serializable {

    private FirestoreService firestoreService;
    private String collectionName;

    public FirestoreGeneCacheDataSource(FirestoreService firestoreService, String collectionName) {
        this.firestoreService = firestoreService;
        this.collectionName = collectionName;
    }

    @Nonnull
    public GeneTaxonomyInfo getTaxonomyFromFirestore(String nanostreamGenomeName)
            throws ExecutionException, InterruptedException, GeneCacheNotFoundExeption {
        GeneTaxonomyInfo geneTaxonomyInfo = firestoreService
                .getObjectByDocumentId(collectionName, nanostreamGenomeName, GeneTaxonomyInfo.class);
        if (geneTaxonomyInfo != null) {
            return geneTaxonomyInfo;
        } else {
            throw new GeneCacheNotFoundExeption();
        }
    }

    public void saveTaxonomyToFirestore(GeneTaxonomyInfo geneTaxonomyInfo) {
        firestoreService.writeObjectToFirestoreCollection(collectionName, geneTaxonomyInfo.getGeneNameNanostream(),
                geneTaxonomyInfo);
    }


    public static class GeneCacheNotFoundExeption extends IOException {
        GeneCacheNotFoundExeption() {
            super("Gene name not found in Cache");
        }
    }

    public static class GeneCacheInfoMissedExeption extends IOException {
        GeneCacheInfoMissedExeption() {
            super();
        }
    }
}
