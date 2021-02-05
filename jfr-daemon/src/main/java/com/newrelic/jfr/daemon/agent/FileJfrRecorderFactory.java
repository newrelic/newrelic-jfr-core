package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.JfrRecorder;
import com.newrelic.jfr.daemon.JfrRecorderFactory;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

public class FileJfrRecorderFactory implements JfrRecorderFactory {

  private final Duration harvestInterval;

  public FileJfrRecorderFactory(Duration harvestInterval) {
    this.harvestInterval = harvestInterval;
  }

  @Override
  public JfrRecorder getRecorder() throws Exception {
    Configuration jfrConfig;
    try {
      jfrConfig = Configuration.getConfiguration("profile");
    } catch (IOException | ParseException e) {
      // This should never happen
      throw new Exception("An error occurred getting configuration.", e);
    }
    Recording recording = new Recording(jfrConfig);
    recording.setMaxAge(harvestInterval.plus(10, ChronoUnit.SECONDS));
    recording.setToDisk(true);
    recording.setName("New Relic JFR Agent Recording");
    recording.start();

    return new FileJfrRecorder(recording);
  }
}
