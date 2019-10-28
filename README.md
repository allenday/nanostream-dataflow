# Nanostream Dataflow

In a healthcare setting, being able to access data quickly is vital. For example, a sepsis patient’s survival rate decreases by 6% for every hour we fail to diagnose the species causing the infection and its antibiotic resistance profile.

Typical genomic analyses are too slow, taking weeks or months to complete. You transport DNA samples from the collection point to a centralized facility to be sequenced and analyzed in a batch process. Recently, nanopore DNA sequencers have become commercially available, such as those from Oxford Nanopore Technologies, streaming raw signal-level data as they are collected and providing immediate access to it. However, processing the data in real-time remains challenging,  requiring substantial compute and storage resources, as well as a dedicated bioinformatician. Not only is the process is too slow, it’s also failure-prone, expensive, and doesn’t scale.

This source repo contains a prototype implementation of a scalable, reliable, and cost effective end-to-end pipeline for fast DNA sequence analysis using Dataflow on Google Cloud.

## Design

![architecture](doc/Taxonomy%20Counting.png)

### Project Structure
- NanostreamDataflowMain - Apache Beam app that provides all data transformations:
    - pipleline - Apache Beam pipeline lib
    - main - Console app that runs Apache Beam pipeline
    - webapp - GCP appengine application 
- aligner - scripts to provision auto-scaled HTTP service for alignment (based on `bwa`)
- simulator - python script that can simulate file uploads to GCS
- utilities - utils for preprocessing of FASTA and FASTQ files
- visualization - module for the visualization of results
- launcher - installation scripts
- doc - additional files for documentation


### Setup

