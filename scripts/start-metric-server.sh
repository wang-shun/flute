#!/bin/bash

CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-flute.properties
REPORT_CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-report-server-flute.properties

HTTP_SERVER_PORT=15002
DB_URL="jdbc:h2:tcp://localhost:9092/flute-db"
REPORT_DB_URL="jdbc:h2:tcp://localhost:9092/flute-report-db"
LOG_DIR="logs/"

mkdir -p $LOG_DIR

(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 \
-cp flute-server/build/libs/flute-server-all-FLUTE_VERSION.jar \
-Djava.net.preferIPv4Stack=true \
-Dlog4j.configuration=file://`pwd`/flute-server/src/main/resources/logging/log4j.xml \
-Dflute.db.metrics.url=$DB_URL -Dflute.db.reports.url=$REPORT_DB_URL \
-Dflute.server.httpPort=$HTTP_SERVER_PORT \
com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE)  < /dev/null > $LOG_DIR/metrics-server.log 2> $LOG_DIR/metrics-server.log &