package com.newrelic.jfr;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

import java.util.Optional;

public class Workarounds {

    /**
     * There are cases where the event has the wrong type inside it
     * for the thread, so calling {@link RecordedEvent#getThread(String)} internally
     * throws a {@link ClassCastException}. We work around it here by just
     * getting the raw value and checking the type.
     *
     * @param ev The event from which to carefully extract the thread
     * @return the thread name, or null if unable to extract it
     */
    public static Optional<String> getThreadName(RecordedEvent ev) {
        Object thisField = ev.getValue("eventThread");
        if (thisField instanceof RecordedThread) {
            return Optional.of(((RecordedThread) thisField).getJavaName());
        }
        return Optional.empty();
    }
}
