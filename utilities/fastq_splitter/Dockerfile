FROM python:3

WORKDIR /splitter
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY splitter.py /splitter/splitter.py

CMD python /splitter/splitter.py  $SOURCE_GCS_FILENAME $DESTINATION_BUCKET_NAME $OUTPUT_STRAND_GCS_FOLDER $OUTPUT_FASTQ_STRAND_LIST_FILENAME

