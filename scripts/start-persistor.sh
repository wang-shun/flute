#!/bin/bash

SCRIPT_FILE_DIR="$(dirname $0)"
SCRIPT_DIR="$(readlink -f $SCRIPT_FILE_DIR)"
LIB_DIR="$(readlink -f $SCRIPT_DIR/../lib)"

CONFIG_FILE="$1"
if [[ "$CONFIG_FILE" == "" ]]; then
    echo "Specify config file as first argument to this script"
fi

java  \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
-cp $LIB_DIR/flute-persistor-all-FLUTE_VERSION.jar \
-Djava.net.preferIPv4Stack=true \
-Dlog4j.configuration=file://$SCRIPT_DIR/log4j.xml \
com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE
