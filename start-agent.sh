#!/bin/bash

ETCD_HOST=etcd
ETCD_PORT=2379
ETCD_URL=http://$ETCD_HOST:$ETCD_PORT

echo ETCD_URL = $ETCD_URL

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -XX:+UseBoundThreads \
       -Xms1536M \
       -Xmx1536M \
       -Dtype=consumer \
       -Dserver.port=20000 \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -XX:+UseBoundThreads \
       -Xms512M \
       -Xmx512M \
       -Dtype=provider \
       -Ddefault.weight=16 \
       -Ddubbo.protocol.port=20880 \
       -Dserver.port=30000 \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -XX:+UseBoundThreads \
       -Xms1536M \
       -Xmx1536M \
       -Dtype=provider \
       -Ddefault.weight=32 \
       -Ddubbo.protocol.port=20880 \
       -Dserver.port=30000 \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /Users/Jason/opensource/middlewarerace/mesh-agent/target/mesh-agent-0.0.1-SNAPSHOT.jar

elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -XX:+UseBoundThreads \
       -Xms2560M \
       -Xmx2560M \
       -Dtype=provider \
       -Ddefault.weight=48 \
       -Ddubbo.protocol.port=20880 \
       -Dserver.port=30000 \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
else
  echo "Unrecognized arguments, exit."
  exit 1
fi
