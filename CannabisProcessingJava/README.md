## Cannabis-3k Processing Pipeline

This module contains the pipeline that processes Cannabis-3k dataset from [FASTQ](https://en.wikipedia.org/wiki/FASTQ_format) files to [VCF](https://en.wikipedia.org/wiki/Variant_Call_Format) files against provided [FASTA](https://en.wikipedia.org/wiki/FASTA_format) references.
Processing includes next steps:
1. Reading samples metadata from CSV files with SRA format
2. Downloading samples FASTQ files and FASTA references
3. Aligning FASTQ files and create SAM files
4. Converting SAM into BAM
5. Sorting BAM files content
6. Merging samples from the same SRA samples
7. Indexing BAM files
8. Variant Calling with [Deep Variant](https://github.com/google/deepvariant)
9. Exporting VCF files into big query with [gcp-variant-transforms](https://github.com/googlegenomics/gcp-variant-transforms)

### Example

#### Per-sample (fastq => bigquery) processing job:
```bash
mvn clean package
java -cp target/NanostreamCannabis-0.0.2.jar \
    com.google.allenday.nanostream.cannabis.NanostreamCannabisApp \
        --project=cannabis-3k \
        --region="us-central1" \
        --workerMachineType=n1-standard-4 \
        --maxNumWorkers=20 \
        --numWorkers=5 \
        --runner=DataflowRunner \
        --srcBucket=cannabis-3k \
        --stagingLocation="gs://dataflow-temp-and-staging/staging/"\
        --tempLocation="gs://dataflow-temp-and-staging/temp/"\
        --inputCsvUri="gs://cannabis-3k/sra_csv_index/converted/*" \
        --sraSamplesToFilter="SRS1098403" \
        --resultBucket=cannabis-3k-results \
        --referenceNamesList="AGQN03","MNPR01" \
        --allReferencesDirGcsUri="gs://cannabis-3k/reference/" \
        --outputDir="cannabis_processing_output/" \
        --memoryOutputLimit=0 \
        --controlPipelineWorkerRegion="us-west2" \
        --stepsWorkerRegion="us-central1" \
        --makeExamplesWorkers=4 \
        --makeExamplesCoresPerWorker=16 \
        --makeExamplesRamPerWorker=60 \
        --makeExamplesDiskPerWorker=90 \
        --callVariantsCoresPerWorker=16 \
        --callVariantsRamPerWorker=60 \
        --callVariantsDiskPerWorker=60 \
        --deepVariantShards=4 \
        --exportVcfToBq=True \
        --vcfBqDatasetAndTablePattern="cannabis_3k_results_test.GENOMICS_VARIATIONS_%s"
```

#### Per-sample (fastq => vcf) processing job:
TODO
```
# Simply set  paramater `exportVcfToBq=False` of the fastq=>bigquery pipeline:
--exportVcfToBq=False
```

#### Group-of-samples (vcf => bigquery) processing job:
```bash
mvn clean package
java -cp target/NanostreamCannabis-0.0.2.jar \
    com.google.allenday.nanostream.cannabis.vcf_to_bq.NanostreamCannabisBatchVcfToBqApp \
        --project=cannabis-3k \
        --region="us-central1" \
        --workerMachineType=n1-standard-4 \
        --maxNumWorkers=20 \
        --runner=DataflowRunner \
        --stagingLocation="gs://dataflow-temp-and-staging/staging/"\
        --tempLocation="gs://dataflow-temp-and-staging/temp/"\
        --resultBucket="cannabis-3k-results" \
        --outputDir="cannabis_processing_output/" \
        --referenceNamesList="AGQN03" \
        --allReferencesDirGcsUri="gs://cannabis-3k/reference/" \
        --vcfBqDatasetAndTablePattern="cannabis_3k_results_test.GENOMICS_VARIATIONS_%s" \
        --srcBucket=cannabis-3k-results \
        --vcfPathPattern="cannabis_processing_staged/vcf_to_bq/ready_to_export/%s/" \
        --workingBucket="cannabis-3k-results" \
        --workingDir="vcf-to-bq-working/"
```



### SRA CSV file content example
```csv
Assay_Type	AvgSpotLen	BioSample	Center_Name	DATASTORE_provider	DATASTORE_region	Experiment	InsertSize	Instrument	LibraryLayout	LibrarySelection	LibrarySource	Library_Name	LoadDate	MBases	MBytes	Organism	Platform	ReleaseDate	Run	SRA_Sample	Sample_Name	cultivar	dev_stage	geo_loc_name	tissue	BioProject	BioSampleModel	Consent	DATASTORE_filetype	SRA_Study	age
WGS	100	SAMN00738286	UNIVERSITY OF TORONTO	gs s3 sra-sos	gs.US s3.us-east-1 sra-sos.be-md sra-sos.st-va	SRX100361	0	Illumina HiSeq 2000	SINGLE	size fractionation	GENOMIC	CS-US_SIL-1	2012-01-21	5205	3505	Cannabis sativa	ILLUMINA	2011-10-12	SRR351492	SRS266493	CS-USO31-DNA	USO-31	missing	missing	young leaf	PRJNA73819	Plant	public	sra	SRP008673	missing
WGS	100	SAMN00738286	UNIVERSITY OF TORONTO	gs s3 sra-sos	gs.US s3.us-east-1 sra-sos.be-md sra-sos.st-va	SRX100368	0	Illumina HiSeq 2000	SINGLE	size fractionation	GENOMIC	CS-US_SIL-2	2012-01-21	2306	1499	Cannabis sativa	ILLUMINA	2011-10-12	SRR351493	SRS266493	CS-USO31-DNA	USO-31	missing	missing	young leaf	PRJNA73819	Plant	public	sra	SRP008673	missing
```
[You can get one of these from URL XXXXX](XXXXX)


## Docs in general terms

### Running
Use following commands to run processing:

```
mvn clean package
java -cp target/NanostreamCannabis-(current_version).jar \
    com.google.allenday.nanostream.cannabis.NanostreamCannabisApp \
        --project=(gcp_project_id) \
        --region=(gcp_worker_machines_region) \
        --workerMachineType=(gcp_worker_machines_type) \
        --maxNumWorkers=(max_number_of_workers) \
        --numWorkers=(start_number_of_workers) \
        --runner=(apache_beam_runner) \
        --stagingLocation=(dataflow_gcs_staging_location) \
        --tempLocation=(dataflow_gcs_temp_location) \
        --srcBucket=(all_source_data_gcs_bucket) \
        --inputCsvUri=(gcs_uri_of_sra_csv_file) \ # Could be pattern with wildcards (*) 
        --resultBucket=(gcs_bucket_for_writing_results) \
        --outputDir=(gcs_dir_path_for_writing_results) \
        --referenceNamesList=(list_of_references_names_that will_be_used) \
        --allReferencesDirGcsUri=(gcs_uri_of_dir_with_references) \
```
If VCF results should be exported to BigQuery you need to add following arguments:
```
        --exportVcfToBq=True \
        --vcfBqDatasetAndTablePattern=(bq_dataset_and_table_name_pattern)
```

Also could be added optional parameters:
```
        --sraSamplesToFilter=(list_of_sra_samles_to_process) \ # Use if need to process some certain samples
        --memoryOutputLimit=(max_file_size_to_pass_internaly_between_transforms) \ # Set 0 to store all intermediante files in GCS \
```

Optional [DeepVariant](https://github.com/google/deepvariant) parameters:

```
        --controlPipelineWorkerRegion=(region_of_deep_varint_control_pipeline) \
        --stepsWorkerRegion=(region_of_deep_varint_steps_machines) \
        --makeExamplesWorkers=(number) \
        --makeExamplesCoresPerWorker=(number) \
        --makeExamplesRamPerWorker=(number) \
        --makeExamplesDiskPerWorker=(number) \
        --call_variants_workers=(number)
        --callVariantsCoresPerWorker=(number) \
        --callVariantsRamPerWorker=(number) \
        --callVariantsDiskPerWorker=(number) \
        --postprocess_variants_cores=(number) \
        --postprocess_variants_ram_gb=(number) \
        --postprocess_variants_disk_gb=(number) \
        --preemptible=(bool) \
        --max_premptible_tries=(number) \
        --max_non_premptible_tries=(number) \
        --deepVariantShards=(number) \
```

### Batch export to BigQuery
This module contains a pipeline designed for processing of multiple VCF files (batches). Use following commands to run batch processing:
```
mvn clean package
java -cp target/NanostreamCannabis-(current_version).jar \
    com.google.allenday.nanostream.cannabis.vcf_to_bq.NanostreamCannabisBatchVcfToBqApp \
        --project=(gcp_project_id) \
        --region=(gcp_worker_machines_region) \
        --workerMachineType=(gcp_worker_machines_type) \
        --maxNumWorkers=(max_number_of_workers) \
        --numWorkers=(start_number_of_workers) \
        --runner=(apache_beam_runner) \
        --stagingLocation=(dataflow_gcs_staging_location) \
        --tempLocation=(dataflow_gcs_temp_location) \
        --resultBucket=(gcs_bucket_for_writing_results) \
        --outputDir=(gcs_dir_path_for_writing_results) \
        --referenceNamesList=(list_of_references_names_that will_be_used) \
        --allReferencesDirGcsUri=(gcs_uri_of_dir_with_references) \
        --srcBucket=(vcf_data_gcs_bucket) \
        --vcfBqDatasetAndTablePattern=(bq_dataset_and_table_name_pattern) \
        --vcfPathPattern=(pattern_of_vcf_files_path) \
        --workingBucket=(gcs_bucket_for_storing_working_files) \
        --workingDir=(gcs_dir_for_storing_working_files) 
```
 


