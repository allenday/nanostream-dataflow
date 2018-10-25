## Sequencing Data Simulator

This module infinitely uploads files to GCS according to timings defined in source file.
Source file must be a tsv file with two columns and without header.
Columns:
- "time" - float number or seconds when to upload file, relative to the simulation round start
- "GCS filename" - fully qualified GCS(Google Cloud Storage) object URI of the file that should be uploaded

Example source file content:

```
0	gs://bucket-name/0/read50_strand.fast5
0.483051	gs://bucket-name/0/read55_strand.fast5
```


You can use Docker to run simulator

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
    (container_name)
```

for example :

``` 
docker run \
    -e SOURCE_FILE='gs://nano-stream-test/20170320_GN_179_timestamped_60x.dilate_60x.tsv' \
    -e DESTINATION_BUCKET='simulator-temporary-aerohs8s' \
    nanostream-simulator

```

#### Docker run locally

To run locally you need google service account key, see: https://cloud.google.com/iam/docs/creating-managing-service-account-keys

```
docker run \
    -v (your_google_credentials_file_path):/gcloud_keys/
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/(google_credentials_file_name)' \
    -e SOURCE_FILE='(source_data_file_path)' \
    -e DESTINATION_BUCKET='(bucket for simulated uploads)' \
    (container_name)
```

for example :

``` 
docker run \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    -e SOURCE_FILE='gs://nano-stream-test/20170320_GN_179_timestamped_60x.dilate_60x.tsv' \
    -e DESTINATION_BUCKET='simulator-temporary-aerohs8s' \
    nanostream-simulator

```

