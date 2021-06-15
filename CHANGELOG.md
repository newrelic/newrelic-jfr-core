# Changelog
This file documents noteworthy changes to the `jfr-core` projects.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Coming soon
* tbd

## Version 1.3.0 (2021-06-01)
* The JFR daemon aggregates JfrMethodSample events into JfrFlamelevel events. It does not report JfrMethodSample events anymore and does report JfrFlamelevel events. [#196](https://github.com/newrelic/newrelic-jfr-core/pull/196)
* Refactor jfr-agent-extension module into jfr-tools [#190](https://github.com/newrelic/newrelic-jfr-core/pull/190)
* Refactor newrelic-api to compileOnly dependency [#189](https://github.com/newrelic/newrelic-jfr-core/pull/189)

## Version 1.2.0 (2021-05-03)
* The JFR daemon has been refactored to better support running with the New Relic Java agent. [#178](https://github.com/newrelic/newrelic-jfr-core/pull/178)
* README improvements [#176](https://github.com/newrelic/newrelic-jfr-core/pull/176), [#178](https://github.com/newrelic/newrelic-jfr-core/pull/178), [#180](https://github.com/newrelic/newrelic-jfr-core/pull/180)
* Add new config options to support HTTP proxy configuration. Note: [HTTPS proxy is not currently supported by OkHttp](https://github.com/square/okhttp/issues/6561). [#178](https://github.com/newrelic/newrelic-jfr-core/pull/178)
* Added validation to guard against illegal access to `RecordedObject` fields that may not exist. [#186](https://github.com/newrelic/newrelic-jfr-core/pull/186)

## Version 1.1.0 (2021-03-12)
* The JFR daemon has been back-ported to run with Java 8 (`version 8u262+`) or higher. [#143](https://github.com/newrelic/newrelic-jfr-core/pull/143)
* Update telemetry sdk to `0.12.0`.
* Generate two new `jdk.GarbageCollection` metrics. [#169](https://github.com/newrelic/newrelic-jfr-core/pull/169): 
    * `jfr.GarbageCollection.minorDuration`
    * `jfr.GarbageCollection.majorDuration`
* Add support for general heap summary. [#106](https://github.com/newrelic/newrelic-jfr-core/pull/106)
* Documentation updates:
    * Add EU endpoints. [#109](https://github.com/newrelic/newrelic-jfr-core/pull/109)
    * Add audit logging config. [#131](https://github.com/newrelic/newrelic-jfr-core/pull/131)
    * Add proxy config. [#163](https://github.com/newrelic/newrelic-jfr-core/pull/163)
* Add support for generating jlink binaries. [#128](https://github.com/newrelic/newrelic-jfr-core/pull/128)
* Add support for Valhalla VBC events [#115](https://github.com/newrelic/newrelic-jfr-core/pull/115)
* Change the default app name to `My Application`. [#125](https://github.com/newrelic/newrelic-jfr-core/pull/125)
* Address `ConnectIOException` that could cause the JFR daemon to crash. [#127](https://github.com/newrelic/newrelic-jfr-core/pull/127)
* Add support for APM license keys. [#163](https://github.com/newrelic/newrelic-jfr-core/pull/163)
* Guard against potential null values from `RecordedEvent`s. [#126](https://github.com/newrelic/newrelic-jfr-core/pull/126)

## Version 1.0.0 (2020-11-03)
* **Initial 1.0.0 GA release**
* `jfr-mappers` module [no longer depends on `telemetry-java-http11`](https://github.com/newrelic/newrelic-jfr-core/pull/90).
* [Fix for a potential freeze](https://github.com/newrelic/newrelic-jfr-core/pull/97) when the number of events in a 
single JFR recording file exceeds the maximum queue size.
* [Clean up temp dirs](https://github.com/newrelic/newrelic-jfr-core/pull/96) to prevent resource leak.
* [Use camel case](https://github.com/newrelic/newrelic-jfr-core/pull/100) for event/metric names instead of inconsistent delimiter.
* [Clear per-thread cache data](https://github.com/newrelic/newrelic-jfr-core/pull/101) between harvest cycles to prevent monotonic growth.
* [Omit the service name](https://github.com/newrelic/newrelic-jfr-core/pull/102) when the java agent is also running on the target process (favor using entity guid instead).

## Version 0.5.0 (2020-09-30)
* Update telemetry sdk to 0.8.0.
* Minor documentation/readme [change](https://github.com/newrelic/newrelic-jfr-core/pull/83).
* Small [change](https://github.com/newrelic/newrelic-jfr-core/pull/86) to how the JFR profile is configured.

## Version 0.4.0 (2020-09-22)

* JFR Daemon - If the agent is present, no longer wait for the agent to connect to obtain an entity guid.
Instead, buffer the data and wait to send it until the entity guid is available.
* JFR Daemon - Omit the app name from the common attributes when the entity guid is present

## Version 0.3.0 (2020-07-31)

* JFR Daemon - change main class from `JFRController` to `JFRDaemon`
* JFR Daemon - now supports fetching `entity.guid` common attribute from remote MBean
* JRR Daemon - startup delays until service startup is complete
* JFR Daemon - make determining localhost address more robust
* JFR Daemon - retry with backoff when recordings fail
* JFR Mappers - null guard against null thread in `ThreadAllocationStatistics` events
* JFR Mappers - fix stack overflow boundary case when truncating stack frames