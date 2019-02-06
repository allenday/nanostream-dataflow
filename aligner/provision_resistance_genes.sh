#!/bin/bash
set -e

# set environment variables
export NAME="bwa-resistance-genes"
export REGION="asia-northeast1-c"
export ZONE="${REGION}-b"
export MACHINE_TYPE="n1-standard-1"
export MIN_REPLICAS=1
export MAX_REPLICAS=3
export TARGET_CPU_UTILIZATION=0.5

export DOCKER_IMAGE='dockersubtest/nano-larry:http'
export BWA_FILES='gs://nano-stream1/NewDatabases/resFinder/*'

source provision.sh

[[ $1 = '-c' ]] && cleanup || setup
