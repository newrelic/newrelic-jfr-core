package com.newrelic.jfr.daemon;

import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
import com.newrelic.jfr.daemon.app.JmxJfrRecorderFactory;

/**
 * A {@link JmxJfrRecorderFactory} is responsible for generating instances of {@link JfrRecorder}.
 *
 * @see FileJfrRecorderFactory
 * @see JmxJfrRecorderFactory
 */
public interface JfrRecorderFactory {

  /**
   * Obtain an instance of a {@link JfrRecorder}.
   *
   * @return the recorder
   * @throws Exception if a fatal error occurs which prevents creation of additional recorders
   */
  JfrRecorder getRecorder() throws Exception;
}
