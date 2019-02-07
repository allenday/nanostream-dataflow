# Nanostream Dataflow

### Project structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision autoscaled HTTP service for alignment (based on bwa)
- simulator - python script that can simulate file uploads to GCS
- fasta_formatter - python script for formatting fasta files into project readable format
- visualization - module for the visualization of results
- doc - additional files for documentation

### Setup
To run all Nanostream system you should make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/)
2) Create [Google Cloud Storage](https://cloud.google.com/storage/) **dest bucket** for adding fastq files. 
You can use ([this python module](https://github.com/Pseverin/nanostream-dataflow/blob/master/simulator)) to provide a simulation of an adding fastq files
3) Create **PubSub subscription** for topic created at Step 3
4) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket
```
gsutil notification create \
-t FILE_UPLOAD -f json \
-e OBJECT_FINALIZE \
-p <sub-folder>/ gs://<project_id>
```
5) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data

Run provisioning scripts: 

6) On Google Cloud Shell, `git clone https://github.com/Firedrops/nanostream-dataflow.git`.
7) `cd /nanostream-dataflow/aligner/`
8) `bash provision_species.sh` or `bash provision_gene_resistance.sh`

Optional:

9) If you running the pipeline in *resistant_genes* mode you should provide *fasta db* and *gene list* files stored at the GCS bucket

### Build
You can skip this step and run project with [pre-built jar file](https://github.com/allenday/nanostream-dataflow/blob/master/NanostreamDataflowMain/build/)
To build jar from source, follow next steps:
1) Install [Maven](https://maven.apache.org/install.html)
2) Add [Japsa](https://github.com/mdcao/japsa) package to local Maven repository. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.7-10a -Dpackaging=jar
```
3) Build uber-jar file
... TODO

### Running
To start **Nanostream Pipeline** you should run following command:
```
java -cp (path_to_nanostream_app_jar) \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
  --project=nano-stream1 `# Google Cloud Project name` \
  --streaming=true `# should be true for streaming (infinite) mode` \
  --processingMode=species `# specifies "species" or "resistant_genes" mode of data processing` \
  --inputDataSubscription=projects/nano-stream1/topics/file_upload `# PubSub subscription name from step 4` \
  --alignmentWindow=20 `# Size of the window in which FastQ records will be collected for Alignment` \
  --statisticUpdatingDelay=30 `# Delay between updating output statistic data` \
  --servicesUrl=http://34.85.27.91 `# Base URL for http services (Aligner and K-Align)` TODOTODO \ 
  --bwaEndpoint=/cgi-bin/bwa.cgi `# Aligner endpoint` \
  --bwaDatabase=DB.fasta `# Aligner DB name` \
  --kAlignEndpoint=/cgi-bin/kalign.cgi `# K-Align endpoint` \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com `# Firestore DB url from step 5` \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic `# Collection name of the Firestore database that will be used for writing output statistic data` \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies `# Collection name of the Firestore database that will be used for writing output Sequences Body data` \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache `# Collection name of the Firestore database that will be used for saving NCBI genome data cache` \
  --resistantGenesFastDB=gs://nano-stream-test/gene_info/DB_resistant_formatted.fasta `# OPTIONAL Only for resistant_genes mode. Path to fasta file with resistant genes database (step 6)` \
  --resistantGenesList=gs://nano-stream-test/gene_info/resistant_genes_list.txt `# OPTIPNAL Only for resistant_genes mode. Path to fasta file with resistant genes list(step 6)` 
```

<details><summary>species</summary><p>
  
```
java -cp /home/coingroupimb/git larry 2019-02-06/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=species \
  --inputDataSubscription=projects/nano-stream1/topics/file_upload \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=http://34.85.27.91 \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=DB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache \
```
  
</p></details>

<details><summary>resistance</summary><p>
  
```
java -cp /home/coingroupimb/git larry 2019-02-06/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
  --project=nano-stream1 `# Google Cloud Project name` \
  --streaming=true `# should be true for streaming (infinite) mode` \
  --processingMode=species `# specifies "species" or "resistant_genes" mode of data processing` \
  --inputDataSubscription=projects/nano-stream1/topics/file_upload `# PubSub subscription name from step 4` \
  --alignmentWindow=20 `# Size of the window in which FastQ records will be collected for Alignment` \
  --statisticUpdatingDelay=30 `# Delay between updating output statistic data` \
  --servicesUrl=http://34.85.27.91 `# Base URL for http services (Aligner and K-Align)` \ 
  --bwaEndpoint=/cgi-bin/bwa.cgi `# Aligner endpoint` \
  --bwaDatabase=DB.fasta `# Aligner DB name` \
  --kAlignEndpoint=/cgi-bin/kalign.cgi `# K-Align endpoint` \
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com `# Firestore DB url from step 5` \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic `# Collection name of the Firestore database that will be used for writing output statistic data` \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies `# Collection name of the Firestore database that will be used for writing output Sequences Body data` \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache `# Collection name of the Firestore database that will be used for saving NCBI genome data cache` \
  --resistantGenesFastDB=gs://nano-stream-1/NewDatabases/DB_resistant_formatted.fasta `# OPTIONAL Only for resistant_genes mode. Path to fasta file with resistant genes database (step 6)` \
  --resistantGenesList=gs://nano-stream1/NewDatabases/resistant_genes_list.txt `# OPTIONAL Only for resistant_genes mode. Path to fasta file with resistant genes list(step 6)`
```

</p></details>

### Cleanup

To previosly-generated services and remnants, 
See steps 6 - 8 above, except `bash provision_species.sh -c` or `bash provision_gene_resistance.sh -c`
