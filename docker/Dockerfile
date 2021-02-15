# Java 11 base image
FROM openjdk:11.0-jre-slim

# working directory when the docker is run
WORKDIR /usr

# install curl
RUN apt-get update \
    && apt-get install -y curl

# install Google Cloud SDK (gcloud, gsutil, bq)
# these commands come from https://cloud.google.com/sdk/docs/install
# other examples of installing gcloud in a docker image:
#   - AoU (https://github.com/all-of-us/workbench/blob/master/api/src/dev/server/Dockerfile#L34)
#   - Terra Jupyter (https://github.com/DataBiosphere/terra-docker/blob/master/terra-jupyter-base/Dockerfile#L74)
ENV CLOUD_SDK_VERSION 326.0.0-0
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
    && apt-get install apt-transport-https ca-certificates gnupg -y \
    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg  add - \
    && apt-get update -y && apt-get install google-cloud-sdk=${CLOUD_SDK_VERSION} -y

# install Nextflow
ENV NXF_VER 20.10.0
ENV NXF_MODE google
RUN curl -s https://get.nextflow.io | bash \
    && mv nextflow /usr/local/bin

# copy any custom scripts into the image
COPY scripts/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
