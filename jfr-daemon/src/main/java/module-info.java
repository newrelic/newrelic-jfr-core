module com.newrelic.jfr.daemon {
  exports com.newrelic.jfr.daemon;

  requires jdk.jfr;
  requires java.net.http;
  requires java.management;
  requires org.slf4j;
  requires com.newrelic.telemetry;
  requires com.newrelic.jfr;
}
