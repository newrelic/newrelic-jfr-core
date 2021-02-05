package com.newrelic.jfr.daemon;

import com.newrelic.jfr.daemon.agent.FileJfrRecorder;
import com.newrelic.jfr.daemon.app.JmxJfrRecorder;
import java.nio.file.Path;

/**
 * A {@link JfrRecorder} is responsible for recording JFR data to a path when prompted via {@link
 * #recordToFile()}.
 *
 * @see FileJfrRecorder
 * @see JmxJfrRecorder
 */
public interface JfrRecorder {

  /**
   * Record JFR data and return the path its recorded to.
   *
   * @return the path JFR data has been recorded
   * @throws JfrRecorderException if a fatal error occur that prevents this recorder from
   *     functioning
   */
  Path recordToFile() throws JfrRecorderException;
}
