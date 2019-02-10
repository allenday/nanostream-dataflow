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

Build with:
```
docker build -t (container_name) .
```

for example:
```     
docker build -t nanostream-simulator .
```

#### Docker run on GCP

```
docker run \
    -e SOURCE_FILE='(source_data_file_path)' \
    -e DESTINATION_BUCKET='(bucket for simulated uploads)' \
    -e PUBLISHING_SPEED=(publishing_speed_rate) \
    (container_name)
```

for example:
``` 
docker run \
    -e SOURCE_FILE='gs://nanostream-dataflow-demo-data/simulator/20170320_GN_179_timestamped_60x.dilate_60x.tsv' \
    -e DESTINATION_BUCKET='simulator-temporary-aerohs8s' \
    -e PUBLISHING_SPEED=1 \
    nanostream-simulator
```

#### Docker run locally

To run locally you need a [service account key](https://cloud.google.com/iam/docs/creating-managing-service-account-keys), then:
``` 
docker run \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    \
    `# path to Google credentials JSON file` \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    \
    `# source of the read URIs and timing data` \
    -e SOURCE_FILE='gs://nanostream-dataflow-demo-data/simulator/20170320_GN_179_timestamped_60x.dilate_60x.tsv' \
    \
    `# bucket for simulated uploads` \
    -e DESTINATION_BUCKET='simulator-temporary-aerohs8s' \
    \
    `# accelerate publication rate of reads to the queue` \
    -e PUBLISHING_SPEED=1 \
    \
    `# use your own container name if desired` \
    allenday/nanostream-dataflow-simulator
```
