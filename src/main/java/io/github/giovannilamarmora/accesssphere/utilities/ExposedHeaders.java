package io.github.giovannilamarmora.accesssphere.utilities;

public interface ExposedHeaders {
  String DEVICE_TOKEN = Cookie.COOKIE_DEVICE_TOKEN;
  String SESSION_ID = Cookie.COOKIE_SESSION_ID;
  String AUTHORIZATION = "Authorization";
  String LOCATION = "Location";
  String TRACE_ID = "Trace-ID";
  String SPAN_ID = "Span-ID";
  String PARENT_ID = "Parent-ID";
  String REGISTRATION_TOKEN = Cookie.COOKIE_TOKEN;
  String REDIRECT_URI = Cookie.COOKIE_REDIRECT_URI;
}
