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

There is an installation script [install.py](launcher/install.py) that 
- creates required resources: 
    - A bucket for uploaded files
    - A bucket for dataflow templates
    - A bucket for reference database
    - A pubsub topic and for uploaded data
    - A pubsub topic, subscription, functions to start/stop dataflow templates
- deploys dataflow templates
- deploys [Nanostream management application](NanostreamDataflowMain/webapp/README.md)    


#### Requirements 

Before run automatic setup scripts or perform manual steps make sure you 
- created [Google Cloud Project](https://console.cloud.google.com); 
- bounded it to a payment account;
- Firestore used in native mode (use "nam5 (United States)" location).  


#### Setup using docker on Google Cloud Console

1. Open [Google Cloud Shell](https://ssh.cloud.google.com/)

2. Clone the project from Github

3. Set your Cloud Platform project in your session:  
```gcloud config set project <your project id>```

4. Build docker launcher
```
docker build -t launcher .
```

5. Run docker launcher
```
docker run -e GOOGLE_CLOUD_PROJECT=<your project id> launcher
```

6. Create a Cloud Firestore database: https://firebase.google.com/docs/firestore/quickstart#create

7. Upload Reference Databases to a place available to your project. You may use a bucket `gs://<your project id>-reference-db` created by [installation script](launcher/install.py). 

8. Open [Nanostream management application](NanostreamDataflowMain/webapp/README.md), create New Pipeline.   

9. Start upload your data to upload bucket (`gs://<your project id>-upload-bucket/<your folder data>`)
 

#### Setup by running install.py directly

You can run installation script [install.py](launcher/install.py) directly.
 
Make sure you have installed: 
- Python3 (https://www.python.org/downloads/)
- Maven (http://maven.apache.org/download.cgi)
- Google Cloud Sdk (https://cloud.google.com/sdk/install)
- Firebase Tools (https://firebase.google.com/docs/cli)

1. Init your gcloud configuration: `gcloud init`
2. Obtain a JSON file with a service account credentials (https://cloud.google.com/docs/authentication/production#obtaining_and_providing_service_account_credentials_manually).
3. Export GOOGLE_APPLICATION_CREDENTIALS environment var (https://cloud.google.com/docs/authentication/production#setting_the_environment_variable).
4. Run `python3 launcher/install.py`  
 

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



### Available Reference databases
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


### Pipeline template description
[Pipeline](NanostreamDataflowMain/pipeline/README.md)
