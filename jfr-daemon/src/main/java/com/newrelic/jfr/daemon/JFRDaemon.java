/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.EnvironmentVars.*;
import static java.util.function.Function.identity;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.lifecycle.MBeanServerConnector;
import com.newrelic.jfr.daemon.lifecycle.RemoteEntityGuidCheck;
import com.newrelic.telemetry.Attributes;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
          .onComplete(
              guid -> {
                var attr = new JFRCommonAttributes(config).build(guid);
                var eventConverter = buildEventConverter(attr);
                eventConverterReference.set(eventConverter);
                readinessCheck.set(true);
              })
          .build()
          .start();

      var uploader = buildUploader(config, readinessCheck, eventConverterReference);
      var jfrController = new JFRController(uploader, config);
      jfrController.setup();
      jfrController.loop(config.getHarvestInterval());
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      throw new RuntimeException(e);
    }
  }

  public static DaemonConfig buildConfigFromArgs(String agentArgs) {
    var daemonVersion = new VersionFinder().get();
    var mandatoryArgs = agentArgs.split("\\|");
    if (mandatoryArgs.length < 2) {
      throw new RuntimeException("Agent startup needs at least an API key and an app name");
    }

    var builder = DaemonConfig.builder().apiKey(mandatoryArgs[0]).daemonVersion(daemonVersion);
    builder.monitoredAppName(mandatoryArgs[1]);
    try {
      if (mandatoryArgs.length > 2) {
        builder.metricsUri(new URI(mandatoryArgs[2]));
      }
      if (mandatoryArgs.length > 3) {
        builder.eventsUri(new URI(mandatoryArgs[3]));
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return builder.build();
  }

  public static DaemonConfig buildConfig() {

    var daemonVersion = new VersionFinder().get();

    var builder =
        DaemonConfig.builder().apiKey(System.getenv(INSERT_API_KEY)).daemonVersion(daemonVersion);

    builder.maybeEnv(ENV_APP_NAME, identity(), builder::monitoredAppName);
    builder.maybeEnv(REMOTE_JMX_HOST, identity(), builder::jmxHost);
    builder.maybeEnv(REMOTE_JMX_PORT, Integer::parseInt, builder::jmxPort);
    builder.maybeEnv(METRICS_INGEST_URI, URI::create, builder::metricsUri);
    builder.maybeEnv(EVENTS_INGEST_URI, URI::create, builder::eventsUri);
    builder.maybeEnv(JFR_SHARED_FILESYSTEM, Boolean::parseBoolean, builder::useSharedFilesystem);
    builder.maybeEnv(AUDIT_LOGGING, Boolean::parseBoolean, builder::auditLogging);

    return builder.build();
  }

  public static JFRUploader buildUploader(
      DaemonConfig config,
      AtomicBoolean readinessCheck,
      AtomicReference<EventConverter> eventConverterReference)
      throws MalformedURLException {
    var telemetryClient = new TelemetryClientFactory().build(config);
    var queue = new LinkedBlockingQueue<RecordedEvent>(250_000);
    var recordedEventBuffer = new RecordedEventBuffer(queue);
    return JFRUploader.builder()
        .telemetryClient(telemetryClient)
        .recordedEventBuffer(recordedEventBuffer)
        .eventConverter(eventConverterReference)
        .readinessCheck(readinessCheck)
        .build();
  }

  public static EventConverter buildEventConverter(Attributes attr) {
    return EventConverter.builder()
        .commonAttributes(attr)
        .metricMappers(ToMetricRegistry.createDefault())
        .eventMapper(ToEventRegistry.createDefault())
        .summaryMappers(ToSummaryRegistry.createDefault())
        .build();
  }
}
