# The easy way

Clone the git repository, and run the following command:

```
./gradlew bundleJar startBackend
```

# The other way

Follow the steps below to configure and start __Flute__ services.

## Configure and start a database

At this point, we need to do one of two things:

   1. Use an existing database server, with an existing user that has `CREATE` privileges.
   2. Use a temporary `h2` database for testing purposes.

### Use an existing database server

Ensure that you have a JDBC-compliant driver available for your chosen database server.

   1. Create two databases: `flute-report-db` and `flute-db`.
   2. Create a user with `CREATE` privileges on those two databases. Note the username (`$USER`) and password (`$PASSWORD`).

### Use a temporary database

For testing purposes, __Flute__ comes with a quick-start `h2` database server.

   1. Download [flute-integration-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-integration-all-0.1.2.jar).
   2. Choose a directory to host the database `$DB_DIR` (e.g. `/tmp/flute`).
   2. Execute command: 
```
java -cp ./flute-integration-all-0.1.2.jar \
com.aitusoftware.flute.integration.service.DatabaseServerMain $DB_DIR
```

## Configure and start the persistor

The `Persistor` is the component responsible for receiving histogram data from clients, and storing it in the database.

Using [acceptance-test-flute.properties](https://github.com/aitusoftware/flute/blob/master/integration/src/test/resources/acceptance-test-flute.properties) 
as a guide, configure the `persistor` to use your database of choice in `$CONFIG_FILE`:
 
```
# replace with your metrics database url, or leave if using the bundled h2 database
flute.db.metrics.url=jdbc:h2:tcp://localhost:9092/flute-db
# replace 'SA' with your database $USER, or leave if using the bundled h2 database
flute.db.metrics.username=SA
# set database $PASSWORD, or leave if using the bundled h2 database
flute.db.metrics.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
flute.server.tcp.listenAddress=0.0.0.0:51000
flute.histogram.maxValue=100000
flute.histogram.significantDigits=3
```

   1. Download [flute-persistor-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-persistor-all-0.1.2.jar).
   2. Execute command:
```
java -cp flute-persistor-all-0.1.2.jar \
com.aitusoftware.flute.archive.FluteMetricsPersistorMain $CONFIG_FILE
```

## Configure and start the server

The `Server` component is responsible for serving reports generated from stored histogram data. 
Custom reports can be generated, and their meta-data will be stored in a separate database.

The `server` requires its own config; required properties are as follows:

```
# replace with your metrics database url, or leave if using the bundled h2 database
flute.db.metrics.url=jdbc:h2:tcp://localhost:9092/flute-db
# replace 'SA' with your database $USER, or leave if using the bundled h2 database
flute.db.metrics.username=SA
# set database $PASSWORD, or leave if using the bundled h2 database
flute.db.metrics.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
# replace with your reports database url, or leave if using the bundled h2 database
flute.db.reports.url=jdbc:h2:tcp://localhost:9092/flute-report-db
# replace 'SA' with your database $USER, or leave if using the bundled h2 database
flute.db.reports.username=SA
# set database $PASSWORD, or leave if using the bundled h2 database
flute.db.reports.password=
# set the driver class-name for your chosen database, ensure that it is available on the classpath
flute.db.metrics.driver.className=org.h2.Driver
flute.histogram.maxValue=100000
flute.histogram.significantDigits=3
flute.server.httpPort=15002
```

   1. Download [flute-server-all-0.1.2.jar](https://github.com/aitusoftware/flute/releases/download/v0.1.2/flute-server-all-0.1.2.jar).
   2. Clone the __Flute__ repository, or download source and unzip the source so that the server UI resources are available to the application. [See #2](https://github.com/aitusoftware/flute/issues/2).
   3. Make a note of the resource directory; relative the repository root, it will be `server/src/main/resources/ui`.
   4. E.g. `RESOURCE_DIR=server/src/main/resources/ui`
   4. Execute command:
```
java -cp flute-server-all-0.1.2.jar -Dflute.resource.base=$RESOURCE_DIR \
com.aitusoftware.flute.server.http.HttpQueryServerMain $CONFIG_FILE
```

# Generating data

Now that the server components are up and running, it's time to send some data for recording.

Just to test that everything is working ok, we are going to run a `HiccupClient`, which requires a config file
containing the following properties:

```
flute.test.acceptance.reporting.tcp.address=0.0.0.0:51000
flute.histogram.maxValue=100000
flute.histogram.significantDigits=3
```

   1. Execute command:

```
java -cp ./flute-integration-all-0.1.2.jar \
com.aitusoftware.flute.integration.client.HiccupClient $CONFIG_FILE
```

Now that we're sending some data to the back-end, it's time to 
[create a report](https://github.com/aitusoftware/flute/wiki/Reports).