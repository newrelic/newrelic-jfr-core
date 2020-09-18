/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.EnvironmentVars.ENV_APP_NAME;
import static com.newrelic.jfr.daemon.EnvironmentVars.EVENTS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;
import static com.newrelic.jfr.daemon.EnvironmentVars.JFR_SHARED_FILESYSTEM;
import static com.newrelic.jfr.daemon.EnvironmentVars.METRICS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_HOST;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_PORT;
import static java.util.function.Function.identity;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.lifecycle.MBeanServerConnector;
import com.newrelic.jfr.daemon.lifecycle.RemoteEntityGuidCheck;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.telemetry.Attributes;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    try {
      var config = buildConfig();
      var mBeanServerConnection = new MBeanServerConnector(config).getConnection();
      var readinessCheck = new AtomicBoolean(false);
      var eventConverterReference = new AtomicReference<EventConverter>();

      RemoteEntityGuidCheck.builder()
              .mbeanServerConnection(mBeanServerConnection)
              .onComplete(guid -> {
                var attr = new JFRCommonAttributes(config).build(guid);
                var eventConverter = buildEventConverter(attr);
                eventConverterReference.set(eventConverter);
                readinessCheck.set(true);
              })
              .build()
              .start();
      readinessCheck.set(true);

      var uploader = buildUploader(config, readinessCheck, eventConverterReference);
      var jfrController = new JFRController(uploader, config);
      jfrController.setup();
      jfrController.loop(config.getHarvestInterval());
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      throw new RuntimeException(e);
    }
  }

  private static DaemonConfig buildConfig() {

    var daemonVersion = new VersionFinder().get();

    var builder =
        DaemonConfig.builder().apiKey(System.getenv(INSERT_API_KEY)).daemonVersion(daemonVersion);

    builder.maybeEnv(ENV_APP_NAME, identity(), builder::monitoredAppName);
    builder.maybeEnv(REMOTE_JMX_HOST, identity(), builder::jmxHost);
    builder.maybeEnv(REMOTE_JMX_PORT, Integer::parseInt, builder::jmxPort);
    builder.maybeEnv(METRICS_INGEST_URI, URI::create, builder::metricsUri);
    builder.maybeEnv(EVENTS_INGEST_URI, URI::create, builder::eventsUri);
    builder.maybeEnv(JFR_SHARED_FILESYSTEM, Boolean::parseBoolean, builder::useSharedFilesystem);

    return builder.build();
  }

  static JFRUploader buildUploader(
          DaemonConfig config, AtomicBoolean readinessCheck, AtomicReference<EventConverter> eventConverterReference)
      throws MalformedURLException {
    var telemetryClient = new TelemetryClientFactory().build(config);
    var queue = new LinkedBlockingQueue<RecordedEvent>(50000);
    var recordedEventBuffer = new RecordedEventBuffer(queue);
    return JFRUploader.builder()
        .telemetryClient(telemetryClient)
        .recordedEventBuffer(recordedEventBuffer)
        .eventConverter(eventConverterReference)
        .readinessCheck(readinessCheck)
        .build();
  }

  private static EventConverter buildEventConverter(Attributes attr) {
    return EventConverter.builder()
        .commonAttributes(attr)
        .metricMappers(ToMetricRegistry.createDefault())
        .eventMapper(ToEventRegistry.createDefault())
        .summaryMappers(ToSummaryRegistry.createDefault())
        .build();
  }
}
