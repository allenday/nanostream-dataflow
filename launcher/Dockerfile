FROM google/cloud-sdk

# install maven for compile java code
RUN apt-get install -y maven
RUN mvn --version

WORKDIR /application
COPY ./ /application
COPY ../NanostreamDataflowMain /application/NanostreamDataflowMain

CMD ["python", "-u", "/application/install.py"]