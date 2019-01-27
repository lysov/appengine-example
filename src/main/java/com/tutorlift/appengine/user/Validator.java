package com.tutorlift.appengine.user;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.RegexValidator;

/**
 * Provides methods for user input validation.
 */
final class Validator {

  private static final String POSTAL_CODE_PATTERN =
      "[ABCEGHJKLMNPRSTVXY][0-9][ABCEGHJKLMNPRSTVWXYZ][0-9][ABCEGHJKLMNPRSTVWXYZ][0-9]";

  static Boolean isValidEmail(String email) {
    return EmailValidator.getInstance().isValid(email);
  }

  static Boolean isValidName(String name) {
    return name.length() >= 1 && name.length() <= 20;
  }

  static Boolean isValidHeadline(String headline) {
    return headline.length() <= 50;
  }

  static Boolean isValidBio(String bio) {
    return bio.length() <= 1000;
  }

  static Boolean isValidRate(Integer rate) {
    return rate >= 15 && rate <= 1000;
  }

  static Boolean isValidPerPage(Integer perPage) {
    return perPage > 0 && perPage < 50;
  }

  /** Accepts either "T2N4V5" or "T2N 4V5" format */
  static Boolean isValidPostalCode(String postalCode) {
    RegexValidator postalCodeRegex = new RegexValidator(POSTAL_CODE_PATTERN);
    return postalCodeRegex.isValid(postalCode.replaceAll("\\s",""));
  }
}