Before run automatic setup scripts or perform manual steps make sure you created [Google Cloud Project](https://console.cloud.google.com) and bounded it to a payment account. 

#### Automatic Setup

1. Open [Google Cloud Shell](https://ssh.cloud.google.com/)
2. Clone the project from Github
3. Build docker launcher
```
docker build -t launcher .
```
4. Run docker launcher
```
docker run -e GOOGLE_CLOUD_PROJECT=<project name> launcher
``` 

#### Manual Setup

To run the pipeline take the following steps:

1. Create a [Google Cloud Storage](https://cloud.google.com/storage/) `$UPLOAD_BUCKET` **upload_bucket for FastQ files**.
2. You can use our [simulator](https://github.com/allenday/nanostream-dataflow/blob/master/simulator) to upload FastQ for testing, or if you don't have a real dataset. If your source data are stored in a large (>1 MB) multi-strand FastQ file you can use [FastQ Splitter](https://github.com/allenday/nanostream-dataflow/blob/master/utilities/fastq_splitter) utility. It converts large multi-strand FastQ file into a set of single strand FastQ files and tsv file with strand timings. This transformation makes possible to work with batch source data in a streaming mode.
3. Configure [file upload notifications]((https://cloud.google.com/storage/docs/pubsub-notifications)). This creates PubSub messages when new files are uploaded. With our placeholder name `$UPLOAD_EVENTS`, set up PubSub notifications in the following way:
```
gsutil notification create -t $UPLOAD_EVENTS -f json -e OBJECT_FINALIZE $UPLOAD_BUCKET
```
4. Create a **PubSub subscription** for `$UPLOAD_EVENTS`. With our placeholder name `$UPLOAD_SUBSCRIPTION`, run following command:
```
gcloud pubsub subscriptions create $UPLOAD_SUBSCRIPTION --topic $UPLOAD_EVENTS
```
5. Create a **Cloud Firestore DB** ([See details in section Create a Cloud Firestore project](https://cloud.google.com/firestore/docs/quickstart-mobile-web#create_a_project)) for saving cache and result data.
6. *optional* If you running the pipeline in `resistance_genes` mode you should provide "gene list" file stored in GCS.


### Running the Pipeline

We provide a [pre-built jar file](https://github.com/allenday/nanostream-dataflow/blob/master/NanostreamDataflowMain/build/). See below if you want to build the jar yourself.

To run the pipeline, first define variables for configuration:
```
# Google Cloud project name
PROJECT=`gcloud config get-value project`
# Apache Beam Runner (set org.apache.beam.runners.dataflow.DataflowRunner for running in a Google Cloud Dataflow or org.apache.beam.runners.direct.DirectRunner for running locally on your computer)
RUNNER=org.apache.beam.runners.dataflow.DataflowRunner

# specify mode of data processing (species, resistance_genes)
PROCESSING_MODE=resistance_genes

# PubSub subscription defined above
UPLOAD_SUBSCRIPTION=$UPLOAD_SUBSCRIPTION

# size of the window (in wallclock seconds) in which FastQ records will be collected for alignment
ALIGNMENT_WINDOW=20
# how frequently (in wallclock seconds) are statistics updated for dashboard visualizaiton?
STATS_UPDATE_FREQUENCY=30


# Reference database name
REFERENCE_DATABASE=genomeDB
# Bucket for storing aligned files
ALIGN_RESULTS_BUCKET=$FILES_BUCKET
# GCS path to directory which will be used for storing aligned files. "%s" will be replaces with job time
ALIGNED_OUTPUT_DIR=clinic_processing_output/%s/result_aligned_bam/
# GCS path to directory with references db. References should be stored as ${ALL_REFERENCES_DIR_GCS_URI}${REFERENCE_DATABASE}/(reference_files)
ALL_REFERENCES_DIR_GCS_URI=gs://$FILES_BUCKET/references/


# Collections name prefix of the Firestore database that will be used for writing results
FIRESTORE_COLLECTION_NAME_PREFIX=new_scanning
```
If you run the pipeline in the `resistance_genes` mode you should add additional argument with path of file stored in the GCS. With a placeholder name `$FILES_BUCKET` add next argument:
```
# Path to text file with resistant genes references and groups
RESISTANCE_GENES_LIST=gs://$FILES_BUCKET/gene-info/resistance_genes_list.txt
```
**(Optional) Additional parameters**
You can manually specify some parameters such as *Alignment batch size*, *Memory output limit*, *Firestore document name prefix*:
```
# (OPTIONAL) Max size of batch that will be generated before alignment. Default value - 2000
ALIGNMENT_BATCH_SIZE=1000
# (OPTIONAL) Threshold to decide how to pass data after alignment
MEMORY_OUTPUT_LIMIT=100
# (OPTIONAL) Firestore database document name prefix that will be used for writing statistic results
FIRESTORE_DOCUMENT_NAME_PREFIX=statistic_document
```

To start **Nanostream Pipeline** run following command:
```
java -cp (path_to_nanostream_app_jar) \
  com.google.allenday.nanostream.NanostreamApp \
  --runner=$RUNNER \
  --project=$PROJECT \
  --streaming=true \
  --processingMode=$PROCESSING_MODE \
  --inputDataSubscription=$UPLOAD_SUBSCRIPTION \
  --alignmentWindow=$ALIGNMENT_WINDOW \
  --statisticUpdatingDelay=$STATS_UPDATE_FREQUENCY \
  --referenceNamesList=$REFERENCE_DATABASE \ 
  --outputCollectionNamePrefix=$FIRESTORE_COLLECTION_NAME_PREFIX \
  --outputDocumentNamePrefix=$FIRESTORE_DOCUMENT_NAME_PREFIX \
  --resistanceGenesList=$RESISTANCE_GENES_LIST \
  --resultBucket=$ALIGN_RESULTS_BUCKET \
  --alignedOutputDir=$ALIGNED_OUTPUT_DIR \
  --allReferencesDirGcsUri=$ALL_REFERENCES_DIR_GCS_URI \
  --memoryOutputLimit=$MEMORY_OUTPUT_LIMIT `# (Optional)`\
  --alignmentBatchSize=$ALIGNMENT_BATCH_SIZE `# (Optional)`\
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
1) Use Java 1.8. Dataflow does not yet support 1.9 or greater.
2) Install [Maven](https://maven.apache.org/install.html)
3) Add required Maven dependecies to local Maven repository, such as [**Japsa 1.9-3c**](https://github.com/mdcao/japsa) package. To do this you should run following command from project root:
```
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar

mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar
```
4) Build uber-jar file
```
cd NanostreamDataflowMain
mvn clean package
```
after running this command successfully, there should be a file:
"NanostreamDataflowMain/target/NanostreamDataflowMain-1.0-SNAPSHOT.jar"
