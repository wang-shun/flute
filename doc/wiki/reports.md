__Flute__ comes with a (very) rudimentary reporting interface.

Here we will walk through creating a report that will serve data from two sources.

The sources in question are instances of [`HiccupClient`](https://github.com/aitusoftware/flute/blob/master/integration/src/main/java/com/aitusoftware/flute/integration/client/HiccupClient.java),
whose purpose is to measure in-JVM stalls. A `HiccupClient` will attempt to _sleep_ for
one millisecond, and record the time in microseconds taken to return from the _sleep_ call.

In order to simulate some pauses that might be experienced by a real-life application, `HiccupClient` will generate lots of
garbage to try to force the garbage collector to run.

See [Getting Started](https://github.com/aitusoftware/flute/wiki/GettingStarted#generating-data) for information on how to start 
the `HiccupClient`.

In this way, we can build a picture of the magnitude and frequency of the pauses suffered by our applications.

# Creating a report

If you are running the __Flute__ server locally (see [Getting Started](https://github.com/aitusoftware/flute/wiki/GettingStarted)), 
the reports interface can be found at the following address:

[`http://localhost:15002/flute/resources/html/reports.html`](http://localhost:15002/flute/resources/html/reports.html)

Loading the page for the first time will present a form that allows the user to create a report:

![Create Report dialog](https://github.com/aitusoftware/flute/raw/master/doc/img/wiki/create_report.png "Create Report dialog")

First up, we need to choose a name for our report, and to provide a regular expression to select some metrics from the __Flute__ database:

![Choosing a name](https://github.com/aitusoftware/flute/raw/master/doc/img/wiki/create_report_name_metric.png "Choosing a name")

As you type into the `Metric pattern` box, matching metric names will be displayed below.

Next we will need to choose some time windows to report over, and define what our SLA percentiles are going to be:

![Setting details](https://github.com/aitusoftware/flute/raw/master/doc/img/wiki/create_report_time_windows.png "Setting details")

After clicking the `Create` button, you will be redirected to the list of current reports:

![Current reports](https://github.com/aitusoftware/flute/raw/master/doc/img/wiki/list_reports.png "Current reports")

Click on the `view` control in order to view the report content:

![View report](https://github.com/aitusoftware/flute/raw/master/doc/img/wiki/view_report.png "View report")