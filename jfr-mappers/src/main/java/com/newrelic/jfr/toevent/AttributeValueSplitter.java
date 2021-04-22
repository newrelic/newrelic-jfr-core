package com.newrelic.jfr.toevent;

import com.newrelic.telemetry.Attributes;

public class AttributeValueSplitter {

  static final int MAX_LENGTH = 4096; // Event API attribute size limit
  static final String KEY_EXTENDED = "Extended_";

  void maybeSplit(Attributes attr, String key, String value) {
    int extendedCount = 0;
    while (value.length() > MAX_LENGTH) {
      String firstBlock = value.substring(0, MAX_LENGTH);
      attr.put(incrementKey(extendedCount, key), firstBlock);

      value = value.substring(MAX_LENGTH);
      extendedCount = extendedCount + 1;
    }
    attr.put(incrementKey(extendedCount, key), value);
  }

  String incrementKey(int extendedCount, String key) {
    return extendedCount == 0 ? key : key + KEY_EXTENDED + extendedCount;
  }
}
