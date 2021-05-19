package com.newrelic.jfr.daemon;

import com.google.gson.stream.JsonWriter;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Duplicated code from EventBatchMarchsaller in  the Telemetry SDK.
That class is in a package that is not exported from the com.newrelic.telemertry module

There is probably a clever way to hack the module.
*/
public class BatchSizeMeasurer {

  private static final Logger logger = LoggerFactory.getLogger(BatchSizeMeasurer.class);

  public String toJson(EventBatch batch) {
    logger.debug("Preparing batch to be measured");

    Function<Event, Event> decorator = Function.identity();
    if (batch.hasCommonAttributes()) {
      decorator =
          event -> {
            Event out = new Event(event);
            out.getAttributes().putAll(batch.getCommonAttributes());
            return out;
          };
    }

    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append(
        batch
            .getTelemetry()
            .stream()
            .map(decorator)
            .map(BatchSizeMeasurer::mapToJson)
            .collect(Collectors.joining(",")));
    builder.append("]");
    return builder.toString();
  }

  static String mapToJson(Event event) {
    try {
      StringWriter out = new StringWriter();
      JsonWriter jsonWriter = new JsonWriter(out);
      jsonWriter.beginObject();

      jsonWriter.name("eventType").value(event.getEventType());
      jsonWriter.name("timestamp").value(event.getTimestamp());

      for (Map.Entry<String, Object> entry : event.getAttributes().asMap().entrySet()) {
        Object value = entry.getValue();
        if (value instanceof String) {
          String sValue = (String) value;
          jsonWriter.name(entry.getKey()).value(sValue);
        } else if (value instanceof Number) {
          Number nValue = (Number) value;
          jsonWriter.name(entry.getKey()).value(nValue);
        } else if (value instanceof Boolean) {
          Boolean bValue = (Boolean) value;
          jsonWriter.name(entry.getKey()).value(bValue);
        } else {
          throw new RuntimeException(
              String.format(
                  "Failed to generate json type %s encountered with value %s",
                  value.getClass(), value));
        }
      }

      jsonWriter.endObject();
      return out.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate summary json", e);
    }
  }
}
