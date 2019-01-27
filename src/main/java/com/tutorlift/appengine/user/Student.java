package com.tutorlift.appengine.user;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

/**
 * Represents a Student resource.
 */
final class Student {

  /**
   * Constants of Student entity properties in the database
   */
  final static class Property {
    static final String KIND = "Student";
    static final String ID = "id";
    static final String STRIPE_ID = "stripeId";
    static final String CARD_ID = "cardId";
    static final String PICTURE_URL = "pictureURL";
    static final String EMAIL = "email";
    static final String FIRST_NAME = "firstName";
    static final String LAST_NAME = "lastName";
    static final String HEADLINE = "headline";
    static final String DEFAULT_PAYMENT_METHOD = "defaultPaymentMethod";
    static final String USER_TYPE = "userType";
  }

  /* Shared with Student */
  private String id;
  /* Shared with Student */
  /* Optional upon creation */
  private String pictureURL;
  /* Shared with Student */
  private String email;
  /* Shared with Student */
  private String firstName;
  /* Shared with Student */
  private String lastName;
  /* Shared with Student */
  /* Optional upon creation */
  private String headline;
  /* Optional for registration. "Cash" or "Card" or "Apple Pay". */
  private String defaultPaymentMethod;
  /* Determines if a tutor user needs to be updated and
  the user mode on the client ("Student" or "Tutor") */
  private String userType;

  /* Designated Constructor */
  Student(String id, String pictureURL, String email, String firstName, String lastName,
      String headline, String defaultPaymentMethod, String userType) {
    this.id = id;
    this.pictureURL = pictureURL;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.headline = headline;
    this.defaultPaymentMethod = defaultPaymentMethod;
    this.userType = userType;
  }

  /* Default Constructor */
  Student() {}

  /* Define Student resource properties access level */
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getId() {
    return id;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setId(String id) {
    this.id = id;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getPictureURL() {
    return pictureURL;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setPictureURL(String pictureURL) {
    this.pictureURL = pictureURL;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getEmail() {
    return email;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setEmail(String email) {
    this.email = email;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getFirstName() {
    return firstName;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getLastName() {
    return lastName;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getHeadline() {
    return headline;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setHeadline(String headline) {
    this.headline = headline;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getDefaultPaymentMethod() {
    return defaultPaymentMethod;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setDefaultPaymentMethod(String defaultPaymentMethod) {
    this.defaultPaymentMethod = defaultPaymentMethod;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getUserType() {
    return userType;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setUserType(String userType) {
    this.userType = userType;
  }
}
