# Nanostream Dataflow

### Project Structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision auto-scaled HTTP service for alignment (based on `bwa`)
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

### Running the Pipeline

We provide a [pre-built jar file](https://github.com/allenday/nanostream-dataflow/blob/master/NanostreamDataflowMain/build/). See below if you want to build the jar yourself.

To run the pipeline, first define variables for configuration:
```
# Google Cloud project name
PROJECT=`gcloud config get-value project`
# Apache Beam Runner (Dataflow for Google Cloud Dataflow running or Direct for local running)
RUNNER=org.apache.beam.runners.dataflow.DataflowRunner

# specify mode of data processing (species, resistance_genes)
PROCESSING_MODE=resistance_genes

# PubSub subscription name from step 4
INPUT_PUBSUB=projects/upwork-nano-stream/subscriptions/resistant_fastq_paths_emitter_x1_subscription_1

# size of the window (in wallclock seconds) in which FastQ records will be collected for alignment
ALIGNMENT_WINDOW=20
# how frequently (in wallclock seconds) are statistics updated for dashboard visualizaiton?
STATS_UPDATE_FREQUENCY=30

# base URL for http services (bwa and kalign)
SERVICES_HOST=http://130.211.33.64
# bwa path
BWA_ENDPOINT=/cgi-bin/bwa.cgi
# bwa database name
BWA_DATABASE=DB.fasta
# kalign path
KALIGN_ENDPOINT=/cgi-bin/kalign.cgi

# Firestore DB url from step 5
FIRESTORE_URL=https://upwork-nano-stream.firebaseio.com
# Collection name of the Firestore database that will be used for writing output statistic data
FIRESTORE_COLLECTION_STATS=resistance_sequences_statistic
# Collection name of the Firestore database that will be used for writing output Sequences Body data
FIRESTORE_COLLECTION_RESISTANCE_BODIES=resistance_sequences_bodies
# Collection name of the Firestore database that will be used for saving NCBI genome data cache
FIRESTORE_TAXONOMY_CACHE=resistance_gene_cache

# [optional] Only used in resistance_genes mode. Path to fasta file with resistance genes database (step 6)
RESISTANCE_GENES_FASTA=gs://nanostream-dataflow-demo-data/gene-info/DB_resistant_formatted.fasta
# [optional] Only used in resistance_genes mode. Path to fasta file with resistant genes list (step 6)
RESISTANCE_GENES_LIST=gs://nanostream-dataflow-demo-data/gene-info/resistance_genes_list.txt
```

To start **Nanostream Pipeline** run following command:
```
java -cp (path_to_nanostream_app_jar) \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=$RUNNER \
  --project=$PROJECT \
  --streaming=true \
  --processingMode=$PROCESSING_MODE \
  --inputDataSubscription=$INPUT_PUBSUB \
  --alignmentWindow=$ALIGNMENT_WINDOW \
  --statisticUpdatingDelay=$STATS_UPDATE_FREQUENCY \
  --servicesUrl=$SERVICES_HOST \
  --bwaEndpoint=$BWA_ENDPOINT \
  --bwaDatabase=$BWA_DATABASE \ 
  --kAlignEndpoint=$KALIGN_ENDPOINT \
  --outputFirestoreDbUrl=$FIRESTORE_URL \
  --outputFirestoreSequencesStatisticCollection=$FIRESTORE_COLLECTION_STATS \
  --outputFirestoreSequencesBodiesCollection=$FIRESTORE_COLLECTION_RESISTANCE_BODIES \
  --outputFirestoreGeneCacheCollection=$FIRESTORE_TAXONOMY_CACHE \
  --resistanceGenesFastDB=$RESISTANCE_GENES_FASTA \
  --resistanceGenesList=$RESISTANCE_GENES_LIST
```

### Available databases
For this project the bucket **nanostream-dataflow-demo-data** were created
with reference databases of species and antibiotic resistance genes.

The bucket has a structure like:
```
gs://nanostream-dataflow-demo-data/
|- reference-sequences/
|-- antibiotic-resistance-genes/
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
|-- species/
|--- DB.fasta
|--- DB.fasta.[amb,ann,bwt,pac,sa]
```
where:
- DB.fasta - FASTA file with reference sequences
- DB.fasta.amb, DB.fasta.ann, DB.fasta.bwt, DB.fasta.pac, DB.fasta.sa - files generated and used by `bwa` in order to improve performance, see details in [this SEQanswers answer](http://seqanswers.com/forums/showpost.php?s=06f0dadc73bdf687f265a94c8217d0bd&p=90992&postcount=2)

**nanostream-dataflow-demo-data** - is a public bucket with [requester pays](https://cloud.google.com/storage/docs/requester-pays) option enabled.

### Building from Source

To build jar from source, follow next steps:
1) Install [Maven](https://maven.apache.org/install.html)
2) Add [Japsa](https://github.com/mdcao/japsa) package to local Maven repository. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.7-10a -Dpackaging=jar
```
3) Build uber-jar file
```
cd NanostreamDataflowMain
mvn clean package
```
after running this command successfully, there should be a file:
"NanostreamDataflowMain/target/NanostreamDataflowMain-1.0-SNAPSHOT.jar"
