package com.newrelic.jfr.tools;
// FIXME Just in this package for now...

import com.newrelic.jfr.daemon.*;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.json.EventBatchMarshaller;
import com.newrelic.telemetry.json.AttributesJson;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.json.MetricBatchJsonCommonBlockWriter;
import com.newrelic.telemetry.metrics.json.MetricBatchJsonTelemetryBlockWriter;
import com.newrelic.telemetry.metrics.json.MetricBatchMarshaller;
import com.newrelic.telemetry.metrics.json.MetricToJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jdk.jfr.consumer.RecordedEvent;

/**
 * StatsMaker is a tool that reads a JFR recording file and generates statistics about the amount of
 * metric and event data that is sent by the JFR daemon after processing the recording.
 */
public class StatsMaker implements TelemetrySender {

  private final MetricBatchMarshaller metricMarshaller =
      new MetricBatchMarshaller(
          new MetricBatchJsonCommonBlockWriter(new AttributesJson()),
          new MetricBatchJsonTelemetryBlockWriter(new MetricToJson()));

  private final EventBatchMarshaller eventMarshaller = new EventBatchMarshaller();
  private String metricJson;
  private String eventJson;

  /**
   * StatsMaker entry point.
   *
   * @param args absolute path to the JFR recording file to be analyzed
   */
  public static void main(String[] args) {
    Path fileName = null;
    try {
      fileName = getPathToTmpFile(args[0]);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    StatsMaker maker = new StatsMaker();
    maker.run(fileName);
  }

  static Path getPathToTmpFile(final String jfrName) throws IOException {
    Path dir = Files.createTempDirectory("nr-jfr");
    Path file = Files.createTempFile(dir, "stream-" + System.currentTimeMillis(), null);
    byte[] jfrBytes = Files.readAllBytes(Paths.get(jfrName));
    Files.write(file, jfrBytes);
    return file;
  }

  private void run(Path fileName) {
    BlockingQueue<RecordedEvent> queue = new LinkedBlockingQueue<>(250_000);
    RecordedEventBuffer recordedEventBuffer = new RecordedEventBuffer(queue);
    JFRUploader uploader = new JFRUploader(this, recordedEventBuffer);
    DaemonConfig config = SetupUtils.buildConfig();
    uploader.readyToSend(new EventConverter(SetupUtils.buildCommonAttributes(config)));

    uploader.handleFile(fileName);
    long lengthMillis = Duration.between(uploader.fileStart(), uploader.fileEnd()).toMillis();
    double lengthHours = (double) lengthMillis / 3600_000;
    System.out.println();
    System.out.println("Duration (hrs): " + lengthHours);
    System.out.println();

    double metricSize = (double) metricJson.length() / (1024 * 1024);
    System.out.println("Total metrics data (MB): " + metricSize);
    System.out.println("Hourly metrics data (MB): " + metricSize / lengthHours);
    System.out.println("Monthly metrics data (GB): " + 30 * 24 * metricSize / (lengthHours * 1024));
    System.out.println();

    double eventSize = (double) eventJson.length() / (1024 * 1024);
    System.out.println("Total events data (MB): " + eventSize);
    System.out.println("Hourly events data (MB): " + eventSize / lengthHours);
    System.out.println("Monthly events data (GB): " + 30 * 24 * eventSize / (lengthHours * 1024));
  }

  /**
   * Overrides TelemetrySender#sendBatch
   *
   * @param batch MetricBatch to send
   */
  @Override
  public void sendBatch(MetricBatch batch) {
    System.out.println("Metrics to be sent: " + batch.size());
    metricJson = metricMarshaller.toJson(batch);
  }

  /**
   * Overrides TelemetrySender#sendBatch
   *
   * @param batch EventBatch to send
   */
  @Override
  public void sendBatch(EventBatch batch) {
    System.out.println("Events to be sent: " + batch.size());
    eventJson = eventMarshaller.toJson(batch);
  }
}
