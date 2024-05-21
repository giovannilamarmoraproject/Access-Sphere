package io.github.giovannilamarmora.accesssphere.exception;

import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;

@ControllerAdvice
public class ExceptionHandler extends UtilsException {

  @org.springframework.web.bind.annotation.ExceptionHandler(
      value = MissingRequestValueException.class)
  public ResponseEntity<ExceptionResponse> handleException(
      MissingRequestValueException e, ServerHttpRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ExceptionResponse response = getExceptionResponse(e, request, ExceptionMap.ERR_OAUTH_400);
    response.getError().setMessage("The params " + e.getName() + " is required!");
    response.getError().setException(ExceptionMap.ERR_OAUTH_400.exception());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }
}
