#!/bin/bash

CONFIG_FILE=integration/src/test/resources/acceptance-test-flute.properties
REPORT_CONFIG_FILE=integration/src/test/resources/acceptance-test-report-server-flute.properties

HTTP_SERVER_PORT=15002
REPORTING_HTTP_SERVER_PORT=15003
DB_DIR="/tmp/$USER/flute-db"
DB_URL="jdbc:h2:tcp://localhost:9092/flute-db"
REPORT_DB_URL="jdbc:h2:tcp://localhost:9092/flute-report-db"

rm -rf $DB_DIR
mkdir -p $DB_DIR
mkdir -p build/logs/


(java -cp integration/build/libs/integration-all-0.1.1.jar com.aitusoftware.flute.integration.service.DatabaseServerMain $DB_DIR) < /dev/null > build/logs/db-server.log 2> build/logs/db-server.log &
(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -cp persistor/build/libs/persistor-all-0.1.1.jar -Dlog4j.configuration=file://`pwd`/persistor/src/main/resources/logging/log4j.xml -Dflute.db.metrics.url=$DB_URL com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE) < /dev/null > build/logs/persistor.log 2> build/logs/persistor.log &
(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -cp server/build/libs/server-all-0.1.1.jar -Dlog4j.configuration=file://`pwd`/server/src/main/resources/logging/log4j.xml -Dflute.db.metrics.url=$DB_URL -Dflute.db.reports.url=$REPORT_DB_URL -Dflute.server.httpPort=$HTTP_SERVER_PORT -Dflute.resource.base=server/src/main/resources/ui com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE)  < /dev/null > build/logs/http-server.log 2> build/logs/http-server.log &

