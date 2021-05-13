#!/bin/bash

# Send the output from this script to a tmp file.
exec >> /tmp/post-startup-output.txt
exec 2>&1

# Switch to the 'jupyter' user used in AI notebooks.
su jupyter

echo $USER

# Default post startup script for AI notebooks

# TODO git config?
# TODO source Terra workspace id as env variable from metadata server?

# Install these globally (not in a virtual environment)\n",
pip3 install --upgrade pre-commit nbdime nbstripout pylint pytest

# Install nbstripout globally
nbstripout --install

# Install & configure the Terra CLI

# TODO should we just use the latest version?
mkdir ~/bin
cd ~/bin || exit
env TERRA_CLI_VERSION="0.38.0" curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash
echo 'export PATH=${HOME}/bin:${PATH}' >> ~/.bash_profile
./terra

# Set browser manual since that is always what we want for cloudtops.
./terra config set browser manual

# TODO mount the current workspace?
