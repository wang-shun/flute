#!/bin/bash

CONFIG_FILE="$1"
if [[ "$CONFIG_FILE" == "" ]]; then
    echo "Specify config file as first argument to this script"
fi

java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 \
-cp flute-server/build/libs/flute-server-all-FLUTE_VERSION.jar \
-Djava.net.preferIPv4Stack=true \
-Dlog4j.configuration=file://log4j.xml \
com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE