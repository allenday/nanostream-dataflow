FROM python:3

WORKDIR /converter

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY converter.py /converter/converter.py

CMD python converter.py $SOURCE_FILE $DEST_BUCKET_NAME $PROJECT_ID
