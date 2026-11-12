package com.hubspot.baragon.service.web;

import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.managers.BaragonAuthManager;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

/**
 * Whether a request carries a key the service knows. The key is read from the request's query
 * string, as the only place the service has ever read it from, and the first value it is given there
 * is the one that counts.
 */
public class AuthKeys {
  private final BaragonAuthManager authManager;
  private final AuthConfiguration authConfiguration;

  public AuthKeys(BaragonAuthManager authManager, AuthConfiguration authConfiguration) {
    this.authManager = authManager;
    this.authConfiguration = authConfiguration;
  }

  public boolean isEnabled() {
    return authConfiguration.isEnabled();
  }

  public boolean isAuthenticated(HttpServletRequest request) {
    return authManager.isAuthenticated(keyOf(request));
  }

  /** The first {@code authkey} of the request's query string, decoded. */
  @Nullable
  public static String keyOf(HttpServletRequest request) {
    final String query = request.getQueryString();
    if (query == null) {
      return null;
    }
    for (String pair : query.split("&")) {
      final int equals = pair.indexOf('=');
      final String name = equals < 0 ? pair : pair.substring(0, equals);
      if (!"authkey".equals(decode(name))) {
        continue;
      }
      return equals < 0 ? "" : decode(pair.substring(equals + 1));
    }
    return null;
  }

  private static String decode(String value) {
    try {
      return URLDecoder.decode(value, "UTF-8");
    } catch (UnsupportedEncodingException | IllegalArgumentException notEncoded) {
      return value;
    }
  }
}
