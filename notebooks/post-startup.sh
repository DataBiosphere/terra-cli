#!/bin/bash
#
# Default post startup script for GCP notebooks.
# GCP Notebook post startup scrips are run only when the instance is first created.

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
sudo -u "${JUPYTER_USER}" sh -c "mkdir -p /home/${JUPYTER_USER}/.terra"
exec >> /home/"${JUPYTER_USER}"/.terra/post-startup-output.txt
exec 2>&1

# start ssh-agent and add it to the bash profile
ssh-agent -s
echo eval '"$(ssh-agent -s)"' >> ~/.bash_profile

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

# Install Nextflow. Use an edge release that allows overriding the default compute engine SA and VPC network
export NXF_VER=21.05.0-edge
export NXF_MODE=google
sudo -u "${JUPYTER_USER}" sh -c "curl -s https://get.nextflow.io | bash"
sudo mv nextflow /usr/bin/nextflow

# Install & configure the Terra CLI
sudo -u "${JUPYTER_USER}" sh -c "curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash"
sudo cp terra /usr/bin/terra
# Set browser manual login since that's the only login supported from an GCP Notebook VM.
sudo -u "${JUPYTER_USER}" sh -c "terra config set browser MANUAL"
# Set the CLI terra server based on the terra server that created the GCP notebook retrieved from
# the VM metadata, if set.
readonly TERRA_SERVER=$(get_metadata_value "instance/attributes/terra-cli-server")
if [[ -n "${TERRA_SERVER}" ]]; then
  sudo -u "${JUPYTER_USER}" sh -c "terra server set --name=${TERRA_SERVER}"
fi
# Set the CLI terra workspace id using the VM metadata, if set.
readonly TERRA_WORKSPACE=$(get_metadata_value "instance/attributes/terra-workspace-id")
if [[ -n "${TERRA_WORKSPACE}" ]]; then
  sudo -u "${JUPYTER_USER}" sh -c "terra workspace set --id=${TERRA_WORKSPACE} --defer-login"
fi
