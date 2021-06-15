package com.newrelic.jfr.profiler;

import java.util.Objects;

public final class FlameLevel {
  private final String name;
  private final Integer count;
  private final String parentId;
  private final String id;

  public FlameLevel(String name, Integer count, String parentId, String id) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FlameLevel that = (FlameLevel) o;
    return Objects.equals(name, that.name)
        && Objects.equals(count, that.count)
        && Objects.equals(parentId, that.parentId)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, count, parentId, id);
  }

  @Override
  public String toString() {
    return "FlameLevel{"
        + "name='"
        + name
        + '\''
        + ", count="
        + count
        + ", parentId='"
        + parentId
        + '\''
        + ", id='"
        + id
        + '\''
        + '}';
  }
}
