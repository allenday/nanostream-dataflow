package com.google.allenday.nanostream.genebank;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DefaultCoder(SerializableCoder.class)
public class GeneBankRepository implements Serializable {

    private NCBIDataSource ncbiDataSource;
    private FirestoreGeneCacheDataSource firestoreGeneCacheDataSource;

    public GeneBankRepository(NCBIDataSource ncbiDataSource, FirestoreGeneCacheDataSource firestoreGeneCacheDataSource) {
        this.ncbiDataSource = ncbiDataSource;
        this.firestoreGeneCacheDataSource = firestoreGeneCacheDataSource;
    }

    public List<String> getHierarchyByName(String name) {
        try {
            GeneTaxonomyInfo geneTaxonomyInfo = firestoreGeneCacheDataSource.getTaxonomyFromFirestore(name);
            if (geneTaxonomyInfo.getSearchQuery() != null && geneTaxonomyInfo.getGeneLocusNCBI() == null) {
                throw new FirestoreGeneCacheDataSource.GeneCacheInfoMissedExeption();
            }
            return geneTaxonomyInfo.getTaxonomy();
        } catch (FirestoreGeneCacheDataSource.GeneCacheNotFoundExeption | FirestoreGeneCacheDataSource.GeneCacheInfoMissedExeption geneCacheNotFoundException) {
            GeneTaxonomyInfo geneTaxonomyInfo = ncbiDataSource.getTaxonomyFromNCBI(name);
            firestoreGeneCacheDataSource.saveTaxonomyToFirestore(geneTaxonomyInfo);
            return geneTaxonomyInfo.getTaxonomy();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
