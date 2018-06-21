#!/bin/bash

SCRIPT_DIR="$(dirname $0)"

CONFIG_FILE="$1"
if [[ "$CONFIG_FILE" == "" ]]; then
    echo "Specify config file as first argument to this script"
fi

java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 \
-cp $SCRIPT_DIR/lib/flute-server-all-FLUTE_VERSION.jar \
-Djava.net.preferIPv4Stack=true \
-Dlog4j.configuration=file://$SCRIPT_DIR/log4j.xml \
com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE