## Fasta file formatter

This module reformats fasta file from multi-line sequence format to a 2-column TSV format.
Formatted date uploads to GCS in `(file_index).fasta` format. Maximum size of dest file - 1GB.
Result files may be concatenated using `gsutil` command:
```
gsutil compose (component-obj-1, component-obj-2, component-obj-3, ...)
```

Example input file content:
```
>JSA_0 NZ_LAZK01000019.1:82188-83212
AGCTTTTCTATAAAATTTAACTTACATTTTTGATATCTAATAATTGATCTACTCAAGTTA
>JSA_1 NZ_LAKP01000081.1:988-1898
GCACCTATCCTTGGAGCGTTGAGTGACCGATTTGGACGTCGACCTGTATTAATTATTTCA
```

Example output file content:
```
>JSA_0 NZ_LAZK01000019.1:82188-83212	AGCTTTTCTATAAAATTTAACTTACATTTTTGATATCTAATAATTGATCTACTCAAGTTA
>JSA_1 NZ_LAKP01000081.1:988-1898	GCACCTATCCTTGGAGCGTTGAGTGACCGATTTGGACGTCGACCTGTATTAATTATTTCA
```

You can use Docker to run this module as follows.

#### Docker build

Build like this, using your own image name if desired.
```     
docker build -t allenday/nanostream-dataflow-fasta-formatter .
```

#### Docker run

To run locally you need a [service account key](https://cloud.google.com/iam/docs/creating-managing-service-account-keys).

```
docker run \
    -v (your_fasta_file_path):/fasta_data/ \
    -v (your_google_credentials_file_path):/gcloud_keys/
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/(google_credentials_file_name)' \
    -e BUCKET_NAME='(dest_bucket_name)' \
    -e SOURCE_FILENAME='(source_data_file_path)' \
    -e DEST_GSC_FOLDER='(destination_GCS_folder)' \
    (container_name)
```

for example :

``` 
docker run \
    -v $(pwd)/fasta_data:/fasta_data/ \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    -e BUCKET_NAME='nano-stream-test' \
    -e SOURCE_FILENAME='/data/DB.fasta' \
    -e DEST_GSC_FOLDER='fasta_output/resistant/' \
    nanostream-fasta-formatter
```
