#!/bin/bash
# Default post startup script for AI notebooks.
# AI Notebook post startup scrips are run only when the instance is first created.

# Send stdout and stderr from this script to a tmp file for debugging.
exec >> /tmp/post-startup-output.txt
exec 2>&1
set -x

# The linux user that JupyterLab will be running as. It's important to do some parts of setup in the
# user space.
JUPYTER_USER="jupyter"

# Install common packages in conda environment
/opt/conda/bin/conda install -y pre-commit nbdime nbstripout pylint pytest
# Install nbstripout for the jupyter user in all git repositories.
sudo -u ${JUPYTER_USER} sh -c "/opt/conda/bin/nbstripout --install --global"

# Install & configure the Terra CLI
cd /tmp || exit
sudo -u ${JUPYTER_USER} sh -c "curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash"
# Set browser manual login since that's the only login supported from an AI Notebook VM.
sudo -u ${JUPYTER_USER} sh -c "./terra config set browser manual"
sudo cp terra /usr/bin/terra
