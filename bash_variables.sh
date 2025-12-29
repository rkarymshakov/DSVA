#!/bin/bash

### 1. GIT CONFIGURATION
GIT_URL=git@github.com:rkarymshakov/DSVA.git
GIT_BRANCH=main

### 2. DEPLOYMENT CONFIGURATION
# Directory on the remote machines where the app will run
SEMWORK_HOMEDIR=/home/dsv/semwork
CODE_SUBDIR=code

# SSH Credentials for remote VMs
# (All VMs must have this user/password, or use SSH keys)
SSH_USER=dsv
SSH_PASS=dsv

### 3. NODE DEFINITIONS
NUM_NODES=2
BASE_PORT=2010

# DEFINE YOUR REAL VM IPS HERE
# Example: VM1 is Master, VM2 is Worker
NODE_IP[1]=192.168.56.101
NODE_IP[2]=192.168.56.102
# NODE_IP[3]=192.168.56.103

# Auto-calculate ports (RMI Port and REST API Port)
for I in $(seq 1 $NUM_NODES) ; do
  # RMI Port (e.g., 2010, 2020)
  NODE_PORT[$I]=$((${BASE_PORT}+(${I}-1)*10))
  # REST API Port (e.g., 3010, 3020)
  NODE_API_PORT[$I]=$((${NODE_PORT[$I]}+1000))
done