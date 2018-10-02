#!/usr/bin/env python

"""
This class provides infinite emulation of publishing messages via Pub/Sub.
Pub/Sub topic and source data receives from arguments
"""

import sys
import time
import logging

from google.cloud import pubsub


def start_infinite_publishing(topic_name, data):
    """Starts infinite publishing of messages by preciously set time"""
    start_time = time.time()
    for msg_tuple in data:
        time_worked = time.time() - start_time
        time_to_publish = msg_tuple[0]

        # Wait until message time
        if time_to_publish > time_worked:
            time.sleep(time_to_publish - time_worked)
        logging.info('Start publishing message "{0}" at {1}'.format(get_shortened_message(msg_tuple[1]), time_to_publish))
        publish_message(topic_name, msg_tuple[1])

    start_infinite_publishing(topic_name, data)


def publish_message(topic_name, message):
    """Publishes a message to a Pub/Sub topic with the given data."""
    pubsub_client = pubsub.PublisherClient()

    # Data must be in a bytestring format
    message_encoded = message.encode('utf-8')

    future = pubsub_client.publish(topic_name, message_encoded)
    logging.info('Message {} published.\n'.format(future.result()))


def get_shortened_message(msg):
    """Creates shortened variant of message"""
    len_limit = 30
    msg_length = len(msg)

    if msg_length > len_limit:
        return '...{}'.format(msg[-len_limit:])
    else:
        return msg


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    # Reads Pub/Sub topic and Source  filename from argument
    pubsub_topic = sys.argv[1]
    source_filename = sys.argv[2]

    # Log arguments
    logging.info('Pub/Sub topic: {}'.format(pubsub_topic))
    logging.info('Source filename: {}'.format(source_filename))

    # Reads Source file
    with open(source_filename, "r") as source_file:
        # Parses Source queue and write it to list
        source_data_parsed = list()
        for line in source_file.readlines():
            line_data = line.strip().split("\t")
            msg_time = line_data[0]
            message_text = line_data[1]
            source_data_parsed.append((float(msg_time), message_text))

    # Sorts list by message time
    source_data_parsed.sort(key=lambda item: item[0])

    # Starts infinite publishing of messages
    start_infinite_publishing(pubsub_topic, source_data_parsed)
