package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import jdk.jfr.consumer.RecordedEvent;

import java.util.stream.Stream;

public class PerThreadNetworkWriteSummarizer implements EventToSummary {
    private final String threadName;
    private final LongSummarizer bytesSummary;
    private final DurationSummarizer duration;

    public PerThreadNetworkWriteSummarizer(String threadName, long startTimeMs) {
        this(threadName, new LongSummarizer("bytesWritten"), new DurationSummarizer(startTimeMs));
    }

    public PerThreadNetworkWriteSummarizer(String threadName, LongSummarizer longSummarizer,
                                           DurationSummarizer duration) {
        this.threadName = threadName;
        this.bytesSummary = longSummarizer;
        this.duration = duration;
    }

    @Override
    public String getEventName() {
        return NetworkWriteSummarizer.EVENT_NAME;
    }

    @Override
    public void accept(RecordedEvent ev) {
        bytesSummary.accept(ev);
        duration.accept(ev);
    }

    @Override
    public Stream<Summary> summarizeAndReset() {
        var attr = new Attributes()
                .put("thread.name", threadName);
        var outWritten = new Summary(
                "jfr:SocketWrite.bytesWritten",
                bytesSummary.getCount(),
                bytesSummary.getSum(),
                bytesSummary.getMin(),
                bytesSummary.getMax(),
                duration.getStartTimeMs(),
                duration.getEndTimeMs(),
                attr);
        var outDuration  = new Summary(
                "jfr:SocketWrite.duration",
                bytesSummary.getCount(),
                duration.getDurationMillis(),
                duration.getMinDurationMillis(),
                duration.getMaxDurationMillis(),
                duration.getStartTimeMs(),
                duration.getEndTimeMs(),
                attr);

        bytesSummary.reset();
        duration.reset();
        return Stream.of(outWritten, outDuration);
    }

}
