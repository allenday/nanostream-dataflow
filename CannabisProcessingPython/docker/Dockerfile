FROM python:3

RUN apt-get update

RUN apt-get install wget -y
RUN wget https://github.com/lh3/minimap2/releases/download/v2.17/minimap2-2.17_x64-linux.tar.bz2
RUN apt-get install bzip2 -y
RUN tar -xvjf minimap2-2.17_x64-linux.tar.bz2 -C /
RUN pip install pysam