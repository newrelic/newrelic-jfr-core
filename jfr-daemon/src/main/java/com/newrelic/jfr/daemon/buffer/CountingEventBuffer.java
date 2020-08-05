package com.newrelic.jfr.daemon.buffer;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventBuffer;

/** Works around the fact that the EventBuffer does not yet keep track of size. */
public class CountingEventBuffer {
  private final EventBuffer delegate;
  private int size = 0;

  public CountingEventBuffer(Attributes attributes) {
    this(new EventBuffer(attributes));
  }

  public CountingEventBuffer(EventBuffer delegate) {
    this.delegate = delegate;
  }

  public void addEvent(Event event) {
    delegate.addEvent(event);
    size++;
  }

  public int size() {
    return size;
  }

  public EventBatch createBatch() {
    size = 0;
    return delegate.createBatch();
  }
}
