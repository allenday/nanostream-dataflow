import logging

def download_to_local(blob, local_dir):
    import os

    filename = blob.name.split('/')[-1]
    folder_prefix = blob.name.replace(filename, '') if len(blob.name.split('/')) > 0 else ""
    folder = local_dir + folder_prefix
    logging.info('Downloading of {} started. Wait for completing...'.format(blob.name))
    # Create this folder locally if not exists
    if not os.path.exists(folder):
        os.makedirs(folder)

    destination_uri = '{}{}'.format(folder, filename)
    blob.download_to_filename(destination_uri)
    logging.info('File {} downloaded to {}'.format(blob.name, destination_uri))
    return destination_uri, folder


def upload_file(gcs_client, uri, filename, content_type, bucket_name):
    """
    Uploads a file to a given Cloud Storage bucket and returns the public url
    to the new object.
    """
    with open(uri, 'rb') as f:
        data = f.read()
    bucket = gcs_client.get_bucket(bucket_name)
    blob = bucket.blob(filename)
    logging.info('Start uploading of {} to {}. Wait for completing...'.format(uri, 'gs://{}/{}'.format(bucket_name, filename)))
    blob.upload_from_string(
        data,
        content_type=content_type)
    logging.info('File {} uploaded to {}'.format(uri, 'gs://{}/{}'.format(bucket_name, filename)))
    return 'gs://{}/{}'.format(bucket_name, filename)

