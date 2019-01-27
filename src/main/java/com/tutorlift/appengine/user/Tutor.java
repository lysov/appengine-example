package com.tutorlift.appengine.user;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.datastore.GeoPt;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Tutor resource.
 */
final class Tutor {

  /**
   * Constants of Tutor entity properties in the database
   */
  final static class Property {
    static final String KIND = "Tutor";
    static final String ID = "id";
    static final String PICTURE_URL = "pictureURL";
    static final String EMAIL = "email";
    static final String FIRST_NAME = "firstName";
    static final String LAST_NAME = "lastName";
    static final String RATING = "rating";
    static final String HEADLINE = "headline";
    static final String BIO = "bio";
    static final String POSTAL_CODE = "postalCode";
    static final String CITY = "city";
    static final String PROVINCE = "province";
    static final String GEO_POINT = "geoPoint";
    static final String RATE = "rate";
    static final String COURSES = "courses";
    static final String ONLINE_LESSON = "isOnlineLesson";
    static final String IN_PERSON_LESSON = "isInPersonLesson";
    static final String IS_HIDDEN = "isHidden";
    static final String IS_SEARCHABLE = "isSearchable";
  }

  /* Shared with Student */
  private String id;
  /* Shared with Student */
  private String pictureURL;
  /* Shared with Student */
  private String email;
  /* Shared with Student */
  private String firstName;
  /* Shared with Student */
  private String lastName;
  private Double rating;
  /* Shared with Student */
  private String headline;
  /* Optional upon creation */
  private String bio;
  /* Required upon creation */
  private String postalCode;
  /* Used in the search. E.g. "Calgary" */
  private String city;
  /* Used in the search. E.g. "AB" */
  private String province;
  private GeoPt geoPoint;
  /* Required upon creation */
  private Integer rate;
  /* There must be at least 1 course for the tutor user to be isSearchable */
  /* Optional upon creation */
  private List<String> courses;
  /* isOnlineLesson and isInPersonLesson cannot be false at the same time */
  /* Required upon creation */
  private Boolean isOnlineLesson;
  private Boolean isInPersonLesson;
  /* A student can decide whether his/her user can appear in the search results,
   * so he/she doesn't have to change her/his availability */
  /** Available to the authenticated user only */
  private Boolean isHidden;
  /* Indicates whether a tutor user all the necessary properties
  to appear in the search results */
  /** Available to the authenticated user only */
  private Boolean isSearchable;

  /*
  Designated Constructor.

  A newly created tutor user has a minimum of optional properties, thus
  it will immediately appear in the search.
   */
  Tutor(String id, String pictureURL, String email, String firstName, String lastName,
      Double rating, String headline, String bio, String postalCode,
      String city, String province, GeoPt geoPoint, Integer rate, List<String> courses,
      Boolean onlineLesson, Boolean isInPersonLesson, Boolean isHidden, Boolean isSearchable) {
    this.id = id;
    this.pictureURL = pictureURL;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.rating = rating;
    this.headline = headline;
    this.bio = bio;
    this.postalCode = postalCode;
    this.city = city;
    this.province = province;
    this.geoPoint = geoPoint;
    this.rate = rate;
    this.courses = courses;
    this.isOnlineLesson = onlineLesson;
    this.isHidden = isHidden;
    this.isInPersonLesson = isInPersonLesson;
    this.isSearchable = isSearchable;
  }

  /* Default Constructor */
  Tutor() {}

  /* Define Tutor resource properties access level */
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
  Double getRating() {
    return rating;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setRating(Double rating) {
    this.rating = rating;
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
  String getBio() {
    return bio;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setBio(String bio) {
    this.bio = bio;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getPostalCode() {
    return postalCode;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getCity() {
    return city;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setCity(String city) {
    this.city = city;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getProvince() {
    return province;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setProvince(String province) {
    this.province = province;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  GeoPt getGeoPoint() {
    return geoPoint;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setGeoPoint(GeoPt geoPoint) {
    this.geoPoint = geoPoint;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  Integer getRate() {
    return rate;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setRate(Integer rate) {
    this.rate = rate;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  List<String> getCourses() {
    return courses;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setCourses(ArrayList<String> courses) {
    this.courses = courses;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  Boolean getIsOnlineLesson() {
    return isOnlineLesson;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  public void setIsOnlineLesson(Boolean isOnlineLesson) {
    this.isOnlineLesson = isOnlineLesson;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  Boolean getIsInPersonLesson() {
    return isInPersonLesson;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  public void setIsInPersonLesson(Boolean isInPersonLesson) {
    this.isInPersonLesson = isInPersonLesson;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  Boolean getIsHidden() {
    return isHidden;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setIsHidden(Boolean isHidden) {
    this.isHidden = isHidden;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  Boolean getIsSearchable() {
    return isSearchable;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  void setIsSearchable(Boolean isSearchable) {
    this.isSearchable = isSearchable;
  }

}
