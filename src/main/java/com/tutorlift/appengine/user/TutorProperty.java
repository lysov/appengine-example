package com.tutorlift.appengine.user;

/**
 * Represents optional query parameters for the GET `/tutors?properties` endpoint
 */
enum TutorProperty {

  ID("id"),
  PICTURE_URL("picture-url"),
  EMAIL("email"),
  FIRST_NAME("first-name"),
  LAST_NAME("last-name"),
  RATING("rating"),
  HEADLINE("headline"),
  BIO("bio"),
  POSTAL_CODE("postal-code"),
  CITY("city"),
  PROVINCE("province"),
  GEO_POINT("geo-point"),
  RATE("rate"),
  COURSES("courses"),
  LESSON_TYPE("lesson-type");

  private String field;

  TutorProperty(String field) {
    this.field = field;
  }

  public String getField() {
    return field;
  }

  public static boolean contains(String testedProperty) {

    for (TutorProperty property : TutorProperty.values()) {
      if (property.name().equals(testedProperty)) {
        return true;
      }
    }

    return false;
  }
}
