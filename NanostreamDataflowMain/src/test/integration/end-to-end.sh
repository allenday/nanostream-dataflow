#!/usr/bin/env bash
# fail on error
set -e

# default values for project and GCP temp location
GCP_PROJECT="${GCP_PROJECT:-upwork-nano-stream}"
OUTPUT_DIR="${OUTPUT_DIR:-gs://nano-stream-test/output_species/}"
BWA_ENDPOINT="${BWA_ENDPOINT:-/cgi-bin/bwa.cgi}"
KALIGN_ENDPOINT="${KALIGN_ENDPOINT:-/cgi-bin/kalign.cgi}"

# resistant genes config
SUBSCRIPTION_NAME="${SUBSCRIPTION_NAME:-simulator_fastq_subscription}"
BASE_URL="${BASE_URL:http://130.211.33.64}"
BWA_DB="${BWA_DB:-DB.fast}"

# species config
#SUBSCRIPTION_NAME="${SUBSCRIPTION_NAME:-fastq_subscription_species}"
#BASE_URL="${BASE_URL:http://35.241.15.140}"
#BWA_DB="${BWA_DB:-genomeDB.fast}"

## run test
# start dataflow application
 mvn clean install exec:java \
-Dexec.mainClass=com.theappsolutions.nanostream.NanostreamApp \
-Dexec.cleanupDaemonThreads=false \
-Dexec.args=" \
--runner=org.apache.beam.runners.dataflow.DataflowRunner \
 --streaming=true \
 --project=${GCP_PROJECT} \
 --subscription=projects/${GCP_PROJECT}/subscriptions/${SUBSCRIPTION_NAME} \
 --outputDirectory=${OUTPUT_DIR} \
 --outputFilenameSuffix=.txt \
 --baseUrl=${BASE_URL} \
 --bwaEndpoint=${BWA_ENDPOINT}\
 --bwaDatabase=${BWA_DB}\
 --kAlignEndpoint=${KALIGN_ENDPOINT}"