FROM google/cloud-sdk

RUN apt-get -y update
RUN apt-get -y install python-dev python-pip vim-common
RUN pip install google-cloud-pubsub
RUN apt-get -y install hdf5-tools git wget

WORKDIR /simulator
COPY ./publisher.py /simulator/publisher.py

CMD gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS && gsutil cp $SOURCE_FILE /simulator/reads.tsv && python publisher.py $PUB_SUB_TOPIC reads.tsv


# build with 'docker build -t (container_name) .'
# for example build with 'docker build -t nanostream-simulator .'
#
# run with:
# docker run \
#     -v (your_google_credentials_file_path):/gcloud_keys/
#     -e PUB_SUB_TOPIC='(pub_sub_topic_name)' \
#     -e SOURCE_FILE='(source_data_file_path)' \
#     -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/(google_credentials_file_name)' \
#     -t (container_name)
# for example run with:
# docker run -v $(pwd)/gcloud_keys:/gcloud_keys -e PUB_SUB_TOPIC='projects/upwork-nano-stream/topics/nanostream_simulator_topic' -e SOURCE_FILE='gs://nano-stream/20170320_GN_179_timestamped_60x.dilate_60x.tsv'  -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' -t nanostream-simulator

