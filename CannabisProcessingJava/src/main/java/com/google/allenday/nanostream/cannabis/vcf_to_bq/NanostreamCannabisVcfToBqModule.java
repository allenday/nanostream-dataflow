package com.google.allenday.nanostream.cannabis.vcf_to_bq;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.pipeline.GenomicsOptions;
import com.google.allenday.genomics.core.processing.lifesciences.LifeSciencesService;
import com.google.allenday.genomics.core.processing.vcf_to_bq.PrepareVcfToBqBatchFn;
import com.google.allenday.genomics.core.processing.vcf_to_bq.VcfToBqBatchTransform;
import com.google.allenday.genomics.core.processing.vcf_to_bq.VcfToBqFn;
import com.google.allenday.genomics.core.processing.vcf_to_bq.VcfToBqService;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

//TODO

/**
 *
 */
public class NanostreamCannabisVcfToBqModule extends AbstractModule {


    private String workingBucket;
    private String workingDir;
    private String project;
    private String region;
    private GenomicsOptions genomicsOptions;

    public NanostreamCannabisVcfToBqModule(Builder builder) {
        this.genomicsOptions = builder.genomicsOptions;
        this.workingBucket = builder.workingBucket;
        this.workingDir = builder.workingDir;
        this.project = builder.project;
        this.region = builder.region;
    }

    public static class Builder {

        private GenomicsOptions genomicsOptions;
        private String workingBucket;
        private String workingDir;
        private String project;
        private String region;

        public Builder setGenomicsOptions(GenomicsOptions genomicsOptions) {
            this.genomicsOptions = genomicsOptions;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineVcfToBqOptions nanostreamPipelineOptions) {
            region = nanostreamPipelineOptions.getRegion();
            project = nanostreamPipelineOptions.getProject();
            setGenomicsOptions(GenomicsOptions.fromAlignerPipelineOptions(nanostreamPipelineOptions));

            workingBucket = nanostreamPipelineOptions.getWorkingBucket();
            workingDir = nanostreamPipelineOptions.getWorkingDir();

            return this;
        }

        public NanostreamCannabisVcfToBqModule build() {
            return new NanostreamCannabisVcfToBqModule(this);
        }

    }

    @Provides
    @Singleton
    public NameProvider provideNameProvider() {
        return NameProvider.initialize();
    }


    @Provides
    @Singleton
    public FileUtils provideFileUtils() {
        return new FileUtils();
    }


    @Provides
    @Singleton
    public LifeSciencesService provideLifeSciencesService() {
        return new LifeSciencesService();
    }


    @Provides
    @Singleton
    public VcfToBqFn provideVcfToBqFn(VcfToBqService vcfToBqService, FileUtils fileUtils) {

        return new VcfToBqFn(vcfToBqService, fileUtils);
    }


    @Provides
    @Singleton
    public VcfToBqService provideVcfToBqService(LifeSciencesService lifeSciencesService, NameProvider nameProvider) {
        VcfToBqService vcfToBqService = new VcfToBqService(lifeSciencesService, String.format("%s:%s", project, genomicsOptions.getVcfBqDatasetAndTablePattern()),
                genomicsOptions.getResultBucket(), genomicsOptions.getVcfToBqOutputDir(), nameProvider.getCurrentTimeInDefaultFormat());
        vcfToBqService.setRegion(region);
        return vcfToBqService;
    }


    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils, genomicsOptions.getAllReferencesDirGcsUri());
    }

    @Provides
    @Singleton
    public PrepareVcfToBqBatchFn providePrepareVcfToBqBatchFn(ReferencesProvider referencesProvider, FileUtils fileUtils, NameProvider nameProvider) {

        return new PrepareVcfToBqBatchFn(referencesProvider, fileUtils, workingBucket, workingDir, nameProvider.getCurrentTimeInDefaultFormat());
    }

    @Provides
    @Singleton
    public VcfToBqBatchTransform provideVcfToBqBatchTransform(PrepareVcfToBqBatchFn prepareVcfToBqBatchFn, VcfToBqFn vcfToBqFn) {

        return new VcfToBqBatchTransform(prepareVcfToBqBatchFn, vcfToBqFn);
    }
}
