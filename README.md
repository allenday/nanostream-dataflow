# Nanostream Dataflow

### Project Structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision autoscaled HTTP service for alignment (based on `bwa`)
- simulator - python script that can simulate file uploads to GCS
- fasta_formatter - python script for formatting fasta files into project readable format
- visualization - module for the visualization of results
- doc - additional files for documentation

### Setup

To run all Nanostream system you should make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/)
2) Create [Google Cloud Storage](https://cloud.google.com/storage/) **destination bucket** for adding fastq files. 
You can use ([this python module](https://github.com/allenday/nanostream-dataflow/blob/master/simulator)) to provide a simulation of an adding fastq files
3) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket
```
gsutil notification create -t (pub_sub_topic_name) -f json -e OBJECT_FINALIZE (adding_fastq_files_simulation_bucket)
```
4) Create **PubSub subscription** for topic created at Step 3
5) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data

Optional:

6) If you running the pipeline in *resistant_genes* mode you should provide *fasta db* and *gene list* files stored at the GCS bucket

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

### Available databases
For this project the bucket **nano-stream-data** were created
with reference databases of species and antibiotic resistant genes.

There is following structure of files:
```
gs://nano-stream-data/
|- databases
|-- antibiotic_resistant
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
|-- species
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
```
where:
- DB.fasta - FASTA file with reference sequences
- DB.fasta.amb, DB.fasta.ann, DB.fasta.bwt, DB.fasta.pac, DB.fasta.sa - files generated and used by `bwa` in order to improve performance, see details in [this SEQanswers answer](http://seqanswers.com/forums/showpost.php?s=06f0dadc73bdf687f265a94c8217d0bd&p=90992&postcount=2)

**nano-stream-data** - is a public bucket with [requester pays](https://cloud.google.com/storage/docs/requester-pays) option enabled.
