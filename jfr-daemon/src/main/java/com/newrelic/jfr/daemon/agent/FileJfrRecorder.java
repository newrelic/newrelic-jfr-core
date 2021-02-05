package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.JfrRecorder;
import com.newrelic.jfr.daemon.JfrRecorderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.Recording;

public class FileJfrRecorder implements JfrRecorder {

  private final Recording recording;

  public FileJfrRecorder(Recording recording) {
    this.recording = recording;
  }

  @Override
  public Path recordToFile() throws JfrRecorderException {
    try {
      Path output = Files.createTempFile("local-recording", ".jfr");
      recording.copy(false);
      recording.dump(output);
      return output;
    } catch (IOException e) {
      throw new JfrRecorderException("Failed recording JFR to temp file.", e);
    }
  }
}
