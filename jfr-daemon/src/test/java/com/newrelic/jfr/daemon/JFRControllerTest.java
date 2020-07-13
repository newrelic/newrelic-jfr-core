package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.JFRController.buildUploader;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JFRControllerTest {

  private static final int DUMMY_PID = 12345;
  private static final int DUMMY_PORT = 9019;

  @Test
  @Disabled
  public void test_simple_stream() throws Exception {
    DaemonConfig config = DaemonConfig.builder().build();
    var processor = buildUploader(config);
    var p = Path.of("src/test/resources/stream-2020-06-10-2.jfr");
    //    processor.handleFile(p);
    assertTrue(true);
  }
}
