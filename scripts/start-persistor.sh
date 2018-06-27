#!/bin/bash

SCRIPT_FILE_DIR="$(dirname $0)"
SCRIPT_DIR="$(readlink -f $SCRIPT_FILE_DIR)"
LIB_DIR="$(readlink -f $SCRIPT_DIR/../lib)"
BIN_DIR="$(readlink -f $SCRIPT_DIR/../bin)"
LOG_DIR="$(readlink -f $SCRIPT_DIR/../log)"

CONFIG_FILE="$1"
if [[ "$CONFIG_FILE" == "" ]]; then
    echo "Specify config file as first argument to this script"
fi

java  \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
-cp "$BIN_DIR/flute-persistor-all-FLUTE_VERSION.jar:$LIB_DIR/*" \
-Djava.net.preferIPv4Stack=true \
-Dlog.dir=$LOG_DIR -Dlog.name=persistor \
-Dlog4j.configuration=file://$SCRIPT_DIR/log4j.xml \
com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE
