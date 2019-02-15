#!/bin/bash
set -e

# set environment variables
export NAME="bwa-resistance-genes"
export REGION="asia-northeast1"
export ZONE="${REGION}-c"
export MACHINE_TYPE="n1-standard-4"
export MIN_REPLICAS=1
export MAX_REPLICAS=3
export TARGET_CPU_UTILIZATION=0.7

export DOCKER_IMAGE='allenday/bwa-http-docker:http'
export BWA_FILES='gs://nano-stream1/CombinedDatabases/resFinder/*'

# REQUESTER_PROJECT - project billed for downloading BWA_FILES
# this line set it value to the active project ID
export REQUESTER_PROJECT=$(gcloud config get-value project)

source provision.sh

[[ $1 = '-c' ]] && cleanup || setup

gcloud compute --health-check \
create larrytest \
--request-path /cgi-bin/bwa.cgi
