#!/bin/bash
set -e

# set environment variables
export NAME="bwa-species"
<<<<<<< HEAD:aligner/provision_species_bwa.sh
export REGION="asia-northeast1"
export ZONE="${REGION}-c"
export MACHINE_TYPE="n1-highmem-4"
=======
export REGION="us-central1"
export ZONE="${REGION}-b"
export MACHINE_TYPE="n1-highmem-8"
>>>>>>> cbfe24c5a164374bbb2014272ef10cc60acf6b75:aligner/provision_species.sh
export MIN_REPLICAS=1
export MAX_REPLICAS=3
export TARGET_CPU_UTILIZATION=0.5

export DOCKER_IMAGE='allenday/bwa-http-docker:http'
export BWA_FILES='gs://nano-stream1/Databases/CombinedDatabases/*'

# REQUESTER_PROJECT - project billed for downloading BWA_FILES
# this line set it value to the active project ID
export REQUESTER_PROJECT=$(gcloud config get-value project)

# provisions internal load balancer, you can provision global one replacing next line with 'source provision_global.sh'
source provision_internal.sh

<<<<<<< HEAD:aligner/provision_species_bwa.sh
[[ $1 = '-c' ]] && cleanup || setup
=======
# Run setup
[[ $1 != '-c' ]] && setup
# Run cleanup if there is '-c' argument
[[ $1 = '-c' ]] && cleanup
>>>>>>> cbfe24c5a164374bbb2014272ef10cc60acf6b75:aligner/provision_species.sh
