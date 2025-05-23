package io.github.giovannilamarmora.accesssphere.exception;

import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ExceptionMap implements ExceptionCode {
  /**
   * @oAuth2.0 Exception Map for Authentication
   */
  ERR_OAUTH_400(ExceptionType.BAD_INPUT_ERROR.name(), HttpStatus.BAD_REQUEST, "Input miss match"),
  ERR_OAUTH_401(
      "OAUTH_NOT_VALID",
      HttpStatus.UNAUTHORIZED,
      "You cannot make this request cause the auth-token is invalid"),
  ERR_OAUTH_403(
      "OAUTH_ACCESS_DENIED", HttpStatus.FORBIDDEN, "You are not allowed to make this request"),
  ERR_OAUTH_404("DATA_NOT_FOUND", HttpStatus.NOT_FOUND, "Data Not Found"),
  ERR_OAUTH_423("ACCOUNT_BLOCKED", HttpStatus.LOCKED, "Your account has been blocked."),
  ERR_OAUTH_500("OAUTH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Generic Server Error"),
  /**
   * @Token Exception Map for Token
   */
  ERR_TOKEN_400("BAD_INPUT_ERROR", HttpStatus.BAD_REQUEST, "Invalid input"),
  ERR_TOKEN_401(
      "TOKEN_NOT_VALID",
      HttpStatus.UNAUTHORIZED,
      "You cannot make this request cause the auth-token is invalid"),
  ERR_TOKEN_500("TOKEN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Generic Server Error"),
  /**
   * @Strapi Exception Map for Strapi
   */
  ERR_STRAPI_400("BAD_INPUT_ERROR", HttpStatus.NOT_FOUND, "Invalid input"),
  ERR_STRAPI_404("DATA_NOT_FOUND", HttpStatus.NOT_FOUND, "Data not found"),
  ERR_STRAPI_500("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Generic Server Error"),
  /**
   * @User Exception Map for User
   */
  ERR_USER_400("BAD_INPUT_ERROR", HttpStatus.BAD_REQUEST, "Invalid input"),
  ERR_USER_401(
      "TOKEN_NOT_VALID",
      HttpStatus.UNAUTHORIZED,
      "You cannot make this request cause the auth-token is invalid"),
  ERR_USER_415(
      "PAYLOAD_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, "Data is too large to be uploaded"),
  ERR_USER_429("TOO_MANY_REQUEST", HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
  ERR_EMAIL_SEND_001("CLIENT_EXCEPTION", HttpStatus.BAD_REQUEST, "Error on client: "),
  /**
   * @TechnicalUser ExceptionMap for TechUser
   */
  ERR_TECH_403(
      "TECHNICAL_EXCEPTION", HttpStatus.FORBIDDEN, "You are not allowed to make this request"),
  /**
   * @MFA ExceptionMap for MFA
   */
  ERR_MFA_400("BAD_INPUT_ERROR", HttpStatus.BAD_REQUEST, "Invalid input");

  private final HttpStatus status;
  private final String message;
  private final String exception;

  ExceptionMap(String exception, HttpStatus status, String message) {
    this.exception = exception;
    this.status = status;
    this.message = message;
  }

  @Override
  public String exception() {
    return this.exception;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public HttpStatus getStatus() {
    return this.status;
  }
}
