from datetime import datetime
import logging
import sys
from google.cloud import storage


def get_shortened_file_name(filename):
    """Creates shortened variant of filename"""
    len_limit = 30
    msg_length = len(filename)

    if msg_length > len_limit:
        return '...{}'.format(filename[-len_limit:])
    else:
        return filename


def upload_file(data, filename, content_type, destination_bucket_name):
    """
    Uploads a file to a given Cloud Storage bucket and returns the public url
    to the new object.
    """
    bucket = storage.Client().get_bucket(destination_bucket_name)
    blob = bucket.blob(filename)

    blob.upload_from_string(
        data,
        content_type=content_type)


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
        destination_file_name), flush=True)

    blob.download_to_filename(destination_file_name)


DATE_FORMAT = '%Y-%m-%dT%H:%M:%SZ'
START_TIME_TAG = 'start_time='

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    if len(sys.argv) < 4:
        print(
            'Script must be called with 4 arguments - source_gcs_filename, destination_bucket_name, '
            'output_strand_gs_folder, output_fastq_index_data_filename')
        print('Example: "python splitter.py gs://my_bucket/src.fastq my_bucket my_bucket_folder '
              'fastq_strand_list_data.tsv"')
        exit(1)

    source_gcs_filename = sys.argv[1]
    destination_bucket_name = sys.argv[2]
    output_strand_gs_folder = sys.argv[3]
    output_fastq_index_data_filename = sys.argv[4]

    filename_parts = source_gcs_filename.split("/")
    strand_filename_prefix = filename_parts[len(filename_parts) - 1].split(".")[0]

    source_bucket_name, source_gcs_filename, gcs_file_download_path = parse_file_to_bucket_and_filename(
        source_gcs_filename)
    download_gcs_file(source_bucket_name, source_gcs_filename, gcs_file_download_path)

    number_of_strands = 0
    min_start_time = None
    with open(gcs_file_download_path, "r") as file:
        for line in file:
            if START_TIME_TAG in line:
                number_of_strands += 1
                time_str = line[line.index(START_TIME_TAG) + len(START_TIME_TAG):].replace("\n", "")
                time = datetime.strptime(time_str, DATE_FORMAT)
                if min_start_time is None or min_start_time > time:
                    min_start_time = time

        print("Number of strands: {}".format(number_of_strands), flush=True)
        print("Min start time: {}".format(min_start_time), flush=True)

    simulation_files = []
    with open(gcs_file_download_path, "r") as file:
        for read in range(number_of_strands):
            header_line = file.readline()
            sequence_line = file.readline()
            plus_line = file.readline()
            quality_line = file.readline()

            header_parts = header_line.split(' ')
            read_n = header_parts[2].split('=')[1]
            ch = header_parts[3].split('=')[1]
            start_time = header_parts[4].split('=')[1].strip()
            start_time_object = datetime.strptime(start_time, DATE_FORMAT)

            delta_seconds = (start_time_object - min_start_time).seconds
            strand_filename = strand_filename_prefix + '_ch%s_read%s_strand.fastq' % (ch, read_n)
            print('%s/%s' % (read + 1, number_of_strands), flush=True)
            print('%s\t%s' % (delta_seconds, strand_filename), flush=True)
            simulation_files.append((delta_seconds, strand_filename))

            strand_data = ''
            strand_data += header_line + "\n"
            strand_data += sequence_line + "\n"
            strand_data += plus_line + "\n"
            strand_data += quality_line
            upload_file(strand_data, output_strand_gs_folder + "/" + strand_filename, "text/plain",
                        destination_bucket_name)

        simulation_files.sort()

        fastq_strand_list_data = ''
        fastq_strand_list_data_path = "gs://" + destination_bucket_name + "/" + output_strand_gs_folder + "/"
        for item in simulation_files:
            fastq_strand_list_data += ('%s\t' + fastq_strand_list_data_path + '%s\n') % item
        print('Uploading fastq strand list data', flush=True)
        upload_file(fastq_strand_list_data, output_fastq_index_data_filename, "text/plain", destination_bucket_name)
