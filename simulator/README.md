## Sequencing Data Simulator

This module infinitely uploads files to GCS according to timings defined in a source file. The source file must be tab-separated values with two columns and no header.

Columns are:
- `time` - float number or seconds when to upload file, relative to the simulation round start
- `GCS path` - fully qualified URI of Google Cloud Storage object of the file that should be uploaded

Here is some example source file content. It contains two sequences to be uploaded at 0 seconds and 0.483051 seconds offsets from program start time.
```
0	gs://bucket-name/0/read50_strand.fast5
0.483051	gs://bucket-name/0/read55_strand.fast5
```
A complete file is located [here](https://storage.cloud.google.com/nanostream-dataflow-demo-data/simulator/20170320_GN_179_timestamped_60x.dilate_60x.tsv)

You can use Docker to run simulator as follows.

#### Docker build

Build like this. You can use a different image name instead of `nanostream-simulator`:
```     
docker build -t nanostream-simulator .
```

### Docker run

First define some variables:
```
# docker image to use
IMAGE=allenday/nanostream-dataflow-simulator
# this is the path to the TSV as described above
SOURCE_FILE=gs://nanostream-dataflow-demo-data/simulator/20170320_GN_179_timestamped_60x.dilate_60x.tsv
# to which bucket will simulated reads be uploaded?
DESTINATION_BUCKET='nanostream-dataflow-qc-simulator'
# [optional] acceleration factor for timestamps
PUBLISHING_SPEED=1
GOOGLE_CLOUD_PROJECT=`gcloud config get-value project`
```

#### on GCP

Then run it like this on GCP:
``` 
docker run \
    -e GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT \
    -e SOURCE_FILE=$SOURCE_FILE \
    -e DESTINATION_BUCKET=$DESTINATION_BUCKET \
    -e PUBLISHING_SPEED=$PUBLISHING_SPEED \
    $IMAGE
```

#### Docker run locally

To run locally you need another variable. Get a [service account key](https://cloud.google.com/iam/docs/creating-managing-service-account-keys), then:

```
GOOGLE_APPLICATION_CREDENTIALS=/gcloud_keys/gcloud_credentials.json
```

Then run it like this locally:
``` 
docker run \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    -e GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT \
    -e GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS \
    -e SOURCE_FILE=$SOURCE_FILE \
    -e DESTINATION_BUCKET=$DESTINATION_BUCKET \
    -e PUBLISHING_SPEED=$PUBLISHING_SPEED \
    $IMAGE
```
