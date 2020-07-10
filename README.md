[![Community Project header](https://github.com/newrelic/open-source-office/raw/master/examples/categories/images/Community_Project.png)](https://github.com/newrelic/open-source-office/blob/master/examples/categories/index.md#community-project)

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
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

#### gradle dependency

```
compile group: 'com.newrelic', name: 'jfr-mappers', version: '0.2.0-SNAPSHOT'
```

## JFR Daemon

This module builds a stand-alone process that consumes JFR events
from an existing java process and sends telemetry to New Relic.  This daemon
process issues commands over JMX to periodically generate a series of rolling 
JFR files.  It uses these files to build a "pseudo stream" of telemetry events.

### How To Use

TBD.  This is a work in progress.

---
## Support

New Relic hosts and moderates an online forum where customers can interact with New Relic employees as well as other customers to get help and share best practices. Like all official New Relic open source projects, there's a related Community topic in the New Relic Explorers Hub.

## Contributing
We encourage your contributions to improve `jfr-core`! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.
To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com.

## License
`jfr-core` is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

`jfr-core` also uses source code from [third party libraries](THIRD_PARTY_NOTICES.md). Full details on which libraries are used and the terms under which they are licensed can be found in the third party notices document.
