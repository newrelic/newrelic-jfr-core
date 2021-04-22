package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.AttributeValueSplitter.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newrelic.telemetry.Attributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeValueSplitterTest {

  private AttributeValueSplitter classUnderTest;
  private final String TEST_KEY = "test_key";
  private final String TEST_KEY_EXTENDED = TEST_KEY + KEY_EXTENDED;
  private Attributes attr;

  @BeforeEach
  void setup() {
    this.classUnderTest = new AttributeValueSplitter();
    this.attr = new Attributes();
  }

  @Test
  void keyShouldNotIncrement() {
    assertEquals(TEST_KEY, classUnderTest.incrementKey(0, TEST_KEY));
  }

  @Test
  void keyIncrementTo3() {
    assertEquals(TEST_KEY_EXTENDED + 3, classUnderTest.incrementKey(3, TEST_KEY));
  }

  @Test
  void attrHasOneKeyValue() {
    var testValue = "a";
    testValue = testValue.repeat(MAX_LENGTH);
    classUnderTest.maybeSplit(attr, TEST_KEY, testValue);
    assertEquals(attr.asMap().size(), 1);
    assertEquals(testValue, attr.asMap().get(TEST_KEY));
  }

  @Test
  void attrHasTwoKeyValues() {
    var expectedExtendedValue = "a";
    var expectedFirstValue = expectedExtendedValue.repeat(MAX_LENGTH);
    var testValue = expectedExtendedValue.repeat(MAX_LENGTH + 1);
    var expectedExtendedKey = TEST_KEY_EXTENDED + 1;

    classUnderTest.maybeSplit(attr, TEST_KEY, testValue);
    assertEquals(attr.asMap().size(), 2);
    assertEquals(expectedExtendedValue, attr.asMap().get(expectedExtendedKey));
    assertEquals(expectedFirstValue, attr.asMap().get(TEST_KEY));
  }

  @Test
  void attrHasTwo4096Values() {
    var value = "a";
    var expectedValue = value.repeat(MAX_LENGTH);
    var testValue = value.repeat(MAX_LENGTH * 2);
    var expectedExtendedKey = TEST_KEY_EXTENDED + 1;

    classUnderTest.maybeSplit(attr, TEST_KEY, testValue);
    assertEquals(attr.asMap().size(), 2);
    assertEquals(expectedValue, attr.asMap().get(expectedExtendedKey));
    assertEquals(expectedValue, attr.asMap().get(TEST_KEY));
  }
}
