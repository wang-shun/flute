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
