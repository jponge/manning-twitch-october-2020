#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

HTTP_PORT=$1 STORE_HOST=$2 STORE_PORT=$3 java \
  -Dhazelcast.logging.type=slf4j \
  --add-modules java.se \
  --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.management/sun.management=ALL-UNNAMED \
  --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
  -jar edge/target/edge-0-SNAPSHOT.jar
