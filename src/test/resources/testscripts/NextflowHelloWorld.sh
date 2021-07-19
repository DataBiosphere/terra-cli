#!/bin/bash
set -e
## This script runs a hello world Nextflow pipeline.
## Do this in an integration test because it generates local files, which are cleaned up by integration,
# but not unit, tests.

terra status
terra workspace create
terra status

cat mariko.txt

terra nextflow run hello > nextflowHelloWorld_stdout.txt
