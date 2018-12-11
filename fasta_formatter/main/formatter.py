#!/usr/bin/env python

import sys
import logging

from google.cloud import storage

NAME_CONTENT_SEPARATOR = "\t"
UPLOAD_FILE_SIZE_LIMIT = 1000000000
destination_gcs_folder = ""
bucket_name = ""
sequence_item_data = ""
output_file_data = ""
file_index = 0
file_list = []


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


def is_line_not_empty(line):
    """
    Checks whether line is not empty
    """
    return len(line) > 0


def is_end_of_file(empty_line_count):
    """
    Checks whether reading of file reaches bottom of file
    """
    return empty_line_count > 5


def is_start_of_new_sequence_item(line):
    """
    Checks whether line is the first line of sequence item data
    """
    return line[0] == '>'


def work_with_line(work_line):
    """
    Define line parsing strategy and proceed parsing
    """
    global sequence_item_data

    # Clear from redundant tags
    work_line = work_line.replace("\n", "").replace("\t", "")
    if is_start_of_new_sequence_item(line):
        accumulate_data_and_upload_to_gsc_if_needed(True)
        sequence_item_data = work_line + NAME_CONTENT_SEPARATOR
    else:
        sequence_item_data += work_line


def accumulate_data_and_upload_to_gsc_if_needed(with_limit):
    """
    Concatenate gathered sequence data to file data and upload it to GSC if needed
    """
    global output_file_data, destination_gcs_folder, file_index, bucket_name, sequence_item_data

    if len(sequence_item_data) == 0:
        return

    print("Adding {} symbols".format(len(sequence_item_data)), flush=True)
    output_file_data += sequence_item_data + "\n"
    if not with_limit or len(output_file_data) > UPLOAD_FILE_SIZE_LIMIT:
        print("Uploading {} symbols".format(len(output_file_data)), flush=True)

        file_name = destination_gcs_folder + str(file_index) + ".fasta"
        file_list.append("gs://" + bucket_name + "/" + file_name)
        upload_file(output_file_data, file_name, "text/plain", bucket_name)
        file_index = + 1
        output_file_data = ""


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    if len(sys.argv) < 3:
        print(
            'Script must be called with 3 arguments - bucket name, source source file name and destination GSC folder.')
        print('Example: "python nano-stream-test formatter.py /data/DB.fasta fasta_output/resistant/"')
        exit(1)

    # Reads destination bucket name, source source file name and destination GSC folder from arguments
    bucket_name = sys.argv[1]
    source_filename = sys.argv[2]
    destination_gcs_folder = sys.argv[3]

    # Reads Source file
    with open(source_filename, "r") as source_file:
        source_data_parsed = list()

        empty_line_count = 0
        while True:
            line = source_file.readline()
            if is_line_not_empty(line):
                empty_line_count = 0
                work_with_line(line)
            else:
                empty_line_count += 1
            if is_end_of_file(empty_line_count):
                accumulate_data_and_upload_to_gsc_if_needed(False)
                break
        print("Output file names: {}".format(file_list), flush=True)
