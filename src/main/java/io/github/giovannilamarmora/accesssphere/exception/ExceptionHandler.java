package io.github.giovannilamarmora.accesssphere.exception;

import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.exception.dto.ExceptionResponse;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
