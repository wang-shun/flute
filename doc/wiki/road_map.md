# Release 1.0.0

The following features need to be implemented before __Flute__ can be used for long-term use:

### Client module

   1. Clean-up of resources when a `TimeTracker` is no longer used ([#5](https://github.com/aitusoftware/flute/issues/5)).
   2. Ability to recover from sender overflow when unable to connect to the persistor ([#4](https://github.com/aitusoftware/flute/issues/4)).

### Agent module

   1. Configurable metric names when annotating methods ([#3](https://github.com/aitusoftware/flute/issues/3)).

### Persistor module

   1. Roll-up of metric data to reduce overhead of calculating historic data ([#1](https://github.com/aitusoftware/flute/issues/1)).
   
### Server module

   1. UI resources should be loaded from the classpath if no local path is specified ([#2](https://github.com/aitusoftware/flute/issues/2)).