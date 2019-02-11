FROM python:3

WORKDIR /formatter
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY main ./

CMD python /formatter/formatter.py $SOURCE_MOUNT/$SOURCE_FILE $DESTINATION_BUCKET $DESTINATION_FOLDER
