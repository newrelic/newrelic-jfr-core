package com.newrelic.jfr.daemon;

/** Signals that an error has occurred recording Java Flight Recorder data. */
public class JfrRecorderException extends Exception {

  public JfrRecorderException(String message) {
    super(message);
  }

  public JfrRecorderException(String message, Exception cause) {
    super(message, cause);
  }
}
