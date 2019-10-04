#!/usr/bin/env bash

# IP address of the aligner cluster created by running aligner/provision_species.sh
SERVICES_HOST=http://$SPECIES_ALIGNER_CLUSTER_IP
# Google Cloud project name
PROJECT=`gcloud config get-value project`

echo "PROJECT: $PROJECT"
echo "SPECIES_ALIGNER_CLUSTER_IP: $SPECIES_ALIGNER_CLUSTER_IP"
echo "BWA_ENDPOINT: $BWA_ENDPOINT"
echo "BWA_DATABASE: $BWA_DATABASE"
echo "KALIGN_ENDPOINT: $KALIGN_ENDPOINT"

if [[ -z $PROJECT ]]
then
    echo "Error! PROJECT is null"
    exit 0
fi
if [[ -z $SPECIES_ALIGNER_CLUSTER_IP ]]
then
    echo "Error! SPECIES_ALIGNER_CLUSTER_IP is null"
    exit 0
fi
if [[ -z $BWA_ENDPOINT ]]
then
    echo "Error! BWA_ENDPOINT is null"
    exit 0
fi
if [[ -z $BWA_DATABASE ]]
then
    echo "Error! BWA_DATABASE is null"
    exit 0
fi
if [[ -z $KALIGN_ENDPOINT ]]
then
    echo "Error! KALIGN_ENDPOINT is null"
    exit 0
fi

mvn test -Dtest=com.google.allenday.nanostream.integration.EndToEndPipelineTest \
    -DprojectId=$PROJECT \
    -DservicesUrl=$SPECIES_ALIGNER_CLUSTER_IP \
    -DbwaEndpoint=$BWA_ENDPOINT \
    -DbwaDB=$BWA_DATABASE \
    -DkAlignEndpoint=$KALIGN_ENDPOINT