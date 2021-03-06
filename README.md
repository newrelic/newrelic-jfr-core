[![Community Plus header](https://github.com/newrelic/opensource-website/raw/master/src/images/categories/Community_Plus.png)](https://opensource.newrelic.com/oss-category/#community-plus)

# JFR Core

![build badge](https://github.com/newrelic/newrelic-jfr-core/workflows/main%20build/badge.svg)

This repository contains the core New Relic JFR components. It can be used 
to acquire JFR events, transform them into New Relic telemetry, and then 
send them to New Relic 
(with the [New Relic Telemetry SDK](https://github.com/newrelic/newrelic-telemetry-sdk-java)).

This repository contains the following modules:

* [jfr-mappers](#jfr-mappers) - Mappers that transform JFR `RecordedEvent` objects into telemetry.  Also 
contains registries of all supported mappers.
* [jfr-daemon](#jfr-daemon) - An out-of-process daemon tool that uses a rotating fileset to near-continuously
send telemetry to New Relic. 

## Installation

For general usage of the produced artifacts see [JFR Mappers](#jfr-mappers) and [JFR Daemon](#jfr-daemon).

## Getting Started

To build the project see [Building](#building).

## Building

This project uses Java 11 and the gradle wrapper.  To build it, run:

```
$ git clone https://github.com/newrelic/newrelic-jfr-core.git
$ cd jfr-core
$ ./gradlew build
```

The resulting jars of interest are:
 * `jfr-mappers/build/libs/jfr-mappers-<version>.jar`
 * `jfr-daemon/build/libs/jfr-daemon-<version>.jar`

## Running tests

Unit tests are run with gradlew:

```
$ ./gradlew test
```

---

## JFR Mappers

This module is a library of reusable JFR (Java Flight Recorder) mappers 
used to transform JFR `RecordedEvent` instances into New Relic telemetry collections 
that are compatible with the 
[telemetry SDK](https://github.com/newrelic/newrelic-telemetry-sdk-java).

We don't intend this library to be used directly. Instead, leverage tools
like the [jfr-daemon](#jfr-daemon)
or [the JFR reporter extension](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-java-profiling-using-jfr-metrics) 
that are built upon this library.

### As a dependency

_Note: SNAPSHOT artifact is still preliminary._

#### maven dependency
```
<dependency>
    <groupId>com.newrelic</groupId>
    <artifactId>jfr-mappers</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### gradle dependency

```
compile group: 'com.newrelic', name: 'jfr-mappers', version: '1.0.0'
```

## JFR Daemon

This module builds a stand-alone process that consumes JFR events
from an existing java process and sends telemetry to New Relic.  This daemon
process issues commands over JMX to periodically generate a series of rolling 
JFR files.  It uses these files to build a "pseudo stream" of telemetry events.

### How To Use

After building or downloading the jfr-daemon jar, you should first export the INSIGHTS_INSERT_KEY variable
with [your insights key](https://docs.newrelic.com/docs/apis/get-started/intro-apis/types-new-relic-api-keys#event-insert-key) or [license key](https://docs.newrelic.com/docs/apis/get-started/intro-apis/new-relic-api-keys#ingest-license-key):

```
$ export INSIGHTS_INSERT_KEY=abc123youractualkeyhere
```

After that, you can run the daemon like this:

```
$ java -jar jfr-daemon-<version>.jar 
```
(where &lt;version&gt; is the actual version number).

By default, the daemon will connect JMX to `localhost` on port 1099 and send data to 
New Relic US production metric and event ingest endpoints.  If you need to change this
default behavior, the following environment variables are recognized:

| env var name          | required? | default             | description  |
|-----------------------|-----------|---------------------|--------------|
| INSIGHTS_INSERT_KEY   |     Y     |  n/a                | The New Relic [insert key](https://docs.newrelic.com/docs/apis/get-started/intro-apis/types-new-relic-api-keys#event-insert-key) or [license key](https://docs.newrelic.com/docs/apis/get-started/intro-apis/new-relic-api-keys#ingest-license-key) for your account |
| USE_LICENSE_KEY       |     N     |  false              | Use a License Key instead of Insights Insert Key.
| NEW_RELIC_APP_NAME    |     N(!)  |  My Application     | The name of the remote application being monitored.  You should probably set this so that your application shows up properly in the NR1 platform.
| REMOTE_JMX_HOST       |     N     |  localhost          | The host to pull JFR data from via JMX        |
| REMOTE_JMX_PORT       |     N     |  1099               | The port to pull JFR data from via JMX        |
| METRICS_INGEST_URI    |     N     |  [US production](https://metric-api.newrelic.com/metric/v1), [EU production](https://metric-api.eu.newrelic.com/metric/v1)          | Where to send metric data
| EVENTS_INGEST_URI     |     N     |  [US production](https://insights-collector.newrelic.com/v1/accounts/events), [EU production](https://insights-collector.eu01.nr-data.net/v1/accounts/events) | Where to send event data
| JFR_SHARED_FILESYSTEM |     N     |  false              | Use a shared filesystem instead of streaming data from JMX
| AUDIT_LOGGING         |     N     |  false              | [Enables audit logging](https://github.com/newrelic/newrelic-telemetry-sdk-java#enabling-audit-logging) in the underlying Telemetry SDK

Expose remote JMX on the application that the jfr-daemon will be attaching to by adding the following system properties:

```
-Dcom.sun.management.jmxremote 
-Dcom.sun.management.jmxremote.port=1099 
-Dcom.sun.management.jmxremote.ssl=false 
-Dcom.sun.management.jmxremote.authenticate=false
```
### Logging

The JFR daemon logs with the Slf4j-Simple implementation at the default `Info` level. For audit logging from the underlying Telemetry SDK, set the log level to `Debug` and enable audit logging via environment variable as described above. 

### Proxy

Currently, the JFR daemon can not be directly configured to accept a proxy. Instead, please use system properties `-Dhttps.proxyHost` and `-Dhttps.proxyPort` [(reference)](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html) to specify your proxy.

---
## Support

Should you need assistance with New Relic products, you are in good hands with several support channels.

**Support Channels**
>
* [New Relic Documentation](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-java-profiling-using-jfr-metrics): Comprehensive guidance for using our platform
* [New Relic Community](https://discuss.newrelic.com/t/product-announcement-real-time-java-profiling/97199): The best place to engage in troubleshooting questions
* [New Relic Developer](https://developer.newrelic.com/): Resources for building a custom observability applications
* [New Relic University](https://learn.newrelic.com/): A range of online training for New Relic users of every level

## Privacy
At New Relic we take your privacy and the security of your information seriously, and are committed to protecting your information. We must emphasize the importance of not sharing personal data in public forums, and ask all users to scrub logs and diagnostic information for sensitive information, whether personal, proprietary, or otherwise.

We define “Personal Data” as any information relating to an identified or identifiable individual, including, for example, your name, phone number, post code or zip code, Device ID, IP address, and email address.

For more information, review [New Relic’s General Data Privacy Notice](https://newrelic.com/termsandconditions/privacy).

## Contribute
We encourage your contributions to improve [project name]! Keep in mind that when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.

If you have any questions, or to execute our corporate CLA (which is required if your contribution is on behalf of a company), drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

If you would like to contribute to this project, review [these guidelines](./CONTRIBUTING.md).

To [all contributors](https://github.com/newrelic/newrelic-jfr-core/graphs/contributors), we thank you!  Without your contribution, this project would not be what it is today.  

## License
`jfr-core` is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

`jfr-core` also uses source code from [third party libraries](THIRD_PARTY_NOTICES.md). Full details on which libraries are used and the terms under which they are licensed can be found in the third party notices document.
