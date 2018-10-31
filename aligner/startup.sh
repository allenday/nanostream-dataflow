#!/bin/bash
DOCKER_IMAGE=`wget --header 'Metadata-Flavor: Google' -O - -q 'http://metadata.google.internal/computeMetadata/v1/instance/attributes/DOCKER_IMAGE'`
BWA_FILES=`wget --header 'Metadata-Flavor: Google' -O - -q 'http://metadata.google.internal/computeMetadata/v1/instance/attributes/BWA_FILES'`

docker run -p 80:80 -e BWA_FILES=$BWA_FILES $DOCKER_IMAGE
