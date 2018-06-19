#!/bin/bash

CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-flute.properties

DB_URL="jdbc:h2:tcp://localhost:9092/flute_db"
LOG_DIR="logs/"

#flute_metrics@
#flute_reports@

mkdir -p $LOG_DIR

(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
-cp flute-persistor/build/libs/flute-persistor-all-FLUTE_VERSION.jar \
-Djava.net.preferIPv4Stack=true \
-Dlog4j.configuration=file://`pwd`/flute-persistor/src/main/resources/logging/log4j.xml \
-Dflute.db.metrics.url=$DB_URL \
com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE) < /dev/null > $LOG_DIR/persistor.log 2> $LOG_DIR/persistor.log &