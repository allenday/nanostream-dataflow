# Nanostream Dataflow

### Project tructure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision autoscaled HTTP service for alignment (based on bwa)
- simulator - python script that can simulate file uploads to GCS
- fasta_formatter - python script for formatting fasta files into project readable format
- visualization - module for the visualization of results
- doc - additional files for documentation

### Setup

To run all Nanostream system you shuld make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/)
2) Create [Google Cloud Storage](https://cloud.google.com/storage/) **dest bucket** for adding fastq files. 
You can use ([this python module](https://github.com/Pseverin/nanostream-dataflow/blob/master/simulator)) to provide a simulation of an adding fastq files
3) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket
```
gsutil notification create -t (pub_sub_topic_name) -f json -e OBJECT_FINALIZE (adding_fastq_files_simulation_bucket)
```
4) Create **PubSub subscription** for topic created at Step 3
5) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data

Optional:
6) If you running the pipeline in *resistant_genes* mode you should provide *fasta db* and *gene list* files stored at the GCS bucket

### Build
You can't skip this step and run project with [pre-built jar file](https://github.com/Pseverin/nanostream-dataflow/tree/refactoring/NanostreamDataflowMain/build)
If you want build jar file by yourself, you shuld implement next steps:
1) Install [Maven](https://maven.apache.org/install.html)
2) Add [Japsa](https://github.com/mdcao/japsa) package to local Maven repository. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.7-10a -Dpackaging=jar
```
3) Build fat-jar file

### Running
To start **Nanostream Pipeline** you should run following command:
```
java -cp (path_to_nanostream_app_jar) \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner `# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)` \
  --project=upwork-nano-stream `# Google Cloud Project name` \
  --streaming=true `# should be true for streaming (infinite) mode` \
  --processingMode=resistant_genes `# pecifies "species" or "resistant_genes" mode of data processing` \
  --inputDataSubscription=projects/upwork-nano-stream/subscriptions/resistant_fastq_paths_emitter_x1_subscription_1 `# PubSub subscription name from step 4` \
  --alignmentWindow=20 `# Size of the window in which FastQ records will be collected for Alignment` \
  --statisticUpdatingDelay=30 `# Delay between updating output statistic data` \
  --servicesUrl=http://130.211.33.64 `# Base URL for http services (Aligner and K-Align)` \
  --bwaEndpoint=/cgi-bin/bwa.cgi `# Aligner endpoint` \
  --bwaDatabase=DB.fasta `# Aligner DB name` \
  --kAlignEndpoint=/cgi-bin/kalign.cgi `# K-Align endpoint` \
  --outputFirestoreDbUrl=https://upwork-nano-stream.firebaseio.com `# Firestore DB url from step 5` \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic `# Collection name of the Firestore database that will be used for writing output statistic data` \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies `# Collection name of the Firestore database that will be used for writing output Sequences Body data` \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache `# Collection name of the Firestore database that will be used for saving NCBI genome data cache` \
  --resistantGenesFastDB=gs://nano-stream-test/gene_info/DB_resistant_formatted.fasta `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes database (step 6)` \
  --resistantGenesList=gs://nano-stream-test/gene_info/resistant_genes_list.txt `# OPTOPNAL Only for resistant_genes mode. Path to fasta file with resistant genes list(step 6)` 
```


TODO:
- simulation of fastq files (Done)
- change allenday/bwa-http-docker to listen on port 80 without using self-signed certificate (Done)
- load balancer and autoscaling group for aligner (Done)
- wrap [minimap2](https://github.com/lh3/minimap2) into HTTP. (Not clear, how to output SAM file)
