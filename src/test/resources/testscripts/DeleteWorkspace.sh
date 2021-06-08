#!/bin/bash
set -e
## This script deletes the current workspace after running an integration test.

terra status

# delete the workspace
terra workspace delete
