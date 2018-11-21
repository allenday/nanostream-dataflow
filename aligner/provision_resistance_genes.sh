#!/bin/bash
set -e

# set environment variables
export NAME="bwa-resistance-genes"
export REGION="europe-west3"
export ZONE="${REGION}-b"
export MACHINE_TYPE="n1-standard-1"
export MIN_REPLICAS=1
export MAX_REPLICAS=3
export TARGET_CPU_UTILIZATION=0.5

export DOCKER_IMAGE='allenday/bwa-http-docker:http'
export BWA_FILES='gs://nano-stream-test/ResistanceGenes/resFinder/*'

source provision.sh

[[ $1 = '-c' ]] && cleanup || setup
