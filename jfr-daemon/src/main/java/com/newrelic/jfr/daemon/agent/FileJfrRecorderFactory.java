package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.JfrRecorder;
import com.newrelic.jfr.daemon.JfrRecorderException;
import com.newrelic.jfr.daemon.JfrRecorderFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import static java.util.Collections.*;

public class FileJfrRecorderFactory implements JfrRecorderFactory {

  private final Duration harvestInterval;
  private final List<String> enabledJfrEvents;

  public FileJfrRecorderFactory(Duration harvestInterval) {
    this(harvestInterval, EMPTY_LIST);
  }

  public FileJfrRecorderFactory(Duration harvestInterval, List<String> enabledJfrEvents) {
    this.harvestInterval = harvestInterval;
    this.enabledJfrEvents = enabledJfrEvents;
  }

  @Override
  public JfrRecorder getRecorder() throws JfrRecorderException {
    Configuration jfrConfig;
    try {
      Reader reader =
          new InputStreamReader(
              FileJfrRecorderFactory.class
                  .getClassLoader()
                  .getResourceAsStream("newrelic_jfr_profile.jfc"));
      jfrConfig = Configuration.create(reader);
    } catch (IOException | ParseException e) {
      // This should never happen
      throw new JfrRecorderException("An error occurred getting configuration.", e);
    }
    Recording recording = new Recording(jfrConfig);
    enabledJfrEvents.forEach(recording::enable);
    recording.setMaxAge(harvestInterval.plus(10, ChronoUnit.SECONDS));
    recording.setToDisk(true);
    recording.setName("New Relic JFR Agent Recording");
    recording.start();

    return new FileJfrRecorder(recording);
  }
}
