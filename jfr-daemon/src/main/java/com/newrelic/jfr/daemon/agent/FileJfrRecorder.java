package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.JfrRecorder;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.Recording;

public class FileJfrRecorder implements JfrRecorder {

  private final Recording recording;

  public FileJfrRecorder(Recording recording) {
    this.recording = recording;
  }

  @Override
  public Path recordToFile() throws Exception {
    var output = Files.createTempFile("local-recording", ".jfr");
    recording.copy(false);
    recording.dump(output);
    return output;
  }
}
