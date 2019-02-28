# Klebsiella Example
Klebsiella fastq dataset is temporary unavalilable!

### Open cloudshell in GCP project
https://cloud.google.com/shell/docs/quickstart#start_cloud_shell

### Clone repository

```
git clone https://github.com/allenday/nanostream-dataflow.git
cd nanostream-dataflow/
PROJECT_ROOT=$(pwd)
```

### Start aligner cluster

```
cd $PROJECT_ROOT/aligner/
./provision_resistance_genes.sh
```

### Build jar

```
cd $PROJECT_ROOT
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar
mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar
cd NanostreamDataflowMain
mvn clean install
```

### Create buckets and notifications

```
# bucket for supporting files
FILES_BUCKET=<Put your bucket name here>
# bucket for uploading FastQ files
UPLOAD_BUCKET=<Put your bucket name here>

# PubSub topic name
UPLOAD_EVENTS=$UPLOAD_BUCKET-events
# PubSub subscription name
UPLOAD_SUBSCRIPTION=$UPLOAD_EVENTS-subscription

gsutil notification create -t $UPLOAD_EVENTS -f json -e OBJECT_FINALIZE gs://$UPLOAD_BUCKET
gcloud pubsub subscriptions create $UPLOAD_SUBSCRIPTION --topic $UPLOAD_EVENTS
```

### Download supporting file

```
PROJECT=`gcloud config get-value project`
gsutil -u $PROJECT cp gs://nanostream-dataflow-demo-data/gene-info/resistant_genes_list.txt gs://$FILES_BUCKET/
```

### Setup variables

```
RUNNER=org.apache.beam.runners.dataflow.DataflowRunner
PROCESSING_MODE=resistance_genes
ALIGNMENT_WINDOW=20
STATS_UPDATE_FREQUENCY=30
RESISTANCE_GENES_ALIGNER_CLUSTER_IP=$(gcloud compute forwarding-rules describe bwa-resistance-genes-forward --global --format="value(IPAddress)")
SERVICES_HOST=http://$RESISTANCE_GENES_ALIGNER_CLUSTER_IP
# bwa path
BWA_ENDPOINT=/cgi-bin/bwa.cgi
# bwa database name
BWA_DATABASE=DB.fasta
# kalign path
KALIGN_ENDPOINT=/cgi-bin/kalign.cgi
FIRESTORE_COLLECTION_NAME_PREFIX=klebsiella
FIRESTORE_DOCUMENT_NAME_PREFIX=res_genes
RESISTANCE_GENES_LIST=gs://$FILES_BUCKET/resistant_genes_list.txt
```

### Run pipeline

```
java -cp target/NanostreamDataflowMain-1.0-SNAPSHOT.jar \
  com.google.allenday.nanostream.NanostreamApp \
  --runner=$RUNNER \
  --project=$PROJECT \
  --streaming=true \
  --processingMode=$PROCESSING_MODE \
  --inputDataSubscription=projects/$PROJECT/subscriptions/$UPLOAD_SUBSCRIPTION  \
  --alignmentWindow=$ALIGNMENT_WINDOW \
  --statisticUpdatingDelay=$STATS_UPDATE_FREQUENCY \
  --servicesUrl=$SERVICES_HOST \
  --bwaEndpoint=$BWA_ENDPOINT \
  --bwaDatabase=$BWA_DATABASE \
  --kAlignEndpoint=$KALIGN_ENDPOINT \
  --outputCollectionNamePrefix=$FIRESTORE_COLLECTION_NAME_PREFIX \
  --outputDocumentNamePrefix=$FIRESTORE_DOCUMENT_NAME_PREFIX \
  --resistanceGenesList=$RESISTANCE_GENES_LIST
```

### Upload FastQ file
Klebsiella fastq dataset is temporary unavalilable!
```
gsutil -u $PROJECT cp gs://nanostream-dataflow-demo-data/2_Klebsiella/GN_091_pass.fastq.gz .
gunzip GN_091_pass.fastq.gz
gsutil cp GN_091_pass.fastq gs://$UPLOAD_BUCKET/
```