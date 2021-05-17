#!/bin/bash
# Default post startup script for AI notebooks

# Send the output and console from this script to a tmp file for debugging.
exec >> /tmp/post-startup-output.txt
exec 2>&1

set -x

echo "USER: ${USER}"

# The linux user that JupyterLab will be running as. It's important to do most installation in the
# user space.
VM_USER="jupyter"

# TODO git config?
# TODO source Terra workspace id as env variable from metadata server?

conda info --envs

# Install these globally (not in a virtual environment)\n",
# sudo apt-get --assume-yes install python-setuptools DO NOT SUBMIT remove me?
# TODO use conda directly? vm user still needed?
# sudo -u ${VM_USER} sh -c '
conda install -y pre-commit nbdime nbstripout pylint pytest

# Install nbstripout globally
nbstripout --install --global

# Install & configure the Terra CLI

# TODO should we just use the latest version?
cd /tmp || exit
sudo -u ${VM_USER} sh -c 'env TERRA_CLI_VERSION="0.38.0" curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash'

# Set browser manual since that is always what we want for cloudtops.
sudo -u ${VM_USER} sh -c './terra config set browser manual'

sudo cp terra /usr/bin/terra

# TODO mount the current workspace?
