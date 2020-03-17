FROM google/cloud-sdk

# install maven for compile java code
RUN apt-get install -y maven wget
RUN mvn --version

# Install standalone binary of firebase-tools (https://firebase.google.com/docs/cli)
RUN wget https://firebase.tools/bin/linux/latest -O /usr/bin/firebase && chmod +x /usr/bin/firebase
RUN firebase --version

WORKDIR /application
COPY ./ /application

# Pre-fetch maven dependencies
RUN mvn install:install-file -Dfile=NanostreamDataflowMain/libs/japsa.jar -DgroupId=coin -DartifactId=japsa -Dversion=1.9-3c -Dpackaging=jar && \
    mvn install:install-file -Dfile=NanostreamDataflowMain/libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar  && \
    mvn -B dependency:get -Dartifact=org.apache.maven:maven-core:2.2.1 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-clean-plugin:3.1.0 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-release-plugin:2.3.2 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-release-plugin:2.5.3 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-resources-plugin:3.1.0 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-compiler-plugin:3.8.1 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M4 && \
    mvn -B dependency:get -Dartifact=org.apache.maven.plugins:maven-war-plugin:3.2.3 && \
    mvn -B dependency:get -Dartifact=org.codehaus.mojo:exec-maven-plugin:1.6.0 && \
    mvn -B dependency:get -Dartifact=com.google.cloud.tools:appengine-maven-plugin:2.2.0 && \
    mvn -B dependency:get -Dartifact=com.github.eirslett:frontend-maven-plugin:1.9.1 && \
    mvn -B dependency:go-offline -f NanostreamDataflowMain/pipeline/pom.xml && \
    mvn -B dependency:go-offline -f NanostreamDataflowMain/webapp/pom.xml

# Run python3 install script using 'unbuffered' flag
CMD ["python3", "-u", "/application/launcher/install.py"]