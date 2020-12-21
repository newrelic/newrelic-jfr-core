module com.newrelic.jfr.daemon {
  exports com.newrelic.jfr.daemon;

  requires jdk.crypto.ec;
  requires jdk.jfr;
  requires jdk.naming.rmi;
  requires java.management.rmi;
  requires java.net.http;
  requires org.slf4j;
  requires com.newrelic.telemetry;
  requires com.google.gson;
}
