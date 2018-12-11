FROM python:3

WORKDIR /formatter

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

CMD python /main/formatter.py $BUCKET_NAME $SOURCE_FILENAME $DEST_GSC_FOLDER
