#!/bin/bash

# Load variables
source bash_variables.sh

echo "================================================="
echo "   LAMPORT SYSTEM: REMOTE DEPLOYMENT & START"
echo "================================================="

### 1. FETCH CODE (ON MASTER VM)
if [ ! -d ${SEMWORK_HOMEDIR}/${CODE_SUBDIR} ] ; then
  echo "Downloading code from Git..."
  mkdir -p ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  git clone ${GIT_URL} ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  cd ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  git checkout ${GIT_BRANCH}
else
  echo "Updating existing code..."
  cd ${SEMWORK_HOMEDIR}/${CODE_SUBDIR}
  git pull
fi

### 2. BUILD JAR (ON MASTER VM)
echo "Compiling with Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Locate the Fat JAR
FAT_JAR_NAME=$(ls target/*.jar | grep -v "original" | head -n 1)
FAT_JAR_PATH=$(pwd)/target/$FAT_JAR_NAME
echo "JAR created: $FAT_JAR_PATH"

### 3. DISTRIBUTE & START (REMOTE VMs)
# We use sshpass to handle passwords automatically

for ID in $(seq 1 $NUM_NODES) ; do
  TARGET_IP=${NODE_IP[$ID]}
  echo "-------------------------------------------------"
  echo "Deploying Node $ID to $TARGET_IP..."

  # A. Create Directory on Remote VM
  sshpass -p ${SSH_PASS} ssh -o StrictHostKeyChecking=no ${SSH_USER}@${TARGET_IP} "mkdir -p ${SEMWORK_HOMEDIR}/NODE_${ID}"

  # B. Copy JAR to Remote VM
  echo "  -> Copying JAR..."
  sshpass -p ${SSH_PASS} scp -o StrictHostKeyChecking=no ${FAT_JAR_PATH} ${SSH_USER}@${TARGET_IP}:${SEMWORK_HOMEDIR}/NODE_${ID}/semwork.jar

  # C. Start Node in Tmux
  echo "  -> Starting Application..."
  # We kill old session if exists, then start new one
  sshpass -p ${SSH_PASS} ssh -o StrictHostKeyChecking=no ${SSH_USER}@${TARGET_IP} "tmux kill-session -t NODE_${ID} 2>/dev/null; tmux new-session -d -s NODE_${ID}"

  # Send the java command to the tmux session
  # We set java.rmi.server.hostname so RMI works across VMs
  CMD="cd ${SEMWORK_HOMEDIR}/NODE_${ID}/ && java -Djava.rmi.server.hostname=${TARGET_IP} -jar semwork.jar ${NODE_PORT[$ID]}"

  sshpass -p ${SSH_PASS} ssh -o StrictHostKeyChecking=no ${SSH_USER}@${TARGET_IP} "tmux send -t NODE_${ID} '${CMD}' ENTER"

  echo "  -> Node $ID started on $TARGET_IP (Ports: ${NODE_PORT[$ID]} / ${NODE_API_PORT[$ID]})"
done

echo "================================================="
echo "   DEPLOYMENT COMPLETE"
echo "================================================="