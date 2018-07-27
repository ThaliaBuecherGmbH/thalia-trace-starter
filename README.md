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

Our starter provides tracing and monitoring on a per request basis. By default performance
metrics are gathered for any incoming request. The application only exposes those metrics as 
part of its reponse *if the caller demands it*. Metrics are therefore not written to a central data store, 
but provided to the caller, who can itself propagate the metrics to its caller and so on. As the final result, 
the initial entry point of the invocation chain receives performance metrics of the whole chain!
This simple mechanism allows us to

* Collect metrics transparently of complex invocation chains
* Keep infrastrucure costs low. We do not need a big central data store
* Reduce or eliminate security concerns
* Support our development teams to keep an eye on application and system performance

## How

Internally metrics are gathered by instrumenting Spring RestTemplate or DataSources. 
These instrumentation writes invocation statistics as spans into a HTTP request specific repository
This repository is managed by a ServletFilter, which is weaved around the whole application. This
filter toggles monitoring feature state and exposes the gathered spans to the caller if he demands it.

The whole instrumentation part is handled by a Spring Boot starter. We follow a zero
configuration approach. 

## Minimum requirements

* Java 1.8
* Spring Boot 2

## Available instrumentations

* HTTP (Spring MVC, Spring Boot Actuator, Servlets)
* Spring managed RestTemplateBuilder
* Spring managed JDBC DataSources
* Hystrix commands

## Examples

## Configuration and extension points

