## Fastq file splitter

This module splits a fastq file with multiple fastq strands and creates separate fastq file for each strand and
tsv file with a list of fastq files and fastq strands timings.

Example of tsv file with a list of fastq files and fastq strands timings:
```
0	gs://my_bucket/BLOOD_SCANNING_strands/BLOOD_SCANNING_25_02_19_ch2_read8_strand.fastq
1	gs://my_bucket/BLOOD_SCANNING_strands/BLOOD_SCANNING_25_02_19_ch66_read30_strand.fastq
2	gs://my_bucket/BLOOD_SCANNING_strands/BLOOD_SCANNING_25_02_19_ch317_read61_strand.fastq
11	gs://my_bucket/BLOOD_SCANNING_strands/BLOOD_SCANNING_25_02_19_ch369_read35_strand.fastq
13	gs://my_bucket/BLOOD_SCANNING_strands/BLOOD_SCANNING_25_02_19_ch140_read126_strand.fastq
```

You can use Docker to run this module as follows.

## Docker build

Build like this, using your own image name if desired.
```     
docker build -t allenday/nanostream-dataflow-fastq-splitter .
```

## Docker run

To run locally you need a [service account key](https://cloud.google.com/iam/docs/creating-managing-service-account-keys).

Then define some variables:
```
GOOGLE_CLOUD_PROJECT=`gcloud config get-value project`
# docker image to use
IMAGE=allenday/nanostream-dataflow-fastq-splitter
# path to source file with multiple fastq strands
SOURCE_GCS_FILENAME=gs://my_bucket/BLOOD_SCANNING_25_02_19.fastq
# name of the bucket that will be used for saving results
DESTINATION_BUCKET_NAME=my_bucket
# name of the gcs bucket folder that will be used for saving generated fastq files
OUTPUT_STRAND_GCS_FOLDER=BLOOD_SCANNING_25_02_19_strands
# name of the tsv file with a list of fastq files and fastq strands timings
OUTPUT_FASTQ_STRAND_LIST_FILENAME=BLOOD_SCANNING_25_02_19_timestamped.tsv
```

Then run it:
``` 
docker run \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    -e SOURCE_GCS_FILENAME=$SOURCE_GCS_FILENAME \
    -e DESTINATION_BUCKET_NAME=DESTINATION_BUCKET_NAME \
    -e OUTPUT_STRAND_GCS_FOLDER=OUTPUT_STRAND_GCS_FOLDER \
    -e OUTPUT_FASTQ_STRAND_LIST_FILENAME=OUTPUT_FASTQ_STRAND_LIST_FILENAME \
    $IMAGE
```
