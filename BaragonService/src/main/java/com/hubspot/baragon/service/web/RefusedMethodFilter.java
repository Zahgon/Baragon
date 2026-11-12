package com.hubspot.baragon.service.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
 * Refuses the request methods the API answers for no path at all, without an entity and without
 * anything else of the answer. How the answer is framed is left to the connector, which frames an
 * answer of no content as it frames any other.
 */
public class RefusedMethodFilter implements Filter {
  /** The request methods the API answers at all; every other one is refused without an entity. */
  private static final Set<String> ANSWERED_METHODS = new HashSet<>(
    Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")
  );

  @Override
  public void doFilter(
    ServletRequest servletRequest,
    ServletResponse servletResponse,
    FilterChain chain
  ) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    if (!ANSWERED_METHODS.contains(request.getMethod())) {
      response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void init(FilterConfig config) {}

  @Override
  public void destroy() {}
}
