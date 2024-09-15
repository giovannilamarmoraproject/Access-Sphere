package io.github.giovannilamarmora.accesssphere.utilities;

import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.web.CookieManager;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;

public interface RequestManager {

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static String getCookieOrHeaderData(String name, ServerHttpRequest request) {
    String cookie = CookieManager.getCookie(name, request);
    if (ObjectUtils.isEmpty(cookie)) return HeaderManager.getHeader(name, request);
    return cookie;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  static String getCookieOrHeaderData(
      String cookieName, String headerName, ServerHttpRequest request) {
    String cookie = CookieManager.getCookie(cookieName, request);
    if (ObjectUtils.isEmpty(cookie)) return HeaderManager.getHeader(headerName, request);
    return cookie;
  }
}
