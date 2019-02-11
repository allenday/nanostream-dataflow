#!/usr/bin/env python

"""
This module provides infinite emulation of uploading files to GCS.
"""

import sys
import time
import logging

from google.cloud import storage


def start_infinite_adding_files(dest_gcs_bucket_name, data, publishing_speed):
    """Starts infinite adding of files to specific GCS bucked at preciously set time"""
    while True:
        start_time = time.time()
        for msg_tuple in data:
            worked_time = time.time() - start_time
            time_to_add, file_to_add = msg_tuple
            time_to_add = time_to_add / publishing_speed

            # Wait until add time
            if time_to_add > worked_time:
                time.sleep(time_to_add - worked_time)
            src_bucket_name, file_name_to_add, _ = parse_file_to_bucket_and_filename(file_to_add)
            logging.info(
                'Start adding of file "{0}" at {1}'.format(get_shortened_file_name(file_name_to_add), time_to_add))
            add_file_to_bucket(dest_gcs_bucket_name, src_bucket_name, file_name_to_add)


def add_file_to_bucket(dest_gcs_bucket_name, source_bucket_name, source_object_name):
    """Adds the file to the specific GCS bucket"""
    storage_client = storage.Client()
    source_bucket = storage_client.get_bucket(source_bucket_name)
    source_blob = source_bucket.blob(source_object_name)
    destination_bucket = storage_client.get_bucket(dest_gcs_bucket_name)

    if source_blob.exists():
        new_blob = source_bucket.copy_blob(
            source_blob, destination_bucket, "simulator/" + source_object_name)

        print('File {} in bucket {} copied to file {} in bucket {}.'.format(
            get_shortened_file_name(source_blob.name), source_bucket.name, get_shortened_file_name(new_blob.name),
            destination_bucket.name))
    else:
        print('File {} is not exists'.format(get_shortened_file_name(source_blob.name)))


def get_shortened_file_name(filename):
    """Creates shortened variant of filename"""
    len_limit = 30
    msg_length = len(filename)

    if msg_length > len_limit:
        return '...{}'.format(filename[-len_limit:])
    else:
        return filename


def parse_file_to_bucket_and_filename(file_path):
    """Divides file path to bucket name and file name"""
    path_parts = file_path.split("//")
    if len(path_parts) >= 2:
        main_part = path_parts[1]
        if "/" in main_part:
            divide_index = main_part.index("/")
            bucket_name = main_part[:divide_index]
            file_name = main_part[divide_index + 1 - len(main_part):]

            # Creates file name for caching gcs file locally"
            file_name_path_parts = file_name.split("/")
            gcs_file_download_path = file_name_path_parts[-1]
            return bucket_name, file_name, gcs_file_download_path
    return "", "", ""


def download_gcs_file(bucket_name, source_file_name, destination_file_name):
    """Downloads a file from the bucket."""
    storage_client = storage.Client()
    bucket = storage_client.get_bucket(bucket_name)
    blob = bucket.blob(source_file_name)

    print('Downloading file {} to {}.'.format(
        get_shortened_file_name(source_file_name),
        destination_file_name))

    blob.download_to_filename(destination_file_name)


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    if len(sys.argv) < 2:
        print('Script must be called with 2 arguments - source GCS file name and destination GCS bucket name.')
        print('Example: "python simulator.py gs://bucket/source-file destination-bucket"')
        exit(1)

    # Reads destination GCS bucket name and source GCS filename from arguments
    source_gcs_filename = sys.argv[1]
    destination_gcs_bucket_name = sys.argv[2]

    publishing_speed = 1
    if len(sys.argv) > 3:
        publishing_speed = float(sys.argv[3])

    # Downloads source file filename for GCS bucket
    source_bucket_name, source_gcs_filename, gcs_file_download_path = parse_file_to_bucket_and_filename(
        source_gcs_filename)
    download_gcs_file(source_bucket_name, source_gcs_filename, gcs_file_download_path)

    # Reads Source file
    with open(gcs_file_download_path, "r") as source_file:
        # Parses source file and write it to list
        source_data_parsed = list()
        for line in source_file.readlines():
            line_data = line.strip().split("\t")
            add_time, file_to_add = line_data
            source_data_parsed.append((float(add_time), file_to_add))

    # Sorts list by message add_time
    source_data_parsed.sort()

    # Starts infinite adding of files
    start_infinite_adding_files(destination_gcs_bucket_name, source_data_parsed, publishing_speed)
