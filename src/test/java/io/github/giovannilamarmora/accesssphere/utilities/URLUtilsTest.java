package io.github.giovannilamarmora.accesssphere.utilities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

class URLUtilsTest {

  @Test
  @DisplayName("resolveBaseUrl throws IllegalArgumentException when input is empty or null")
  void testResolveBaseUrlThrowsWhenMissing() {
    assertThrows(IllegalArgumentException.class, () -> URLUtils.resolveBaseUrl(null));
    assertThrows(IllegalArgumentException.class, () -> URLUtils.resolveBaseUrl(""));
    assertThrows(IllegalArgumentException.class, () -> URLUtils.resolveBaseUrl("   "));
  }

  @Test
  @DisplayName("resolveBaseUrl normalizes remote domain with https and strips trailing slashes")
  void testResolveBaseUrlRemote() {
    assertEquals(
        "https://access-sphere.giovannilamarmora.com",
        URLUtils.resolveBaseUrl("access-sphere.giovannilamarmora.com///"));
  }

  @Test
  @DisplayName("resolveBaseUrl detects localhost and development hosts and applies http")
  void testResolveBaseUrlLocal() {
    assertEquals("http://localhost:8081", URLUtils.resolveBaseUrl("localhost:8081"));
    assertEquals("http://localhost", URLUtils.resolveBaseUrl("localhost"));
    assertEquals("http://127.0.0.1:8080", URLUtils.resolveBaseUrl("127.0.0.1:8080"));
    assertEquals("http://auth.local:3000", URLUtils.resolveBaseUrl("auth.local:3000"));
    assertEquals("http://192.168.1.50:8080", URLUtils.resolveBaseUrl("192.168.1.50:8080"));
    assertEquals("http://example.com:80", URLUtils.resolveBaseUrl("example.com:80"));
  }

  @Test
  @DisplayName("resolveUrlWithScheme preserves explicit http and https schemes")
  void testResolveUrlWithSchemePreservesScheme() {
    assertEquals("http://localhost:8081", URLUtils.resolveUrlWithScheme("http://localhost:8081/"));
    assertEquals("https://localhost:8443", URLUtils.resolveUrlWithScheme("https://localhost:8443"));
    assertEquals("http://remote.org", URLUtils.resolveUrlWithScheme("http://remote.org"));
    assertEquals("https://remote.org", URLUtils.resolveUrlWithScheme("https://remote.org"));
    assertNull(URLUtils.resolveUrlWithScheme(null));
    assertNull(URLUtils.resolveUrlWithScheme("   "));
  }

  @Test
  @DisplayName("resolveIssuer respects request headers and URL context")
  void testResolveIssuer() {
    // Caso 1: request null con localhost
    assertEquals("http://localhost:8081", URLUtils.resolveIssuer("localhost:8081", null));

    // Caso 2: request null con dominio remoto
    assertEquals("https://api.domain.com", URLUtils.resolveIssuer("api.domain.com", null));

    // Caso 3: remoteAddress nullo
    assertNull(URLUtils.resolveIssuer(null, null));

    // Caso 4: request con header X-Forwarded-Proto https
    ServerHttpRequest requestWithProto = mock(ServerHttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Forwarded-Proto", "https");
    when(requestWithProto.getHeaders()).thenReturn(headers);
    assertEquals(
        "https://my-proxy-host.com",
        URLUtils.resolveIssuer("my-proxy-host.com", requestWithProto));

    // Caso 5: request URI con schema http locale
    ServerHttpRequest requestLocal = mock(ServerHttpRequest.class);
    when(requestLocal.getHeaders()).thenReturn(new HttpHeaders());
    when(requestLocal.getURI()).thenReturn(URI.create("http://localhost:8081/v1/token"));
    assertEquals(
        "http://localhost:8081",
        URLUtils.resolveIssuer("localhost:8081", requestLocal));
  }

  @Test
  @DisplayName("isLocalOrInsecureHost correctly identifies local, private, and port 80 hosts")
  void testIsLocalOrInsecureHost() {
    assertTrue(URLUtils.isLocalOrInsecureHost("localhost"));
    assertTrue(URLUtils.isLocalOrInsecureHost("localhost:8081"));
    assertTrue(URLUtils.isLocalOrInsecureHost("127.0.0.1:8080"));
    assertTrue(URLUtils.isLocalOrInsecureHost("0.0.0.0"));
    assertTrue(URLUtils.isLocalOrInsecureHost("::1"));
    assertTrue(URLUtils.isLocalOrInsecureHost("[::1]:8080"));
    assertTrue(URLUtils.isLocalOrInsecureHost("service.localhost"));
    assertTrue(URLUtils.isLocalOrInsecureHost("dev.internal"));
    assertTrue(URLUtils.isLocalOrInsecureHost("company.lan"));
    assertTrue(URLUtils.isLocalOrInsecureHost("192.168.0.1:8080"));
    assertTrue(URLUtils.isLocalOrInsecureHost("10.0.1.25:9000"));
    assertTrue(URLUtils.isLocalOrInsecureHost("172.20.0.5:8080"));
    assertTrue(URLUtils.isLocalOrInsecureHost("my-site.com:80"));

    assertFalse(URLUtils.isLocalOrInsecureHost("my-site.com:443"));
    assertFalse(URLUtils.isLocalOrInsecureHost("access-sphere.giovannilamarmora.com"));
    assertFalse(URLUtils.isLocalOrInsecureHost(null));
    assertFalse(URLUtils.isLocalOrInsecureHost(""));
  }

  @Test
  @DisplayName("encodeURLValue and decodeURLValue encode and decode query parameters")
  void testEncodeDecodeURLValue() {
    String original = "redirect_uri=http://localhost:8080/cb?scope=openid email";
    String encoded = URLUtils.encodeURLValue(original);
    assertNotEquals(original, encoded);
    String decoded = URLUtils.decodeURLValue(encoded);
    assertEquals(original, decoded);

    assertNull(URLUtils.encodeURLValue(null));
    assertNull(URLUtils.decodeURLValue(null));
  }
}
