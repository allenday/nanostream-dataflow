#!/usr/bin/env bash
# fail on error
set -e

echo "UPLOAD_SUBSCRIPTION: $UPLOAD_SUBSCRIPTION"
echo "FIRESTORE_COLLECTION_NAME_PREFIX: $FIRESTORE_COLLECTION_NAME_PREFIX"
echo "FILES_BUCKET: $FILES_BUCKET"

if [[ -z $UPLOAD_SUBSCRIPTION ]]
then
    echo "Error! UPLOAD_SUBSCRIPTION is null"
    exit 0
fi
if [[ -z $FIRESTORE_COLLECTION_NAME_PREFIX ]]
then
    echo "Error! FIRESTORE_COLLECTION_NAME_PREFIX is null"
    exit 0
fi
if [[ -z $FILES_BUCKET ]]
then
    echo "Error! FILES_BUCKET is null"
    exit 0
fi

# Google Cloud project name
PROJECT=`gcloud config get-value project`
# Apache Beam Runner (set org.apache.beam.runners.dataflow.DataflowRunner for running in a Google Cloud Dataflow or org.apache.beam.runners.direct.DirectRunner for running locally on your computer)
RUNNER=org.apache.beam.runners.dataflow.DataflowRunner

# specify mode of data processing (species, resistance_genes)
PROCESSING_MODE=resistance_genes

# PubSub subscription defined above
UPLOAD_SUBSCRIPTION=$UPLOAD_SUBSCRIPTION
echo $UPLOAD_SUBSCRIPTION

# size of the window (in wallclock seconds) in which FastQ records will be collected for alignment
ALIGNMENT_WINDOW=20
# how frequently (in wallclock seconds) are statistics updated for dashboard visualizaiton?
STATS_UPDATE_FREQUENCY=30

# IP address of the aligner cluster created by running aligner/provision_resistance_genes.sh
#RESISTANCE_GENES_ALIGNER_CLUSTER_IP=$(gcloud compute forwarding-rules describe bwa-resistance-genes --global --format="value(IPAddress)")
RESISTANCE_GENES_ALIGNER_CLUSTER_IP=130.211.33.64
# base URL for http services (bwa and kalign)
# value for species, for resistance_genes use 'SERVICES_HOST=http://$RESISTANCE_GENES_ALIGNER_CLUSTER_IP'
SERVICES_HOST=http://$RESISTANCE_GENES_ALIGNER_CLUSTER_IP
# bwa path
BWA_ENDPOINT=/cgi-bin/bwa.cgi
# bwa database name
BWA_DATABASE=DB.fasta
# kalign path
KALIGN_ENDPOINT=/cgi-bin/kalign.cgi

# Collections name prefix of the Firestore database that will be used for writing results
FIRESTORE_COLLECTION_NAME_PREFIX=$FIRESTORE_COLLECTION_NAME_PREFIX

# Path to resistant genes sequence fasta list formatted with fasta formatter
RESISTANCE_GENES_FASTA=gs://$FILES_BUCKET/gene_info/DB_resistant_formatted.fasta

# Path to text file with resistant genes references and groups
RESISTANCE_GENES_LIST=gs://$FILES_BUCKET/gene_info/resistant_genes_list.txt

java -cp target/NanostreamDataflowMain-1.0-SNAPSHOT.jar \
  com.google.allenday.nanostream.NanostreamApp \
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
  --resistanceGenesList=$RESISTANCE_GENES_LIST