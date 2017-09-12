# Usage lifecycle

The easiest way to use flute is to use the `@FluteMetric`
annotation on a method to be instrumented, and start your 
JVM using the flute agent.

It may be preferable to manually record event latencies, in which
case this can be achieved using a `TimeTracker`.

## Creating a TimeTracker

First, configure a `TimeTracker` factory:

```java
final String fluteServerHost = "10.0.2.123";
final int fluteServerPort = 17888;
final RecordingTimeTrackerFactory factory = 
    new RecordingTimeTrackerFactory().
        publishingTo(new InetSocketAddress(fluteServerHost, fluteServerPort)).
        withSenderEvents(new LoggingSenderEvents()).
        withValidation(true).
        publishingEvery(1, TimeUnit.SECONDS).
        withHistogramConfig(new HistogramConfig(MAX_VALUE, 2));

```

then use the factory to create instances of `TimeTracker`:

```java
final TimeTracker placeOrderTracker = 
    factory.withIdentifier("order.place").create();

final TimeTracker cancelOrderTracker = 
    factory.withIdentifier("order.cancel").create();
```

events can then be recorded:

```java
placeOrderTracker.begin(System.nanoTime());
try {
    doPlaceOrderBusinessOperation();
} finally {
    placeOrderTracker.end(System.nanoTime());
}
```

## Cleaning up

To shutdown time tracker connections to the flute server, 
invoke the `shutdown` method of `RecordingTimeTrackerFactory`:

```java
factory.shutdown();
```
