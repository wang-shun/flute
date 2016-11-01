__Flute__ comes with a (very) rudimentary reporting interface.

Here we will walk through creating a report that will serve data from two sources.

The sources in question are instances of [`HiccupClient`](https://github.com/aitusoftware/flute/blob/master/integration/src/main/java/com/aitusoftware/flute/integration/client/HiccupClient.java),
whose purpose is to measure in-JVM stalls. A `HiccupClient` will attempt to _sleep_ for
one millisecond, and record the time in nanoseconds taken to return from the _sleep_ call.

In this way, we can build a picture of the magnitude and frequency of the pauses suffered by our applications.

## Creating a report

