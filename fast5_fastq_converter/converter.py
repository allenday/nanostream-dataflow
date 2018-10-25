#!/usr/bin/env python

import sys
import logging

from google.cloud import storage
from google.cloud import pubsub_v1
import datetime

DEST_FOLDER_NAME = "fast5_fastq_conversion_output"

def get_shortened_file_name(filename):
    """Creates shortened variant of filename"""
    len_limit = 30
    msg_length = len(filename)

    if msg_length > len_limit:
        return '...{}'.format(filename[-len_limit:])
    else:
        return filename

def download_gsc_file(bucket_name, source_file_name, destination_file_name):
    """Downloads a file from the bucket."""
    storage_client = storage.Client()
    bucket = storage_client.get_bucket(bucket_name)
    blob = bucket.blob(source_file_name)

    blob.download_to_filename(destination_file_name)

    print('File {} downloaded to {}.'.format(
        get_shortened_file_name(source_file_name),
        destination_file_name))


def parse_file_to_bucket_and_filename(file_path):
    """Divides file path to bucket name and file name"""
    path_parts = file_path.split("//")
    if len(path_parts) >= 2:
        main_part = path_parts[1]
        if "/" in main_part:
            divide_index = main_part.index("/")
            bucket_name = main_part[:divide_index]
            file_name = main_part[divide_index + 1 - len(main_part):]
            return bucket_name, file_name
    return "", ""

def pull_sync(src_file_path):

    subscription_name = "fastq-subscription"

    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(
        PROJECT_ID, subscription_name)

    NUM_MESSAGES = 10

    # The subscriber pulls a specific number of messages.

    print("Start pulling the result of '{}'".format(get_shortened_file_name(src_file_path)))

    response = subscriber.pull(subscription_path, max_messages=NUM_MESSAGES, timeout=60, retry=None)
    if len(response.received_messages) > 0:
        message = response.received_messages[0]

        ack_id = message.ack_id
        print("Result successfully pulled at {}: messages {}, message with ack_id={}\n "
              .format(datetime.datetime.now(), len(response.received_messages), ack_id), flush=True)
        subscription_path = subscriber.subscription_path(PROJECT_ID, subscription_name)

        subscriber.acknowledge(subscription_path, [msg.ack_id for msg in response.received_messages])

        _, src_file_name = parse_file_to_bucket_and_filename(src_file_path)
        dest_file_name = DEST_FOLDER_NAME+"/"+src_file_name.replace("fast5", "fastq")

        upload_file(message.message.data, dest_file_name, "text/plain", "nano-stream-test")
    else:
        print("There is no messages in this subscription", flush=True)

def upload_file(data, filename, content_type, bucket_name):
    """
    Uploads a file to a given Cloud Storage bucket and returns the public url
    to the new object.
    """
    bucket = storage.Client().get_bucket(bucket_name)
    blob = bucket.blob(filename)

    blob.upload_from_string(
        data,
        content_type=content_type)

def proceed_fast5_fastq_converting(src_file_path):
    topic_name = "simulator-20170320_GN_179-rate_1x"

    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(PROJECT_ID, topic_name)

    def callback(message_future):
        # When timeout is unspecified, the exception method waits indefinitely.

        if message_future.exception(timeout=30):
            print('Publishing message on {} threw an Exception {}.'.format(
                topic_name, message_future.exception()))
        else:
            print("Message with id={} successfully published!".format(str(datetime.datetime.now())), flush=True)

    data = src_file_path
    # Data must be a bytestring
    data = data.encode('utf-8')
    # When you publish a message, the client returns a Future.
    print("Publishing message '{}' at {}".format(get_shortened_file_name(src_file_path),str(datetime.datetime.now())), flush=True)
    message_future = publisher.publish(topic_path, data=data)
    message_future.add_done_callback(callback)
    pull_sync(src_file_path)

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    if len(sys.argv) < 3:
        print('Script must be called with 3 arguments - source GCS file name, destination bucket name and project id.')
        print('Example: "python converter.py nano-stream-test upwork-nano-stream"')
        exit(1)

    # Reads destination GCS bucket name and source GSC filename from arguments
    source_gsc_filename = sys.argv[1]
    dest_bucket_name = sys.argv[2]
    PROJECT_ID = sys.argv[3]

    # Downloads source file filename for GSC bucket
    source_bucket_name, source_gsc_filename = parse_file_to_bucket_and_filename(source_gsc_filename)
    source_filename = source_gsc_filename
    download_gsc_file(source_bucket_name, source_gsc_filename, source_filename)

    # Reads Source file
    with open(source_filename, "r") as source_file:
        # Parses source file and write it to list
        source_data_parsed = list()
        for line in source_file.readlines():
            line_data = line.strip().split("\t")
            add_time, file_to_add = line_data
            source_data_parsed.append((float(add_time), file_to_add))

    # Sorts list by message add_time
    source_data_parsed.sort()

    for index in range(0, len(source_data_parsed)):
        print("Start processing index={}".format(str(index)), flush=True)
        _, src_file_path = source_data_parsed[index]
        proceed_fast5_fastq_converting(src_file_path)
    #proceed_fast5_fastq_converting("gs://nano-stream-test/20170320_GN_179_timestamped_60x/0/IMB14_011406_LT_20170320_FNFAB45386_MN17027_sequencing_run_GN_179_20032017_82069_ch109_read1556_strand.fast5")
