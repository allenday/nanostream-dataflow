"""
Copyright Nanostream
"""

import apache_beam as beam
from apache_beam.options.pipeline_options import PipelineOptions
from apache_beam.options.pipeline_options import SetupOptions
from apache_beam.options.pipeline_options import StandardOptions
from apache_beam.options.pipeline_options import GoogleCloudOptions
from apache_beam.options.pipeline_options import WorkerOptions
import logging
from utils.cannabis_source import CannabisSample, CannabisSampleMetaData
from utils import gcs_utils

RESULT_SORTED_DIR_NAME = 'result_sorted_bam_python'
RESULT_SORTED_MERGED_DIR_NAME = 'result_sorted_merged_bam_python'


class UserOptions(PipelineOptions):

    @classmethod
    def _add_argparse_args(cls, parser):
        parser.add_value_provider_argument(
            '--urls_list',
            type=str)
        parser.add_argument(
            '--parallel_count',
            default=10,
            type=int)


class CannabisSampleTupleFromLine(beam.DoFn):
    # REFERENCES = ["AGQN03",
    #               "LKUA01",
    #               "LKUB01",
    #               "MNPR01",
    #               "MXBD01",
    #               "QKVJ02",
    #               "QVPT02",
    #               "UZAU01"]
    REFERENCES = ["AGQN03"]

    def process(self, data):
        # logging.info(data)

        cannabis_example_tuple = []
        cannabis_example_meta_data = CannabisSampleMetaData(data)
        cannabis_example_tuple.append(CannabisSample(cannabis_example_meta_data, 1))
        if cannabis_example_meta_data.is_paired():
            cannabis_example_tuple.append(CannabisSample(cannabis_example_meta_data, 2))

        output = []
        for ref in self.REFERENCES:
            output.append(('reference/{}/{}.fa'.format(ref, ref), cannabis_example_tuple))
        return output


def get_file_name_from_uri(uri):
    return uri.split("/")[-1]


def remove_local_dir(local_dir):
    import shutil, os
    if os.path.exists(local_dir):
        shutil.rmtree(local_dir)


class AlignSort(beam.DoFn):

    def process(self, element):
        import time, subprocess, os
        from datetime import datetime
        import pysam
        reference_path, data = element

        blobs = [item.generate_file_name() for item in data]

        files_dir = str(time.time()).replace(".", "_") + data[0].cannabis_example_meta_data.run + "/"
        if not os.path.exists(files_dir):
            os.makedirs(files_dir)
        file_prefix = files_dir + data[0].cannabis_example_meta_data.run

        from google.cloud import storage
        client = storage.Client()
        bucket = client.bucket('cannabis-3k')
        files = []
        for blob_name in blobs:
            destination_uri, folder = gcs_utils.download_to_local(bucket.blob(blob_name),
                                                                  files_dir)
            files.append(destination_uri)

        aligned_sam_path = file_prefix + '.sam'
        command_pattern = "./minimap2-2.17_x64-linux/minimap2 -ax sr {REFERENCE_PATH} {SRC_FILES} -R '@RG\tID:RSP11055\tPL:ILLUMINA\tPU:NONE\tSM:RSP11055' > {ALIGNED_SAM_PATH}"

        command = command_pattern.format(REFERENCE_PATH=reference_path, SRC_FILES=" ".join(files),
                                         ALIGNED_SAM_PATH=aligned_sam_path)

        start = datetime.now()
        logging.info("Alignment start: {}".format(command))
        subprocess.call(command, shell=True)
        delta = datetime.now() - start
        logging.info("Alignment finish in {}: {}".format(delta, command))

        aligned_bam_path = file_prefix + '.bam'
        aligned_sorted_bam_path = file_prefix + '.sorted.bam'

        start = datetime.now()
        logging.info("Convert to BAM start: {}".format(aligned_sam_path))
        bam_content = pysam.view('-bh', aligned_sam_path)

        with open(aligned_bam_path, 'wb') as file:
            file.write(bam_content)
        delta = datetime.now() - start
        logging.info("Convert to BAM finish in {}: {}".format(delta, aligned_sam_path))

        start = datetime.now()
        logging.info("Sort start: {}".format(aligned_bam_path))
        pysam.sort('-o', aligned_sorted_bam_path, aligned_bam_path)
        delta = datetime.now() - start
        logging.info("Sort finish in {}: {}".format(aligned_bam_path, delta))

        gcs_uri = gcs_utils.upload_file(client, aligned_sorted_bam_path,
                                        RESULT_SORTED_DIR_NAME + '/' + get_file_name_from_uri(aligned_sorted_bam_path),
                                        "text/plain", 'cannabis-3k-results')
        remove_local_dir(files_dir)
        return [((data[0].cannabis_example_meta_data, reference_path), gcs_uri)]


