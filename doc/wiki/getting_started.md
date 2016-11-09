## Configure and start a database

At this point, we need to do one of two things:

   1. Use an existing database server, with an existing user that has `CREATE` privileges.
   2. Use a temporary `h2` database for testing purposes.

### Use an existing database server

Ensure that you have a JDBC-compliant driver available for your chosen database server.

   1. Create two databases: `flute_report` and `flute_data`.
   2. Create a user with `CREATE` privileges on those two databases. Note the username (`$USER`) and password (`$PASSWORD`).

### Use a temporary database

For testing purposes, __Flute__ comes with a quick-start `h2` database server.

   1. Download [flute-integration-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-integration-all-0.1.2.jar).
   2. Choose a directory to host the database `$DB_DIR` (e.g. `/tmp/flute`).
   2. Execute command `java -cp ./flute-integration-all-0.1.2.jar com.aitusoftware.flute.integration.service.DatabaseServerMain $DB_DIR`.

## Configure and start the persistor

The `Persistor` is the component responsible for receiving histogram data from clients, and storing it in the database.

Using [acceptance-test-flute.properties](https://github.com/aitusoftware/flute/blob/master/integration/src/test/resources/acceptance-test-flute.properties) 
as a guide, configure the `persistor` to use your database of choice in `$CONFIG_FILE`:
 
```
# replace 'SA' with your database user, or leave if using the bundled h2 database
flute.db.metrics.username=SA
# set database password, or leave if using the bundled h2 database
flute.db.metrics.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
flute.server.tcp.listenAddress=0.0.0.0:51000
flute.histogram.maxValue=100000
flute.histogram.significantDigits=3
```

   1. Download [flute-persistor-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-persistor-all-0.1.2.jar)
   2. Determine the correct JDBC url (if using the bundled h2 database, this will be `jdbc:h2:tcp://localhost:9092/flute-db`)
   2. Execute command `java -cp flute-persistor-all-0.1.2.jar -Dflute.db.metrics.url=$JDBC_URL com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE`

## Configure and start the server

The `Server` component is responsible for serving reports generated from stored histogram data. 
Custom reports can be generated, and their meta-data will be stored in a separate database.

The `server` requires its own config; required properties are as follows:

```
# replace 'SA' with your database user, or leave if using the bundled h2 database
flute.db.metrics.username=SA
# set database password, or leave if using the bundled h2 database
flute.db.metrics.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
# replace 'SA' with your database user, or leave if using the bundled h2 database
flute.db.reports.username=SA
# set database password, or leave if using the bundled h2 database
flute.db.reports.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
flute.histogram.maxValue=100000
flute.histogram.significantDigits=3
flute.server.httpPort=15002
```

   1. Download [flute-server-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-server-all-0.1.2.jar)
   2. Determine the correct __metrics__ JDBC url (if using the bundled h2 database, this will be `jdbc:h2:tcp://localhost:9092/flute-db`)
   2. Determine the correct __reports__ JDBC url (if using the bundled h2 database, this will be `jdbc:h2:tcp://localhost:9092/flute-report-db`)
   2. Execute command `java -cp flute-server-all-0.1.2.jar -Dflute.db.metrics.url=$DB_URL -Dflute.db.reports.url=$REPORT_DB_URL -Dflute.server.httpPort=$HTTP_SERVER_PORT -Dflute.resource.base=server/src/main/resources/ui com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE`
