#!/bin/bash

echo "Initialize ssh key"
eval "$(ssh-agent -s)"
echo "$(whoami)"
echo "${SSH_PRIVATE_KEY}"
ssh-add /root/.ssh/terra_id_rsa
ssh-keyscan -H github.com >> ~/.ssh/known_hosts