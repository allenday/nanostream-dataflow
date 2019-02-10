FROM python:3

WORKDIR /formatter

COPY requirements.txt ./
COPY main ./
RUN pip install --no-cache-dir -r requirements.txt

CMD python /main/formatter.py $SOURCE_FILE $DESTINATION_BUCKET $DESTINATION_FOLDER
