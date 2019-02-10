FROM python:3
RUN pip install --no-cache-dir -r requirements.txt

WORKDIR /formatter
COPY requirements.txt ./
COPY main ./

CMD python /formatter/formatter.py $SOURCE_FILE $DESTINATION_BUCKET $DESTINATION_FOLDER
