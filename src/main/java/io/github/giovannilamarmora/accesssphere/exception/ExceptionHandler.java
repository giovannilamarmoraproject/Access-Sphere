package io.github.giovannilamarmora.accesssphere.exception;

import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.PayloadTooLargeException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionHandler extends UtilsException {

  @org.springframework.web.bind.annotation.ExceptionHandler(
      value = MissingRequestValueException.class)
  public ResponseEntity<ExceptionResponse> handleException(
      MissingRequestValueException e, ServerHttpRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ExceptionResponse response = getExceptionResponse(e, request, ExceptionMap.ERR_OAUTH_400);
    response.getError().setMessage("The params " + e.getName() + " is required!");
    ExceptionType exceptionType = ExceptionType.fromParam(e.getName(), false);
    response
        .getError()
        .setException(
            ObjectToolkit.isNullOrEmpty(exceptionType)
                ? ExceptionMap.ERR_OAUTH_400.exception()
                : exceptionType.name());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      value = HandlerMethodValidationException.class)
  public ResponseEntity<ExceptionResponse> handleMethodValidationException(
      HandlerMethodValidationException e, ServerHttpRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ExceptionCode code = ExceptionMap.ERR_OAUTH_400;
    if (request.getPath().value().contains("mfa")) {
      code = ExceptionMap.ERR_MFA_400;
    } else if (request.getPath().value().contains("user")) {
      code = ExceptionMap.ERR_USER_400;
    }

    // Estrae i messaggi di validazione specifici (es. @NotNull, @NotEmpty, @NotBlank)
    List<String> messages =
        e.getAllErrors().stream()
            .map(
                error -> {
                  if (error instanceof FieldError fieldError) {
                    String defaultMessage = fieldError.getDefaultMessage();
                    if (defaultMessage != null
                        && (defaultMessage.startsWith("must ")
                            || defaultMessage.startsWith("non "))) {
                      return fieldError.getField() + " " + defaultMessage;
                    }
                    return defaultMessage;
                  }
                  return error.getDefaultMessage();
                })
            .filter(msg -> !ObjectToolkit.isNullOrEmpty(msg))
            .distinct()
            .toList();

    String message =
        !messages.isEmpty()
            ? String.join(", ", messages)
            : ObjectToolkit.getOrDefault(e.getReason(), "Validation failure");

    // Risolve l'ExceptionType contestuale in base al campo del payload violato
    String fieldName = null;
    for (ParameterErrors beanResult : e.getBeanResults()) {
      for (FieldError fieldError : beanResult.getFieldErrors()) {
        fieldName = fieldError.getField();
        break;
      }
      if (fieldName != null) break;
    }
    if (fieldName == null) {
      for (ParameterValidationResult valResult : e.getValueResults()) {
        fieldName = valResult.getMethodParameter().getParameterName();
        break;
      }
    }

    ExceptionType exceptionType = null;
    if (!ObjectToolkit.isNullOrEmpty(fieldName)) {
      exceptionType = ExceptionType.fromParam(fieldName, false);
      if (exceptionType == null) {
        // Conversione camelCase in snake_case (es. clientId -> client_id)
        String snakeCase = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        exceptionType = ExceptionType.fromParam(snakeCase, false);
      }
      if (exceptionType == null && fieldName.toLowerCase().contains("role")) {
        exceptionType = ExceptionType.INVALID_ROLES;
      }
    }

    ExceptionResponse response = getExceptionResponse(e, request, code);
    response.getError().setMessage(message);
    response
        .getError()
        .setException(
            ObjectToolkit.isNullOrEmpty(exceptionType) ? code.exception() : exceptionType.name());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      value = ConstraintViolationException.class)
  public ResponseEntity<ExceptionResponse> handleConstraintViolationException(
      ConstraintViolationException e, ServerHttpRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    ExceptionCode code = ExceptionMap.ERR_OAUTH_400;
    if (request.getPath().value().contains("mfa")) {
      code = ExceptionMap.ERR_MFA_400;
    } else if (request.getPath().value().contains("user")) {
      code = ExceptionMap.ERR_USER_400;
    }

    List<String> messages =
        e.getConstraintViolations().stream()
            .map(
                violation -> {
                  String defaultMessage = violation.getMessage();
                  if (defaultMessage != null
                      && (defaultMessage.startsWith("must ")
                          || defaultMessage.startsWith("non "))) {
                    return violation.getPropertyPath() + " " + defaultMessage;
                  }
                  return defaultMessage;
                })
            .filter(msg -> !ObjectToolkit.isNullOrEmpty(msg))
            .distinct()
            .toList();

    String message =
        !messages.isEmpty()
            ? String.join(", ", messages)
            : ObjectToolkit.getOrDefault(e.getMessage(), "Validation failure");

    ExceptionResponse response = getExceptionResponse(e, request, code);
    response.getError().setMessage(message);
    response.getError().setException(code.exception());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(value = PayloadTooLargeException.class)
  public ResponseEntity<ExceptionResponse> handleException(
      PayloadTooLargeException e, ServerHttpRequest request) {
    HttpStatus status = ExceptionMap.ERR_USER_415.getStatus();
    ExceptionResponse response = getExceptionResponse(e, request, ExceptionMap.ERR_USER_415);
    response.getError().setMessage("Data is too large to be uploaded");
    response.getError().setException(ExceptionMap.ERR_USER_415.exception());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(value = ServerWebInputException.class)
  public ResponseEntity<ExceptionResponse> handleServerWebInputException(
      ServerWebInputException e, ServerHttpRequest request) {
    HttpStatus status = (HttpStatus) e.getStatusCode();
    ExceptionCode code = ExceptionMap.ERR_OAUTH_400;
    if (request.getPath().value().contains("mfa")) {
      code = ExceptionMap.ERR_MFA_400;
    }
    String message = null;
    ExceptionType exceptionType = null;
    if (e instanceof WebExchangeBindException bindException) {
      List<String> messages =
          bindException.getBindingResult().getFieldErrors().stream()
              .map(
                  fieldError -> {
                    String defaultMessage = fieldError.getDefaultMessage();
                    if (defaultMessage != null
                        && (defaultMessage.startsWith("must ")
                            || defaultMessage.startsWith("non "))) {
                      return fieldError.getField() + " " + defaultMessage;
                    }
                    return defaultMessage;
                  })
              .filter(msg -> !ObjectToolkit.isNullOrEmpty(msg))
              .distinct()
              .toList();

      if (!messages.isEmpty()) {
        message = String.join(", ", messages);
      } else {
        message =
            bindException.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(msg -> !ObjectToolkit.isNullOrEmpty(msg))
                .findFirst()
                .orElse(null);
      }

      FieldError firstFieldError = bindException.getFieldError();
      if (firstFieldError != null) {
        String fieldName = firstFieldError.getField();
        exceptionType = ExceptionType.fromParam(fieldName, false);
        if (exceptionType == null) {
          String snakeCase = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
          exceptionType = ExceptionType.fromParam(snakeCase, false);
        }
        if (exceptionType == null && fieldName.toLowerCase().contains("role")) {
          exceptionType = ExceptionType.INVALID_ROLES;
        }
      }
    } else if (e.getCause() != null && e.getCause() instanceof DecodingException) {
      if (!ObjectToolkit.isNullOrEmpty(e.getCause().getCause().getCause()))
        message = e.getCause().getCause().getCause().getMessage();
      else if (!ObjectToolkit.isNullOrEmpty(e.getCause().getCause()))
        message = e.getCause().getCause().getMessage();
      message = e.getCause().getMessage();
    }
    ExceptionResponse response = getExceptionResponse(e, request, code);
    response.getError().setMessage(ObjectToolkit.getOrDefault(message, e.getMessage()));
    response
        .getError()
        .setException(
            ObjectToolkit.isNullOrEmpty(exceptionType) ? code.exception() : exceptionType.name());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    return new ResponseEntity<>(response, status);
  }

  public static Mono<Void> handleFilterException(UtilsException e, ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse serverHttpResponse = exchange.getResponse();
    HttpStatus status = e.getExceptionCode().getStatus();
    ExceptionResponse response = getExceptionResponse(e, request, e.getExceptionCode());
    response.getError().setMessage(e.getMessage());
    response.getError().setException(e.getExceptionCode().exception());
    response.getError().setExceptionMessage(null);
    response.getError().setStackTrace(null);
    serverHttpResponse
        .getHeaders()
        .set("Access-Control-Allow-Origin", request.getHeaders().getFirst("origin"));
    serverHttpResponse
        .getHeaders()
        .set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    serverHttpResponse
        .getHeaders()
        .set("Access-Control-Allow-Headers", "Authorization, Content-Type");
    serverHttpResponse.getHeaders().set("Access-Control-Allow-Credentials", "true");
    serverHttpResponse.setStatusCode(status);
    serverHttpResponse.setRawStatusCode(status.value());
    serverHttpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer responseBuffer =
        new DefaultDataBufferFactory().wrap(Mapper.writeObjectToString(response).getBytes());
    return serverHttpResponse.writeWith(Mono.just(responseBuffer));
  }
}
