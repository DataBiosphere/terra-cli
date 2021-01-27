# Java 11 base image
FROM openjdk:11.0-jre-slim

# working directory when the docker is run
WORKDIR /usr

# install curl
RUN apt-get update && \
        apt-get upgrade -y && \
        apt-get install -y curl

# install Nextflow
RUN curl -s https://get.nextflow.io | bash
RUN mv nextflow /usr/local/bin

# install Google Cloud SDK (gcloud, gsutil, bq)
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
RUN apt-get install apt-transport-https ca-certificates gnupg -y
RUN curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg  add -
RUN apt-get update -y && apt-get install google-cloud-sdk -y

# copy the terra_init script into the image
COPY terra_init.sh /usr/local/bin/terra_init.sh
