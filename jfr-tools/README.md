# JFR Tools

## StatsMaker
StatsMaker is a tool that reads a JFR recording file and generates statistics about the amount
of metric and event data that is sent by the JFR daemon after processing the recording.

### Build the jfr-tools jar
From the project root run:  
`./gradlew build`

The generated `jfr-tools` jar can be found at:  
`newrelic-jfr-core/jfr-tools/build/libs/jfr-tools-1.2.0-SNAPSHOT.jar`

### Usage
1. First you’ll need to generate a JFR recording file to process with the StatsMaker.
2. Set your APM license key: `export INSIGHTS_INSERT_KEY=123xyz`
3. Run the `jfr-tools` jar with the JFR recording file: `java -jar jfr-tools-1.2.0-SNAPSHOT.jar </absolute/path/to/jfr/recording/file>`

### Output
StatsMaker will generate output similar to the following indicating the amount of metric and event data sent to New Relic.

```
Metrics to be sent: 363
Events to be sent: 1455

Duration (hrs): 0.004763333333333333

Total metrics data (MB): 0.060192108154296875
Hourly metrics data (MB): 12.63655174687828
Monthly metrics data (GB): 8.885075447023791

Total events data (MB): 2.278265953063965
Hourly events data (MB): 478.2923624347022
Monthly events data (GB): 336.2993173369
```
