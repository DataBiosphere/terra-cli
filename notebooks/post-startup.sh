#!/bin/bash
#
# Default post startup script for AI notebooks.
# AI Notebook post startup scrips are run only when the instance is first created.

set -o errexit
set -o nounset
set -o pipefail
set -o xtrace

# The linux user that JupyterLab will be running as. It's important to do some parts of setup in the
# user space, such as setting Terra CLI settings which are persisted in the user's $HOME.
# This post startup script is not run by the same user.
readonly JUPYTER_USER="jupyter"

# Move to the /tmp directory to let any artifacts left behind by this script can be removed.
cd /tmp || exit

# Send stdout and stderr from this script to a file for debugging.
# Make the .terra directory as the user so that they own it and have correct linux permissions.
sudo -u "${JUPYTER_USER}" sh -c mkdir -p /home/"${JUPYTER_USER}"/.terra
exec >> /home/"${JUPYTER_USER}"/.terra/post-startup-output.txt
exec 2>&1

#######################################
# Retrieve a value from the GCE metadata server or return nothing.
# See https://cloud.google.com/compute/docs/storing-retrieving-metadata
# Arguments:
#   The metadata subpath to retrieve
# Returns:
#   The metadata value if found, or else an empty string
#######################################
function get_metadata_value() {
  curl --retry 5 -s -f \
    -H "Metadata-Flavor: Google" \
    "http://metadata/computeMetadata/v1/$1"
}

# Install common packages in conda environment
/opt/conda/bin/conda install -y pre-commit nbdime nbstripout pylint pytest
# Install nbstripout for the jupyter user in all git repositories.
sudo -u "${JUPYTER_USER}" sh -c "/opt/conda/bin/nbstripout --install --global"

# Install & configure the Terra CLI
sudo -u "${JUPYTER_USER}" sh -c "curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash"
sudo cp terra /usr/bin/terra
# Set browser manual login since that's the only login supported from an AI Notebook VM.
sudo -u "${JUPYTER_USER}" sh -c "terra config set browser MANUAL"
# Set the CLI terra server based on the terra server that created the AI notebook retrieved from
# the VM metadata, if set.
readonly TERRA_SERVER=$(get_metadata_value "instance/attributes/terra-cli-server")
if [[ -n "${TERRA_SERVER}" ]]; then
  sudo -u "${JUPYTER_USER}" sh -c "terra server set --name=${TERRA_SERVER}"
fi
