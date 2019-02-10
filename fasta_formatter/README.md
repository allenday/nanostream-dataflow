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

## Docker build

Build like this, using your own image name if desired.
```     
docker build -t allenday/nanostream-dataflow-fasta-formatter .
```

## Docker run

To run locally you need a [service account key](https://cloud.google.com/iam/docs/creating-managing-service-account-keys).

Then define some variables:
```
GOOGLE_CLOUD_PROJECT=`gcloud config get-value project`
# docker image to use
IMAGE=allenday/nanostream-dataflow-fasta-formatter
# no need to change this.
SOURCE_MOUNT=/data
# directory where input FASTA files can be found.
# it needs to be in or below `pwd`.
SOURCE_FOLDER=eg
# this is the path to the TSV as described above.
SOURCE_FILE=in.fq
# to which bucket will records be written?
DESTINATION_BUCKET='nanostream-dataflow-qc-fasta-formatter'
# to which folder will records be written?
DESTINATION_FOLDER=fasta_output/resistant/
```

Then run it:
``` 
docker run \
    -v $(pwd)/$SOURCE_FOLDER:$SOURCE_MOUNT \
    -v $(pwd)/gcloud_keys:/gcloud_keys/ \
    -e GOOGLE_CLOUD_PROJECT=$GOOGLE_CLOUD_PROJECT \
    -e GOOGLE_APPLICATION_CREDENTIALS='/gcloud_keys/gcloud_credentials.json' \
    -e SOURCE_FILE=$SOURCE_MOUNT/$SOURCE_FILE \
    -e DESTINATION_BUCKET=$DESTINATION_BUCKET \
    -e DESTINATION_FOLDER=$DESTINATION_FOLDER \
    $IMAGE
```
