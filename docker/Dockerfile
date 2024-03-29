# Java 17 base image
# see https://github.com/broadinstitute/dsp-appsec-blessed-images/tree/main/jre
FROM us.gcr.io/broad-dsp-gcr-public/base/jre:17-debian

# working directory when the docker is run
WORKDIR /usr

# install curl (to download install packages), python (for gcloud sdk) and git
RUN apt-get update \
    && apt-get install -y \
    curl \
    python3 \
    git

# install Google Cloud SDK (gcloud, gsutil, bq)
# these commands come from https://cloud.google.com/sdk/docs/downloads-versioned-archives
# other examples of installing gcloud in a docker image:
#   - AoU (https://github.com/all-of-us/workbench/blob/master/api/src/dev/server/Dockerfile#L34)
#   - Terra Jupyter (https://github.com/DataBiosphere/terra-docker/blob/master/terra-jupyter-base/Dockerfile#L74)
ENV CLOUD_SDK_VERSION 439.0.0
ENV PATH /usr/local/google-cloud-sdk/bin:$PATH
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    tar xzf google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    rm google-cloud-sdk-${CLOUD_SDK_VERSION}-linux-x86_64.tar.gz && \
    mv google-cloud-sdk/ /usr/local/ && \
    gcloud config set component_manager/disable_update_check true && \
    # For gcloud alpha storage \
    gcloud components install alpha --quiet

# install Nextflow
ENV NXF_VER 21.10.6
ENV NXF_MODE google
RUN curl -s https://get.nextflow.io | bash \
    && mv nextflow /usr/local/bin

# copy any custom scripts into the image
COPY scripts/* /usr/local/bin/
RUN chmod a+x /usr/local/bin/*
