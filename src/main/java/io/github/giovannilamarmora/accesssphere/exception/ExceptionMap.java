package io.github.giovannilamarmora.accesssphere.exception;

import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum ExceptionMap implements ExceptionCode {
  /**
   * @oAuth2.0 Exception Map for Authentication
   */
  ERR_OAUTH_400("BAD_INPUT_ERROR", HttpStatus.BAD_REQUEST, "Input miss match"),
  ERR_OAUTH_401("OAUTH_NOT_AUTHORIZED", HttpStatus.UNAUTHORIZED, "You cannot make this request"),
  ERR_OAUTH_403("OAUTH_NOT_VALID", HttpStatus.FORBIDDEN, "You cannot make this request cause the auth-token is invalid"),
  ERR_OAUTH_404("NOT_FOUND", HttpStatus.NOT_FOUND, "Data Not Found"),
  ERR_OAUTH_500("OAUTH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Generic Server Error"),
  /**
   * @Token Exception Map for Token
   */
  ERR_TOKEN_400("BAD_INPUT_ERROR", HttpStatus.BAD_REQUEST, "Invalid input"),
  ERR_TOKEN_401("TOKEN_NOT_VALID", HttpStatus.UNAUTHORIZED, "You cannot make this request cause the auth-token is invalid"),
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
  ERR_USER_401("TOKEN_NOT_VALID", HttpStatus.UNAUTHORIZED, "You cannot make this request cause the auth-token is invalid"),
  ERR_USER_500("TOKEN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Generic Server Error"),
  /**
   *
   * @Authentication Exception Map for Authentication
   */
  ERR_AUTH_MSS_002(
      "WRONG_CREDENTIAL",
      HttpStatus.UNAUTHORIZED,
      "Wrong Credential for username or password. Try again!"),
  ERR_AUTH_MSS_003("AUTHENTICATION_EXCEPTION", HttpStatus.BAD_REQUEST, "Generic Error"),
  ERR_AUTH_MSS_005(
      "INVALID_INVITATION_CODE",
      HttpStatus.UNAUTHORIZED,
      "Error on checking the current invitation code provided, wrong code, try again!"),
  ERR_AUTH_MSS_006("INVALID_EMAIL", HttpStatus.BAD_REQUEST, "Invalid email address, Try Again!"),
  ERR_AUTH_MSS_008(
      "AUTH_TOKEN_NOT_VALID",
      HttpStatus.FORBIDDEN,
      "You cannot make this request cause the auth-token is invalid"),
  ERR_AUTH_MSS_009("INVALID_REGEX", HttpStatus.BAD_REQUEST, "Invalid regex passed!"),

  // Image
  ERR_IMG_MSS_001("FILE_NOT_FOUND", HttpStatus.BAD_REQUEST, "File not found!"),
  ERR_IMG_MSS_002(
      "MaxUploadSizeExceeded",
      HttpStatus.BAD_REQUEST,
      "Maximum upload size exceeded! the request was rejected because its size exceeds the configured maximum (512000)"),
  // EmailSender
  ERR_EMAIL_SEND_001("CLIENT_EXCEPTION", HttpStatus.BAD_REQUEST, "Error on client: "),
  ERR_EMAIL_SEND_002(
      "STRING_EXCEPTION", HttpStatus.BAD_REQUEST, "Error on converting string for templates"),
  ERR_JSON_FOR_001("JSON_FORMAT_EXCEPTION", HttpStatus.BAD_REQUEST, "Error on converting object"),
  ERR_ASSET_001(
      "ASSET_NOT_FOUND",
      HttpStatus.NOT_FOUND,
      "An error happen during filtering assets, asset not found!"),
  // Client
  ERR_COIN_GECKO_001(
      "COIN_GECKO_EXCEPTION", HttpStatus.BAD_REQUEST, "An error happen during call CoinGecko!"),
  ERR_EXC_RATES_001(
      "EXCHANGE_RATES_EXCEPTION",
      HttpStatus.BAD_REQUEST,
      "An error happen during call Exchange Rates!"),
  ERR_ANY_API_001(
      "ANY_API_EXCEPTION", HttpStatus.BAD_REQUEST, "An error happen during call AnyApi Rates!"),
  ERR_FOR_DATA_001(
      "FOREX_DATA_EXCEPTION", HttpStatus.BAD_REQUEST, "An error happen during call Forex!"),
  ERR_THREAD_001("THREAD_ERROR", HttpStatus.BAD_REQUEST, "An error happen during sleeping Thread!");

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
