package com.newrelic.jfr.profiler;

public class StackFrame {
    private final String name;
    private final Integer count;
    private final String parentId;
    private final String id;

    public StackFrame(String name, Integer count, String parentId, String id) {
        this.name = name;
        this.count = count;
        this.parentId = parentId;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getCount() {
        return count;
    }

    public String getParentId() {
        return parentId;
    }

    public String getId() {
        return id;
    }
}
