# Nanostream Dataflow

[Visualiser](https://nano-stream1.appspot.com/)

[PubSub Diagnostics](https://upload-watcher-dot-nano-stream1.appspot.com/)
Typical genomic analyses are too slow, taking weeks or months to complete. You transport DNA samples from the collection point to a centralized facility to be sequenced and analyzed in a batch process. Recently, nanopore DNA sequencers have become commercially available, such as those from Oxford Nanopore Technologies, streaming raw signal-level data as they are collected and providing immediate access to it. However, processing the data in real-time remains challenging,  requiring substantial compute and storage resources, as well as a dedicated bioinformatician. Not only is the process is too slow, it’s also failure-prone, expensive, and doesn’t scale.

This source repo contains a prototype implementation of a scalable, reliable, and cost effective end-to-end pipeline for fast DNA sequence analysis using Dataflow on Google Cloud.

## Design

![architecture](doc/Taxonomy%20Counting.png)

### Setup

To run the pipeline take the following steps:

1. Create a [Google Cloud Project](https://cloud.google.com/)
2. Create a [Google Cloud Storage](https://cloud.google.com/storage/) `$UPLOAD_BUCKET` **upload_bucket for FastQ files**.
3. You can use our [simulator](https://github.com/allenday/nanostream-dataflow/blob/master/simulator) to upload FastQ for testing, or if you don't have a real dataset.
4. Create a PubSub Topic in GUI. For example, file_upload.
5. Configure [file upload notifications]((https://cloud.google.com/storage/docs/pubsub-notifications)). This creates PubSub messages when new files are uploaded. Set up PubSub notifications:
```
gsutil notification create \
-t <topic name> -f json \
-e OBJECT_FINALIZE \
-p <subfolder path> gs://<bucket name>
```
6. Create a **PubSub subscription**:
```
gcloud pubsub subscriptions create <subscription name> --topic <topic name>
```
7. Provision an aligner cluster, see [aligner](aligner)
8. Create a **Cloud Firestore DB** ([See details in section Create a Cloud Firestore project](https://cloud.google.com/firestore/docs/quickstart-mobile-web#create_a_project)) for saving cache and result data.
9. *optional* If you running the pipeline in `resistance_genes` mode you should provide "FASTA DB" and "gene list" files stored in GCS.

### Project Structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations
- aligner - scripts to provision auto-scaled HTTP service for alignment (based on `bwa`)
- simulator - python script that can simulate file uploads to GCS
- fasta_formatter - python script for formatting fasta files into project readable format
- visualization - module for the visualization of results
- doc - additional files for documentation
- monitoring - monitors whether pubsub messages are working correctly.

### Setup
To run all Nanostream system you should make next steps:
1) Create [Google Cloud Project](https://cloud.google.com/)
2) Create [Google Cloud Storage](https://cloud.google.com/storage/) **destination bucket** for adding fastq files.
You can use ([this python module](https://github.com/allenday/nanostream-dataflow/blob/master/simulator)) to provide a simulation of an adding fastq files
3) Create **PubSub notifications**  ([See details](https://cloud.google.com/storage/docs/pubsub-notifications)) for **simulator dest bucket** that will be creating notifications when new files will have be added to bucket


<details><summary>CGI example</summary><p>

```
gsutil notification create \
-t file_upload -f json \
-e OBJECT_FINALIZE \
-p Uploads/ gs://nano-stream1
```

</p></details>


4) Create **Firestore DB** ([See details](https://firebase.google.com/products/firestore/)) for saving cache and result data

Run provisioning scripts:

5) On Google Cloud Shell, `git clone https://github.com/Firedrops/nanostream-dataflow.git`.
6) `cd /nanostream-dataflow/aligner/`
7) `bash provision_species.sh` or `bash provision_gene_resistance.sh`

8) install java8 on Gcloud
[instructions here](https://tecadmin.net/install-java-8-on-debian/)

### Running the Pipeline

We provide a [pre-built jar file](https://github.com/allenday/nanostream-dataflow/blob/master/NanostreamDataflowMain/build/). See below if you want to build the jar yourself.

To run the pipeline, first define variables for configuration:
```
# Google Cloud project name
PROJECT=`gcloud config get-value project`
# Apache Beam Runner (set org.apache.beam.runners.dataflow.DataflowRunner for running in a Google Cloud Dataflow or org.apache.beam.runners.direct.DirectRunner for running locally on your computer)
RUNNER=org.apache.beam.runners.dataflow.DataflowRunner

# specify mode of data processing (species, resistance_genes)
PROCESSING_MODE=species

9) If you running the pipeline in *resistant_genes* mode you should provide *fasta db* and *gene list* files stored at the GCS bucket
# PubSub subscription defined above
UPLOAD_SUBSCRIPTION=$UPLOAD_SUBSCRIPTION

# size of the window (in wallclock seconds) in which FastQ records will be collected for alignment
ALIGNMENT_WINDOW=20
# how frequently (in wallclock seconds) are statistics updated for dashboard visualizaiton?
STATS_UPDATE_FREQUENCY=30

# IP address of the aligner cluster created by running aligner/provision_species.sh
SPECIES_ALIGNER_CLUSTER_IP=$(gcloud compute forwarding-rules describe bwa-species-forward --global --format="value(IPAddress)")
# IP address of the aligner cluster created by running aligner/provision_resistance_genes.sh
RESISTANCE_GENES_ALIGNER_CLUSTER_IP=$(gcloud compute forwarding-rules describe bwa-resistance-genes --global --format="value(IPAddress)")
# base URL for http services (bwa and kalign)
# value for species, for resistance_genes use 'SERVICES_HOST=http://$RESISTANCE_GENES_ALIGNER_CLUSTER_IP'
SERVICES_HOST=http://$SPECIES_ALIGNER_CLUSTER_IP
# bwa path
BWA_ENDPOINT=/cgi-bin/bwa.cgi
# bwa database name
BWA_DATABASE=DB.fasta
# kalign path
KALIGN_ENDPOINT=/cgi-bin/kalign.cgi

# Collections name prefix of the Firestore database that will be used for writing results
FIRESTORE_COLLECTION_NAME_PREFIX=new_scanning
```
If you run the pipeline in the `resistance_genes` mode you should add 2 additional arguments with paths of files stored in the GCS. With a placeholder name `$FILES_BUCKET` add next arguments:
1. Path to resistant genes sequence `fasta` list formatted with [fasta formatter](https://github.com/allenday/nanostream-dataflow/tree/master/fasta_formatter):
```
# Path to resistant genes sequence fasta list formatted with fasta formatter
RESISTANCE_GENES_FASTA=gs://$FILES_BUCKET/gene-info/DB_resistant_formatted.fasta
```
2. Path to text file with resistant genes references and groups:
```
# Path to text file with resistant genes references and groups
RESISTANCE_GENES_LIST=gs://$FILES_BUCKET/gene-info/resistance_genes_list.txt
```

To start **Nanostream Pipeline** run following command:

```
java -cp (path_to_nanostream_app_jar) \
  com.theappsolutions.nanostream.NanostreamApp \
  --runner=$RUNNER \
  --project=$PROJECT \
  --streaming=true \
  --processingMode=$PROCESSING_MODE \
  --inputDataSubscription=$UPLOAD_SUBSCRIPTION \
  --alignmentWindow=$ALIGNMENT_WINDOW \
  --statisticUpdatingDelay=$STATS_UPDATE_FREQUENCY \
  --servicesUrl=$SERVICES_HOST \
  --bwaEndpoint=$BWA_ENDPOINT \
  --bwaDatabase=$BWA_DATABASE \
  --kAlignEndpoint=$KALIGN_ENDPOINT \
  --outputFirestoreCollectionNamePrefix=$FIRESTORE_COLLECTION_NAME_PREFIX \
  --resistanceGenesFastDB=$RESISTANCE_GENES_FASTA \
  --resistanceGenesList=$RESISTANCE_GENES_LIST
  --region=REGION
```

<details><summary>CGI species bwa</summary><p>

```
java -cp /home/coingroupimb/nanostream-dataflow/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=species \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow_species \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=http://35.241.45.217/ \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=genomeDB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreCollectionNamePrefix=new_scanning
```
</p></details>

<details><summary>CGI species mm2</summary><p>

```
java -cp /home/coingroupimb/nanostream-dataflow/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=species \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow_species_mm2 \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=35.201.96.177 \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=genomeDB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreCollectionNamePrefix=new_scanning
```
</p></details>

<details><summary>CGI resistance</summary><p>

```
java -cp /home/coingroupimb/nanostream-dataflow/NanostreamDataflowMain/build/NanostreamDataflowMain.jar \
  com.theappsolutions.nanostream.NanostreamApp \
  --region=asia-northeast1 \
  --runner=org.apache.beam.runners.dataflow.DataflowRunner \
  --project=nano-stream1 \
  --streaming=true \
  --processingMode=resistance_genes \
  --inputDataSubscription=projects/nano-stream1/subscriptions/dataflow_resistance \
  --alignmentWindow=20 \
  --statisticUpdatingDelay=30 \
  --servicesUrl=http://35.201.96.177 \
  --bwaEndpoint=/cgi-bin/bwa.cgi \
  --bwaDatabase=genomeDB.fasta \
  --kAlignEndpoint=/cgi-bin/kalign.cgi \
  --outputFirestoreCollectionNamePrefix=new_scanning
  --resistanceGenesFastaDB=gs://nano-stream1/NewDatabases/DB_resistant_formatted.fasta \
  --resistanceGenesList=gs://nano-stream1/NewDatabases/resistant_genes_list.txt \
  --outputFirestoreCollectionNamePrefix=new_scanning
```
  --outputFirestoreDbUrl=https://nano-stream1.firebaseio.com \
  --outputFirestoreSequencesStatisticCollection=resistant_sequences_statistic \
  --outputFirestoreSequencesBodiesCollection=resistant_sequences_bodies \
  --outputFirestoreGeneCacheCollection=resistant_gene_cache \
</p></details>

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

**nano-stream-data** - is a public bucket with [requester pays](https://cloud.google.com/storage/docs/requester-pays) option enabled.

### Cleanup

To clear previously-generated services and remnants,
See steps 6 - 8 above, except `bash provision_species.sh -c` or `bash provision_gene_resistance.sh -c`

### TODO:
1) docker build with recipe
1) mnt gs bucket references instead of cp
2) minimap2 pipeline
3) sessioning?
**nanostream-dataflow-demo-data** - is a public bucket with [requester pays](https://cloud.google.com/storage/docs/requester-pays) option enabled.

### Building from Source

To build jar from source, follow next steps:
1) Use Java 1.8. Dataflow does not yet support 1.9 or greater.
2) Install [Maven](https://maven.apache.org/install.html)
3) Add [**Japsa 1.9-2b**](https://github.com/mdcao/japsa) package to local Maven repository. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-2b -Dpackaging=jar
```
4) Build uber-jar file
```
cd NanostreamDataflowMain
mvn clean package
```
after running this command successfully, there should be a file:
"NanostreamDataflowMain/target/NanostreamDataflowMain-1.0-SNAPSHOT.jar"
