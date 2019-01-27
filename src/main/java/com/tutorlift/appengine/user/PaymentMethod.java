package com.tutorlift.appengine.user;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

/**
 * Represents a Payment resource.
 */
final class PaymentMethod {

  /**
   * Constants of Course entity properties in the database
   */
  final static class Property {
    static final String KIND = "PaymentMethod";
    static final String ID = "id";
    static final String TOKEN_ID = "tokenId";
    static final String BRAND = "brand";
    static final String LAST_4 = "last4";
    static final String EXPIRATION_MONTH = "expirationMonth";
    static final String EXPIRATION_YEAR = "expirationYear";
  }

  /**
   * Constants for Payment Methods
   */
  final static class PaymentMethodConstant {
    static final String CARD = "Card";
    static final String APPLE_PAY = "Apple Pay";
    static final String CASH = "Cash";
  }

  /* Stripe card token */
  private String tokenId;
  /* E.g. Visa */
  private String brand;
  private String last4;
  private String expirationMonth;
  private String expirationYear;

  /* Designated Constructor. */
  PaymentMethod(String tokenId, String brand, String last4,
      String expirationMonth, String expirationYear) {
    this.tokenId = tokenId;
    this.brand = brand;
    this.last4 = last4;
    this.expirationMonth = expirationMonth;
    this.expirationYear = expirationYear;
  }

  /* Default Constructor */
  PaymentMethod() {}

  /* Define Course resource properties access level */
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getTokenId() {
    return tokenId;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setTokenId(String tokenId) {
    this.tokenId = tokenId;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getBrand() {
    return brand;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setBrand(String brand) {
    this.brand = brand;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getLast4() {
    return last4;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setLast4(String last4) {
    this.last4 = last4;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getExpirationMonth() {
    return expirationMonth;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setExpirationMonth(String expirationMonth) {
    this.expirationMonth = expirationMonth;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  String getExpirationYear() {
    return expirationYear;
  }
  @ApiResourceProperty(ignored = AnnotationBoolean.FALSE)
  void setExpirationYear(String expirationYear) {
    this.expirationYear = expirationYear;
  }
}
