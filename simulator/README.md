## Source data publishing simulator
This module provides emulation of infinite publishing messages via Pub/Sub from source data file.

To run it you should use Docker

#### Docker build


Build with:

```
docker build -t (container_name) .
```

for example:

```     
docker build -t nanostream-simulator .
```

#### Docker run

run with:

```
docker run \
    -v (your_google_credentials_file_path):/gcloud_keys/ ```
    -e PUB_SUB_TOPIC='(pub_sub_topic_name)' \
    -e SOURCE_FILE='(source_data_file_path)' \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/(google_credentials_file_name)' \
    -t (container_name)
```

     
for example :
``` 
docker run \
    -v $(pwd)/gcloud_keys:/gcloud_keys \
    -e PUB_SUB_TOPIC='projects/upwork-nano-stream/topics/nanostream_simulator_topic' \
    -e SOURCE_FILE='gs://nano-stream/20170320_GN_179_timestamped_60x.dilate_60x.tsv'  \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    -t nanostream-simulator
```

