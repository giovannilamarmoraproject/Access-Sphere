package io.github.giovannilamarmora.accesssphere.utilities;

import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Classe di utilita centralizzata per la manipolazione, normalizzazione e risoluzione degli URL e
 * domini applicativi. Fornisce metodi riutilizzabili per:
 *
 * <ul>
 *   <li>Risoluzione dinamica del protocollo (HTTP in chiaro vs HTTPS sicuro) in base alla natura
 *       dell'host (localhost, IP privati, domini remoti)
 *   <li>Risoluzione del base URL applicativo o issuer dei token JWT
 *   <li>Encoding e Decoding URL standard UTF-8
 * </ul>
 *
 * Interconnessa con {@code ProjectInitializerService}, {@code JWTData}, e {@code Utils}.
 */
public final class URLUtils {

  /**
   * Risolve l'URL base dell'applicazione a partire dalla stringa di configurazione del dominio. Il
   * dominio applicativo e obbligatorio e deve essere configurato (es. in 'application.yml' con la
   * chiave 'app.domain'). Se il dominio e nullo o non valorizzato, lancia una {@link
   * IllegalArgumentException}.
   *
   * @param domain Dominio o URL dell'applicazione configurato
   * @return URL base normalizzato comprensivo di schema (http o https) e privo di trailing slash
   * @throws IllegalArgumentException se il dominio e nullo o vuoto
   */
  public static String resolveBaseUrl(String domain) {
    if (!StringUtils.hasText(domain)) {
      throw new IllegalArgumentException(
          "The application domain must be configured in application.yml (property: app.domain)");
    }
    return resolveUrlWithScheme(domain);
  }

  /**
   * Normalizza un indirizzo host o URL garantendo la presenza del protocollo corretto ('http://' o
   * 'https://') e rimuovendo eventuali slash finali superflui. Se lo schema e gia presente
   * ('http://' o 'https://'), lo preserva inalterato. Se lo schema e omesso, assegna 'http://' per
   * host locali o porta 80, altrimenti assegna 'https://'.
   *
   * @param hostOrUrl Stringa host o URL da normalizzare
   * @return URL normalizzato con schema, oppure null se la stringa in input e nulla o vuota
   */
  public static String resolveUrlWithScheme(String hostOrUrl) {
    if (ObjectToolkit.isNullOrEmpty(hostOrUrl)) {
      return null;
    }
    String cleaned = hostOrUrl.trim().replaceAll("/+$", "");
    if (cleaned.isEmpty()) {
      return null;
    }
    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
      return cleaned;
    }
    String scheme = isLocalOrInsecureHost(cleaned) ? "http://" : "https://";
    return scheme + cleaned;
  }

  /**
   * Risolve l'issuer o URL di contesto basandosi sull'indirizzo remoto ed eventualmente sulla
   * richiesta HTTP. Se e disponibile una {@link ServerHttpRequest}, controlla l'header
   * 'X-Forwarded-Proto' o lo schema della richiesta.
   *
   * @param remoteAddress Indirizzo host/porta remoto o configurato
   * @param request Richiesta HTTP reattiva (opzionale)
   * @return URL dell'issuer completo con schema corretto
   */
  public static String resolveIssuer(String remoteAddress, ServerHttpRequest request) {
    if (ObjectToolkit.isNullOrEmpty(remoteAddress)) {
      return null;
    }
    String cleaned = remoteAddress.trim().replaceAll("/+$", "");
    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
      return cleaned;
    }
    if (request != null) {
      String forwardedProto = request.getHeaders().getFirst("X-Forwarded-Proto");
      if (StringUtils.hasText(forwardedProto)) {
        return forwardedProto.trim().toLowerCase() + "://" + cleaned;
      }
      if (request.getURI() != null && StringUtils.hasText(request.getURI().getScheme())) {
        String scheme = request.getURI().getScheme();
        if ("https".equalsIgnoreCase(scheme) || isLocalOrInsecureHost(cleaned)) {
          return scheme.toLowerCase() + "://" + cleaned;
        }
      }
    }
    return resolveUrlWithScheme(cleaned);
  }

  /**
   * Determina se l'host specificato rappresenta un ambiente locale o che richiede protocollo HTTP
   * in chiaro. Riconosce:
   *
   * <ul>
   *   <li>Porta esplicita 80 (HTTP standard)
   *   <li>Host localhost o loopback IPv4/IPv6 (127.0.0.1, 0.0.0.0, ::1)
   *   <li>Suffissi locali (.localhost, .local, .internal, .lan)
   *   <li>Classi di indirizzi IP privati (10.x.x.x, 192.168.x.x, 172.16-31.x.x)
   * </ul>
   *
   * @param domain Dominio o stringa host:port da verificare
   * @return true se l'host e locale o non sicuro, false per domini remoti sicuri
   */
  public static boolean isLocalOrInsecureHost(String domain) {
    if (!StringUtils.hasText(domain)) {
      return false;
    }
    String host = domain.trim();
    if (host.contains("/")) {
      host = host.substring(0, host.indexOf('/'));
    }
    String port = null;
    if (host.startsWith("[")) {
      int closeBracket = host.indexOf(']');
      if (closeBracket != -1) {
        if (host.length() > closeBracket + 1 && host.charAt(closeBracket + 1) == ':') {
          port = host.substring(closeBracket + 2);
        }
        host = host.substring(1, closeBracket);
      }
    } else if (host.indexOf(':') != -1 && host.indexOf(':') == host.lastIndexOf(':')) {
      port = host.substring(host.indexOf(':') + 1);
      host = host.substring(0, host.indexOf(':'));
    }

    if ("80".equals(port)) {
      return true;
    }
    if ("443".equals(port)) {
      return false;
    }

    String lowerHost = host.toLowerCase();
    return lowerHost.equals("localhost")
        || lowerHost.equals("127.0.0.1")
        || lowerHost.equals("0.0.0.0")
        || lowerHost.equals("::1")
        || lowerHost.endsWith(".localhost")
        || lowerHost.endsWith(".local")
        || lowerHost.endsWith(".internal")
        || lowerHost.endsWith(".lan")
        || lowerHost.startsWith("192.168.")
        || lowerHost.startsWith("10.")
        || lowerHost.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
  }

  /**
   * Esegue l'URL encoding della stringa specificata utilizzando UTF-8.
   *
   * @param value Valore da codificare
   * @return Valore codificato in URL o il valore originario se nullo/vuoto
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String encodeURLValue(String value) {
    return ObjectUtils.isEmpty(value) ? value : URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * Esegue l'URL decoding della stringa specificata utilizzando UTF-8.
   *
   * @param value Valore da decodificare
   * @return Valore decodificato da URL o il valore originario se nullo/vuoto
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String decodeURLValue(String value) {
    return ObjectUtils.isEmpty(value) ? value : URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
