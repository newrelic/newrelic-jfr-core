package com.newrelic.jfr;

import jdk.jfr.consumer.RecordedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to validate {@code RecordedObject} access.
 *
 * <p>{@code RecordedObject} fields are subject to change or removal in future JDK releases.
 * Accessors of {@code RecordedObject} fields will throw an {@code IllegalArgumentException} if the
 * field doesn't exist and thus a best practice is to validate the field before attempting access.
 */
public class RecordedObjectValidators {
  private static final Logger logger = LoggerFactory.getLogger(RecordedObjectValidators.class);

  private RecordedObjectValidators() {}

  /**
   * Checks whether or not {@code RecordedObject} fields exists. Does diagnostic logging if the
   * field cannot be accessed.
   *
   * @param recordedObject RecordedObject to get field from
   * @param callingClassName name of the event that is calling this API
   * @param objectField field to validate
   * @return true if field exists, else false
   */
  public static boolean hasField(
      RecordedObject recordedObject, String objectField, String callingClassName) {
    if (isRecordedObjectNull(recordedObject, callingClassName)) {
      logger.error(
          "Cannot validate field '"
              + objectField
              + "' due to null RecordedObject in '"
              + callingClassName
              + "'");
      return false;
    } else if (!recordedObject.hasField(objectField)) {
      logger.error(
          "Field '"
              + objectField
              + "' does not exist on RecordedObject in '"
              + callingClassName
              + "'");
      return false;
    }
    return true;
  }

  /**
   * Checks whether or not a {@code RecordedObject} instance is null and does diagnostic logging if
   * true.
   *
   * @param recordedObject RecordedObject to check for nullity
   * @param callingClassName name of the event that is calling this API
   * @return true if the RecordedObject is null, else false
   */
  public static boolean isRecordedObjectNull(
      RecordedObject recordedObject, String callingClassName) {
    if (recordedObject != null) {
      return false;
    }
    logger.error("RecordedObject is null in '" + callingClassName + "'");
    return true;
  }
}
