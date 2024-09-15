package io.github.giovannilamarmora.accesssphere.utilities;

import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.ObjectUtils;

public interface HeaderManager {
  Logger LOG = LoggerFilter.getLogger(HeaderManager.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static void setHeaderInResponse(
      String headerName, String headerValue, ServerHttpResponse response) {
    if (ObjectUtils.isEmpty(headerValue)) return;
    response.getHeaders().set(headerName, headerValue);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static void addHeaderInResponse(
      String headerName, String headerValue, ServerHttpResponse response) {
    if (ObjectUtils.isEmpty(headerValue)) return;
    response.getHeaders().add(headerName, headerValue);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static void addOrSetHeaderInResponse(
      String headerName, String headerValue, ServerHttpResponse response) {
    if (ObjectUtils.isEmpty(headerValue)) return;
    if (response.getHeaders().containsKey(headerName))
      setHeaderInResponse(headerName, headerValue, response);
    else addHeaderInResponse(headerName, headerValue, response);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static String getHeader(String headerName, ServerHttpRequest request) {
    return !ObjectUtils.isEmpty(headerName)
            && !ObjectUtils.isEmpty(request.getHeaders().getFirst(headerName))
        ? request.getHeaders().getFirst(headerName)
        : null;
  }
}
