#!/bin/bash
# Default post startup script for AI notebooks

# Send the output and console from this script to a tmp file for debugging.
exec >> /tmp/post-startup-output.txt
exec 2>&1
set -x

# Import GCP click to deploy convenience functions.
# https://github.com/GoogleCloudPlatform/click-to-deploy/blob/master/vm/chef/cookbooks/c2d-config/files/c2d-utils
source /opt/c2d/c2d-utils || exit 1

echo "USER: ${USER}"

# The linux user that JupyterLab will be running as. It's important to do most installation in the
# user space.
VM_USER="jupyter"


# TODO git config?
# TODO source Terra workspace id as env variable from metadata server?

CONDA="/opt/conda/bin/conda"
${CONDA} info --envs

# Install common packages in conda environment
${CONDA} install -y pre-commit nbdime nbstripout pylint pytest

# Install nbstripout globally
/opt/conda/bin/nbstripout --install --global

# Install & configure the Terra CLI

# TODO should we just use the latest version?
cd /tmp || exit
sudo -u ${VM_USER} sh -c 'env TERRA_CLI_VERSION="0.38.0" curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash'

# Set browser manual since that is always what we want for cloudtops.
sudo -u ${VM_USER} sh -c './terra config set browser manual'

sudo cp terra /usr/bin/terra

# TODO mount the current workspace?
