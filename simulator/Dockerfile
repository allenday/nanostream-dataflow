FROM python:3-alpine

WORKDIR /simulator

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY simulator.py /simulator/simulator.py

CMD python simulator.py $SOURCE_FILE $DESTINATION_BUCKET $PUBLISHING_SPEED
