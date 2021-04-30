package com.newrelic.jfr.profiler;

import java.util.Collections;
import java.util.List;

/** This class holds the elements of a stack trace event */
public final class JvmStackTraceEvent implements StackTraceEvent {
    private final String threadName;
    private final String threadState;
    private final List<JvmStackFrame> frames;

    public JvmStackTraceEvent(String threadName, String threadState, List<JvmStackFrame> frames) {
        this.threadName = threadName;
        this.threadState = threadState;
        this.frames = Collections.unmodifiableList(frames);
    }

    public static final class JvmStackFrame {
        private final String desc;
        private final int line;
        private final int bytecodeIndex;

        public JvmStackFrame(String desc, int line, int bytecodeIndex) {
            this.desc = desc;
            this.line = line;
            this.bytecodeIndex = bytecodeIndex;
        }

        public String getDesc() {
            return desc;
        }

        public int getLine() {
            return line;
        }

        public int getBytecodeIndex() {
            return bytecodeIndex;
        }
    }

    public String getThreadName() {
        return threadName;
    }

    public String getThreadState() {
        return threadState;
    }

    public List<JvmStackFrame> getFrames() {
        return frames;
    }

    @Override
    public String toString() {
        return "JvmStackTraceEvent{"
                + "threadName='"
                + threadName
                + '\''
                + ", threadState='"
                + threadState
                + '\''
                + ", frames="
                + frames
                + '}';
    }
}
