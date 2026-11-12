package com.hubspot.baragon.service.web;

import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.platform.http.ForbiddenException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refuses a request that does not carry a key the service knows, once the path it names has been
 * found: a path no resource of the API answers for is told so before the key is ever looked at. The
 * handlers that are marked as needing no key are let through, and so is every request while the
 * service is configured without a key.
 */
public class AuthKeyInterceptor implements HandlerInterceptor {
  private final AuthKeys keys;

  public AuthKeyInterceptor(AuthKeys keys) {
    this.keys = keys;
  }

  @Override
  public boolean preHandle(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler
  ) {
    if (!keys.isEnabled() || !(handler instanceof HandlerMethod)) {
      return true;
    }
    final HandlerMethod method = (HandlerMethod) handler;
    if (
      method.getMethodAnnotation(NoAuth.class) != null ||
      method.getBeanType().getAnnotation(NoAuth.class) != null
    ) {
      return true;
    }
    if (!keys.isAuthenticated(request)) {
      throw new ForbiddenException();
    }
    return true;
  }
}
