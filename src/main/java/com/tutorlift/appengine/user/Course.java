package com.tutorlift.appengine.user;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import java.util.List;

/**
 * Represents a Course resource.
 */
final class Course {

  /**
   * Constants of Course entity properties in the database
   */
  final static class Property {
    static final String KIND = "Course";
    static final String NAME = "name";
    static final String SUBJECT = "subject";
  }

  /* E.g. Math 10 */
  private String name;
  /* E.g. Math */
  private String subject;

  /* Designated Constructor. */
  Course(String name, String subject) {
    this.name = name;
    this.subject = subject;
  }

  /* Default Constructor */
  Course() {}

  /* Define Course resource properties access level */
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  public String getSubject() {
    return subject;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  public String getName() {
    return name;
  }
}