class MergeSort(beam.DoFn):

    def copy_blob(self, bucket_name, blob_name, new_bucket_name, new_blob_name):
        """Copies a blob from one bucket to another with a new name."""
        storage_client = storage.Client()
        source_bucket = storage_client.get_bucket(bucket_name)
        source_blob = source_bucket.blob(blob_name)
        destination_bucket = storage_client.get_bucket(new_bucket_name)

        new_blob = source_bucket.copy_blob(
            source_blob, destination_bucket, new_blob_name)

        logging.info('Blob {} in bucket {} copied to blob {} in bucket {}.'.format(
            source_blob.name, source_bucket.name, new_blob.name,
            destination_bucket.name))

    def parse_uri_to_bucket_dir_and_filename(self, file_path):
        """Divides file path to bucket name, gcs dir and file name"""
        path_parts = file_path.split("//")
        if len(path_parts) >= 2:
            main_part = path_parts[1]
            if "/" in main_part:
                divide_index = main_part.index("/")
                bucket_name = main_part[:divide_index]
                file_uri = main_part[divide_index + 1 - len(main_part):]

                file_uri_parts = file_uri.split("/")
                file_name = file_uri_parts[-1]
                file_dir = file_uri[:-len(file_name)]

                return bucket_name, file_dir, file_name
        return None, None, None

    def process(self, element):
        sra_sample, meta_data_and_gcs_uri = element
        meta_data, gcs_uris = meta_data_and_gcs_uri

        from google.cloud import storage
        client = storage.Client()
        bucket = client.bucket('cannabis-3k')

        if len(gcs_uris) == 1:
            bucket_name, file_dir, file_name = self.parse_uri_to_bucket_dir_and_filename(gcs_uris[0])
            merged_sorted_bam_path = file_name.replace(RESULT_SORTED_DIR_NAME, RESULT_SORTED_MERGED_DIR_NAME) \
                .replace('.sorted.bam', '.merged,sorted.bam')
            self.copy_blob(bucket_name, file_name, bucket_name, merged_sorted_bam_path)
            return [(meta_data, merged_sorted_bam_path)]
        else:
            blobs = [self.parse_uri_to_bucket_dir_and_filename(uri)[2] for uri in gcs_uris]
            files_dir = str(time.time()).replace(".", "_") + sra_sample + "/"
            if not os.path.exists(files_dir):
                os.makedirs(files_dir)
            file_prefix = files_dir + sra_sample

            files = []
            for blob_name in blobs:
                destination_uri, folder = gcs_utils.download_to_local(bucket.blob(blob_name), files_dir)
                files.append(destination_uri)
            merge_bam_path = file_prefix + '.merged.bam'
            pysam.merge(merge_bam_path, files)
            merged_sorted_bam_path = file_prefix + '.merged.sorted.bam'
            pysam.sort('-o', merged_sorted_bam_path, merge_bam_path)

            gcs_uri = gcs_utils.upload_file(client, merged_sorted_bam_path,
                                            RESULT_SORTED_MERGED_DIR_NAME + '/' + get_file_name_from_uri(
                                                merged_sorted_bam_path),
                                            "text/plain", 'cannabis-3k-results')
            remove_local_dir(files_dir)
            return [(meta_data, gcs_uri)]


def cannabis_tuple_exists(element):
    _, data = element
    logging.info([item.generate_file_name() for item in data])
    from google.cloud import storage
    client = storage.Client()
    bucket = client.bucket('cannabis-3k')
    existed_blobs = [item.generate_file_name() for item in data if bucket.blob(item.generate_file_name()).exists()]
    return len(existed_blobs) == len(data)


def run():
    pipeline_options = PipelineOptions()
    pipeline_options.view_as(StandardOptions).runner = 'DataflowRunner'
    # pipeline_options.view_as(StandardOptions).runner = 'DirectRunner'
    pipeline_options.view_as(SetupOptions).save_main_session = True
    pipeline_options.view_as(SetupOptions).setup_file = "./setup.py"
    pipeline_options.view_as(GoogleCloudOptions).staging_location = "gs://cannabis-3k/dataflow/staging/cannabis_staging"
    pipeline_options.view_as(GoogleCloudOptions).temp_location = "gs://cannabis-3k/dataflow/staging/cannabis_temp"
    pipeline_options.view_as(GoogleCloudOptions).project = "cannabis-3k"
    pipeline_options.view_as(WorkerOptions).machine_type = "n1-highmem-8"
    pipeline_options.view_as(GoogleCloudOptions).region = "us-east1"

    import datetime
    with beam.Pipeline(options=pipeline_options) as p:
        user_options = pipeline_options.view_as(UserOptions)
        (p | beam.io.ReadFromText("gs://cannabis-3k/Cannabis Genomics - 201703 - SRA.csv", skip_header_lines=1) |
         beam.Filter(lambda line: 'SRS1760342' in line) |
         beam.ParDo(CannabisSampleTupleFromLine()) |
         beam.Filter(cannabis_tuple_exists) |
         beam.ParDo(AlignSort()) |
         beam.Map(lambda element: ((element[0][0].sra_sample, element[1]), element)) |
         beam.GroupByKey() |
         beam.io.WriteToText('gs://cannabis-3k-results/cannabis_dataflow/result_{}'.format(
             datetime.datetime.now().strftime('%Y-%m-%d--%H-%M-%S'))))


if __name__ == '__main__':
    logging.getLogger().setLevel(logging.INFO)
    run()
