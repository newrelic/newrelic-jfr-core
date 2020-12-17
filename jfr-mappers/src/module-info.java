module com.newrelic.jfr {
  exports com.newrelic.jfr;
  exports com.newrelic.jfr.toevent;
  exports com.newrelic.jfr.tometric;
  exports com.newrelic.jfr.tosummary;

  requires jdk.jfr;
  requires com.google.gson;
  requires com.newrelic.telemetry;
}
