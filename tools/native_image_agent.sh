#!/usr/bin/env bash

# Usage: ./tools/native_image_agent <terra-arguments>

# Run the Native Image Agent with the GraalVM JVM to obtain necessary configuration options.
# See https://www.graalvm.org/22.0/reference-manual/native-image/Agent/
# The salient part is -agentlib:native-image-agent=config-output-dir=./build/config_out
# Prerequisite: GraalVM
# The application should be built, and we need something in ~/.terra (e.g. from local_dev.sh).
# I do ./gradlew installDist to get the classes into the classpath.

# Any terra arguments should be passed to this script. Not all terra sessions will use all reflected
# classes. For example, in order to instrument the Google OAuth code, do something like `workspace list`
# or `auth login`. It's expected that the config files we need will ultimately be the union of
# generated configs from several terra invocations.

OUTPUT_DIR=./build/agent_out
mkdir -p $OUTPUT_DIR

# On my machine the path looks like /Users/jaycarlton/.jenv/versions/graalvm64-11.0.14/bin/java
# To obtain the full set of arguments for the classpath, etc, add the following to the end of the
# generated shell script at build/install/terra-cli/bin/terra:
# `echo "$JAVACMD" "$@"`

CLASSPATH=\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/terra-cli-0.142.0.jar:/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/datarepo-client-1.0.155-SNAPSHOT.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/workspace-manager-client-0.254.161-SNAPSHOT.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-bigquery-0.11.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloudresourcemanager-1.3.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-notebooks-0.8.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-storage-0.13.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/cloud-resource-schema-0.9.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-services-common-0.9.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/common-0.9.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/terra-resource-janitor-client-0.4.0-SNAPSHOT.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/swagger-annotations-2.1.6.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-hk2-2.30.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/picocli-4.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/logback-classic-1.2.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/docker-java-core-3.2.7.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/docker-java-api-3.2.7.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/docker-java-transport-httpclient5-3.2.7.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/httpclient5-5.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/sam-client_2.13-0.1-ffb0a89-SNAP.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/org.apache.oltu.oauth2.client-1.0.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/org.apache.oltu.oauth2.common-1.0.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/slf4j-api-1.7.30.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-media-json-jackson-2.32.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-module-jaxb-annotations-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-datatype-jdk8-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-databind-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-annotations-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-datatype-jsr310-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloud-storage-1.117.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloud-bigquery-1.128.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jackson-core-2.12.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-oauth-client-jetty-1.31.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-oauth-client-java6-1.31.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloud-core-http-1.95.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/gax-httpjson-0.82.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloud-core-1.95.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-cloud-pubsub-1.112.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/gax-1.65.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-auth-library-oauth2-http-0.26.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-client-2.32.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-media-multipart-2.32.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-common-2.32.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/hk2-locator-2.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/javassist-3.25.0-GA.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/logback-core-1.2.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-services-cloudresourcemanager-v3-rev20210411-1.31.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-services-notebooks-v1-rev20201110-1.30.10.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-services-bigquery-v2-rev20210410-1.31.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-client-1.31.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-oauth-client-1.31.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/api-common-1.10.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/proto-google-cloud-pubsub-v1-1.94.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/auto-value-annotations-1.8.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-http-client-appengine-1.39.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-http-client-gson-1.39.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-http-client-apache-v2-1.39.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-http-client-1.39.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jsr305-3.0.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-auth-library-credentials-0.26.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-http-client-jackson2-1.39.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/protobuf-java-util-3.17.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/opencensus-contrib-http-util-0.28.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/opencensus-impl-0.28.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/opencensus-impl-core-0.28.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/guava-30.1.1-jre.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/gson-fire-1.8.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/gson-2.8.7.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/proto-google-iam-v1-1.0.14.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/proto-google-common-protos-2.3.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/protobuf-java-3.17.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/opencensus-api-0.28.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-context-1.38.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/httpclient-4.5.13.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-logging-1.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-codec-1.15.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/httpcore-4.4.14.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/j2objc-annotations-1.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/checker-compat-qual-2.5.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/javax.annotation-api-1.3.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/failureaccess-1.0.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/error_prone_annotations-2.7.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/threetenbp-1.5.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/google-api-services-storage-v1-rev20210127-1.31.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/docker-java-transport-3.2.7.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-io-2.6.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-compress-1.20.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-lang-2.6.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/bcpkix-jdk15on-1.64.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jna-5.5.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/scala-library-2.13.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/swagger-annotations-1.6.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/logging-interceptor-3.12.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/okhttp-3.12.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/commons-lang3-3.8.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jersey-entity-filtering-2.32.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jakarta.ws.rs-api-2.1.6.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jakarta.annotation-api-1.3.5.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/hk2-api-2.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/hk2-utils-2.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jakarta.inject-2.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/osgi-resource-locator-1.0.3.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/aopalliance-repackaged-2.6.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/bcprov-jdk15on-1.64.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/httpcore5-5.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/okio-1.15.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/mimepull-1.9.13.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-alts-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-api-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-auth-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-core-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-grpclb-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-netty-shaded-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-protobuf-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-protobuf-lite-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/grpc-stub-1.37.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/gax-grpc-1.63.0.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/json-20140107.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/disruptor-3.4.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jakarta.xml.bind-api-2.3.2.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/jakarta.activation-api-1.2.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/animal-sniffer-annotations-1.20.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/conscrypt-openjdk-uber-2.5.1.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/annotations-4.1.1.4.jar:\
/Users/jaycarlton/repos/terra-cli/build/install/terra-cli/lib/perfmark-api-0.23.0.jar

java \
  -agentlib:native-image-agent=config-output-dir=$OUTPUT_DIR \
  --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  -classpath $CLASSPATH \
  bio.terra.cli.command.Main "$@"

