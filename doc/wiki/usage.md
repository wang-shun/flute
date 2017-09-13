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

### Cleaning up

To shutdown time tracker connections to the flute server, 
invoke the `shutdown` method of `RecordingTimeTrackerFactory`:

```java
factory.shutdown();
```

## Using the flute agent

`flute` is bundled with a Java agent (compatible with JDK1.6+) that can be used
to instrument existing code using annotations.

Consider the following class:

```java
final class OrderManager {
    
    void createOrder(OrderDetails details);
    void amendOrder(OrderDetails details);
    void cancelOrder(OrderId id);
}
```

To accurately measure the time taken to execute each of the methods, the
`@FluteMetric` annotation can be used to perform bytecode instrumentation
at class-loading time:

```java
final class OrderManager {
   
    @FluteMetric
    void createOrder(OrderDetails details);
    @FluteMetric
    void amendOrder(OrderDetails details);
    @FluteMetric
    void cancelOrder(OrderId id);
}
```

When starting the process, simply add the `-javaagent` parameter to the command line:

```
java -javaagent:/path/to/flute-agent/flute-agent-all-version.jar=/path/to/config.properties ...
```

### Metric name replacement

By default, `flute` will use the name of an annotated method as the metric name. In the 
example above, the metrics would be 'createOrder', 'amendOrder', and 'cancelOrder'.

A custom metric name can be specified on the annotation, with the ability to 
perform system property replacements at runtime.

To include the hostname in a metric for example, add the following to the annotation:

```java
final class OrderManager {
   
    @FluteMetric(metricName = "system.${app.hostname}.orderManager.createOrder")
    void createOrder(OrderDetails details);
```

and ensure that the `app.hostname` system property is correctly set (i.e. via a `-D` parameter).

This will cause the metrics from this invocation will be reported with the 
name `system.${app.hostname}.orderManager.createOrder`.