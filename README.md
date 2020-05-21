[![Community Project header](https://github.com/newrelic/open-source-office/raw/master/examples/categories/images/Community_Project.png)](https://github.com/newrelic/open-source-office/blob/master/examples/categories/index.md#community-project)

# JFR Mappers

This repository is a library of reusable JFR (Java Flight Recorder) mappers, 
used to transform JFR `RecordedEvent` instances into collections 
of New Relic telemetry, compatible with the 
[telemetry SDK](https://github.com/newrelic/newrelic-telemetry-sdk-java).

In general, users will not use this library directly, but will instead leverage
higher-level tooling built upon this library, such as
[the JFR reporter extension](https://docs.newrelic.com/docs/agents/java-agent/features/real-time-java-profiling-using-jfr-metrics).

## Building

This library uses the gradle wrapper.  To build:

```
$ git clone https://github.com/newrelic/newrelic-jfr-mappers.git
$ cd jfr-mappers
$ ./gradlew build
```

The resulting library (jar) will be in `jfr-mappers/build/libs/`.

## Running tests

Unit tests are run with gradlew:

```
$ ./gradlew test
```

## As a dependency

PRELIMINARY - ARTIFACTS ARE NOT YET BEING PUBLISHED

### maven dependency
```
<dependency>
    <groupId>com.newrelic</groupId>
    <artifactId>jfr-mappers</artifactId>
    <version>0.0.1</version>
</dependency>
```

### gradle dependency

```
compile group: 'com.newrelic', name: 'jfr-mappers', version: '0.0.1'
```

## Support

New Relic hosts and moderates an online forum where customers can interact with New Relic employees as well as other customers to get help and share best practices. Like all official New Relic open source projects, there's a related Community topic in the New Relic Explorers Hub. You can find this project's topic/threads here:

>Add the url for the support thread here

## Contributing
Full details about how to contribute to
Contributions to improve `jfr-mappers` are encouraged! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.
To execute our corporate CLA, which is required if your contribution is on behalf of a company, or if you have any questions, please drop us an email at open-source@newrelic.com.

## License
`jfr-mappers` is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

`jfr-mappers` also uses source code from [third party libraries](THIRD_PARTY_NOTICES.md). Full details on which libraries are used and the terms 
under which they are licensed can be found in the third party notices document.
