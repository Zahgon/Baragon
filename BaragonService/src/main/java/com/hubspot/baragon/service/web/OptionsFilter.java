package com.hubspot.baragon.service.web;

import com.hubspot.baragon.service.platform.errors.ErrorMessage;
import com.hubspot.baragon.service.platform.http.HttpHeaders;
import com.hubspot.baragon.service.platform.http.MediaType;
import com.hubspot.baragon.service.platform.http.WebApplicationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Answers the one request method the API answers itself rather than through a handler: an
 * {@code OPTIONS} request on a known path is told which methods that path takes, as a plain-text
 * list and in the {@code Allow} header. The key a request carries decides this answer as it decides
 * any other, and a key the service does not know is refused here too, whatever the handlers of the
 * path are marked as needing.
 */
public class OptionsFilter implements Filter {
  private final RouteTable routes;
  private final AuthKeys keys;
  private final ErrorEntities entities;

  public OptionsFilter(RouteTable routes, AuthKeys keys, ErrorEntities entities) {
    this.routes = routes;
    this.keys = keys;
    this.entities = entities;
  }

  @Override
  public void doFilter(
    ServletRequest servletRequest,
    ServletResponse servletResponse,
    FilterChain chain
  ) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    if (
      "OPTIONS".equals(request.getMethod()) &&
      routes != null &&
      !"*".equals(request.getRequestURI())
    ) {
      final Set<String> allowed = routes.optionsMethods(path(request));
      if (allowed != null) {
        if (keys.isEnabled() && !keys.isAuthenticated(request)) {
          entities.write(
            response,
            403,
            new ErrorMessage(403, WebApplicationException.defaultMessage(403))
          );
          return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.ALLOW, String.join(",", allowed));
        response.setContentType(MediaType.TEXT_PLAIN);
        final byte[] body = String.join(", ", allowed).getBytes(StandardCharsets.UTF_8);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
        return;
      }
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  /** The path the request names, read against the root the API answers under. */
  static String path(HttpServletRequest request) {
    final String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
    final String root = request.getContextPath() == null ? "" : request.getContextPath();
    final String path = uri.startsWith(root) ? uri.substring(root.length()) : uri;
    return path.isEmpty() ? "/" : path;
  }

  @Override
  public void init(FilterConfig config) {}

  @Override
  public void destroy() {}
}
