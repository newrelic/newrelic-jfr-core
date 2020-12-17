module com.newrelic.jfr.daemon {
    exports com.newrelic.jfr.daemon;

    requires jdk.jfr;
    requires java.net.http;
    requires java.management;
    requires org.slf4j;
    requires com.newrelic.telemetry;
    requires com.google.gson;
//    requires com.newrelic.jfr;
}

//module com.newrelic.jfr {
//        exports com.newrelic.jfr;
//        exports com.newrelic.jfr.toevent;
//        exports com.newrelic.jfr.tometric;
//        exports com.newrelic.jfr.tosummary;
//
//        requires jdk.jfr;
//        requires com.google.gson;
//        requires com.newrelic.telemetry;
//        }