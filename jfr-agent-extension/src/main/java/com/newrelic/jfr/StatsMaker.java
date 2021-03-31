package com.newrelic.jfr;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import jdk.jfr.consumer.RecordedEvent;

public class StatsMaker implements TelemetrySender {

  private final MetricBatchMarshaller metricMarshaller =
      new MetricBatchMarshaller(
          new MetricBatchJsonCommonBlockWriter(new AttributesJson()),
          new MetricBatchJsonTelemetryBlockWriter(new MetricToJson()));

  private final EventBatchMarshaller eventMarshaller = new EventBatchMarshaller();
  private String metricJson;
  private String eventJson;

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
    uploader.readyToSend(new EventConverter(SetupUtils.buildCommonAttributes()));

    uploader.handleFile(fileName);
    outputStats();
  }

  private void outputStats() {
    double metricSize = (double) metricJson.length() / (1024 * 1024);
    System.out.println("Total metrics data (MB): " + metricSize);

    double eventSize = (double) eventJson.length() / (1024 * 1024);
    System.out.println("Total events data (MB): " + eventSize);
  }

  @Override
  public void sendBatch(MetricBatch batch) {
    System.out.println("Metrics to be sent: " + batch.size());
    metricJson = metricMarshaller.toJson(batch);
  }

  @Override
  public void sendBatch(EventBatch batch) {
    System.out.println("Events to be sent: " + batch.size());
    eventJson = eventMarshaller.toJson(batch);
  }
}
