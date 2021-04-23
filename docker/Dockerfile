# Java 11 base image
FROM openjdk:11.0-jre-slim

# working directory when the docker is run
WORKDIR /usr

# install curl (to download install packages) and python (for gcloud sdk)
RUN apt-get update \
    && apt-get install -y curl \
    && apt-get install -y python

# install Google Cloud SDK (gcloud, gsutil, bq)
# these commands come from https://cloud.google.com/sdk/docs/downloads-versioned-archives
# other examples of installing gcloud in a docker image:
#   - AoU (https://github.com/all-of-us/workbench/blob/master/api/src/dev/server/Dockerfile#L34)
#   - Terra Jupyter (https://github.com/DataBiosphere/terra-docker/blob/master/terra-jupyter-base/Dockerfile#L74)
ENV CLOUD_SDK_VERSION 337.0.0
ENV PATH /usr/local/google-cloud-sdk/bin:$PATH
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    tar xzf google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    rm google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    mv google-cloud-sdk/ /usr/local/ && \
    gcloud config set component_manager/disable_update_check true

# install Nextflow
ENV NXF_VER 20.10.0
ENV NXF_MODE google
RUN curl -s https://get.nextflow.io | bash \
    && mv nextflow /usr/local/bin

# copy any custom scripts into the image
COPY scripts/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
