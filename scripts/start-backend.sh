#!/bin/bash

CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-flute.properties
REPORT_CONFIG_FILE=flute-integration/src/test/resources/acceptance-test-report-server-flute.properties

HTTP_SERVER_PORT=15002
REPORTING_HTTP_SERVER_PORT=15003
DB_DIR="/tmp/$USER/flute-db"
DB_URL="jdbc:h2:tcp://localhost:9092/flute-db"
REPORT_DB_URL="jdbc:h2:tcp://localhost:9092/flute-report-db"

rm -rf $DB_DIR
mkdir -p $DB_DIR
mkdir -p build/logs/


(java -cp flute-integration/build/libs/flute-integration-all-0.1.4.jar -Djava.net.preferIPv4Stack=true com.aitusoftware.flute.integration.service.DatabaseServerMain $DB_DIR) < /dev/null > build/logs/db-server.log 2> build/logs/db-server.log &
(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -cp flute-persistor/build/libs/flute-persistor-all-0.1.4.jar  -Djava.net.preferIPv4Stack=true -Dlog4j.configuration=file://`pwd`/flute-persistor/src/main/resources/logging/log4j.xml -Dflute.db.metrics.url=$DB_URL com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE) < /dev/null > build/logs/persistor.log 2> build/logs/persistor.log &
(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -cp flute-server/build/libs/flute-server-all-0.1.4.jar  -Djava.net.preferIPv4Stack=true -Dlog4j.configuration=file://`pwd`/flute-server/src/main/resources/logging/log4j.xml -Dflute.resource.base=`pwd`/flute-server/src/main/resources/ui -Dflute.db.metrics.url=$DB_URL -Dflute.db.reports.url=$REPORT_DB_URL -Dflute.server.httpPort=$HTTP_SERVER_PORT com.aitusoftware.flute.server.http.MetricsServerMain $CONFIG_FILE)  < /dev/null > build/logs/metrics-server.log 2> build/logs/metrics-server.log &
(java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007 -cp flute-server/build/libs/flute-server-all-0.1.4.jar  -Djava.net.preferIPv4Stack=true -Dlog4j.configuration=file://`pwd`/flute-server/src/main/resources/logging/log4j.xml -Dflute.resource.base=`pwd`/flute-server/src/main/resources/ui -Dflute.db.metrics.url=$DB_URL -Dflute.db.reports.url=$REPORT_DB_URL -Dflute.server.httpPort=$REPORTING_HTTP_SERVER_PORT com.aitusoftware.flute.server.reporting.http.ReportAdminServerMain $CONFIG_FILE)  < /dev/null > build/logs/report-admin-server.log 2> build/logs/report-admin-server.log &

