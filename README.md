# Thalia Spring Boot starter for call tracing

## Why

Our services live in an interconnected and distributed world. Services take requests, have to talk to other services
and compute results based on gathered data. This distributed nature makes it hard to track application performance and
find potential performance bottlenecks. 

There are some very famous tracing solutions (e.g. Zipkin) available. Most of them gather invocation traces in some way
and write them back to a central data store. Traces can then be analyzed. This concept introduces some drawbacks:

* The central data store tend to become *really huge* on high traffic sites
* Tracing is always enabled. We gather a lot of data, but most of it isn't touched again
* Infrastructure costs can explode
* We introduce a possible (security) singularity in form of the central data store

And here comes our starter into play!

## How

## Examples

## Configuration

