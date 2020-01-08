FROM google/cloud-sdk

# install maven for compile java code
RUN apt-get install -y maven wget
RUN mvn --version

# Install standalone binary of firebase-tools (https://firebase.google.com/docs/cli)
RUN wget https://firebase.tools/bin/linux/latest -O /usr/bin/firebase && chmod +x /usr/bin/firebase
RUN firebase --version

WORKDIR /application
COPY ./ /application

CMD ["python3", "-u", "/application/launcher/install.py"]