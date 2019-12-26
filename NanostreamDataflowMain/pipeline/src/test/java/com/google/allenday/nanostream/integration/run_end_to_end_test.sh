#!/usr/bin/env bash

# IP address of the aligner cluster created by running aligner/provision_species.sh
SERVICES_HOST=http://$SPECIES_ALIGNER_CLUSTER_IP
# Google Cloud project name
PROJECT=`gcloud config get-value project`

echo "PROJECT: $PROJECT"
echo "RESULT_BUCKET: $RESULT_BUCKET"
echo "REFERENCE_NAME_LIST: $REFERENCE_NAME_LIST"
echo "ALL_REFERENCES_GCS_URI: $ALL_REFERENCES_GCS_URI"
echo "ALIGNED_OUTPUT_DIR: $ALIGNED_OUTPUT_DIR"

if [[ -z $PROJECT ]]
then
    echo "Error! PROJECT is null"
    exit 0
fi
if [[ -z $RESULT_BUCKET ]]
then
    echo "Error! RESULT_BUCKET is null"
    exit 0
fi
if [[ -z $REFERENCE_NAME_LIST ]]
then
    echo "Error! REFERENCE_NAME_LIST is null"
    exit 0
fi
if [[ -z $ALL_REFERENCES_GCS_URI ]]
then
    echo "Error! ALL_REFERENCES_GCS_URI is null"
    exit 0
fi
if [[ -z $ALIGNED_OUTPUT_DIR ]]
then
    echo "Error! ALIGNED_OUTPUT_DIR is null"
    exit 0
fi

mvn test -Dtest=com.google.allenday.nanostream.integration.EndToEndPipelineTest \
    -DprojectId=$PROJECT \
    -DresultBucket=$RESULT_BUCKET \
    -DreferenceNamesList=$REFERENCE_NAME_LIST \
    -DallReferencesDirGcsUri=$ALL_REFERENCES_GCS_URI \
    -DalignedOutputDir=$ALIGNED_OUTPUT_DIR
