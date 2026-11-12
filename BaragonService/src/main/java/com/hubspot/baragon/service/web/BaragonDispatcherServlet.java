package com.hubspot.baragon.service.web;

import com.hubspot.baragon.service.platform.errors.ErrorMessage;
import com.hubspot.baragon.service.platform.http.WebApplicationException;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Dispatches every request as it is written. An {@code OPTIONS} request for a path no resource declares is
 * answered as any other request for it would be, rather than being told which methods the servlet itself
 * implements; a {@code HEAD} request reaches the handler of its own method, and the length of the entity it
 * did not ask for is the length the connector names; and a request target the container let through that is
 * not written as one of the API's paths names no resource of the API.
 */
public class BaragonDispatcherServlet extends DispatcherServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doOptions(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    processRequest(request, response);
  }

  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    processRequest(request, response);
  }

  @Override
  protected void doService(HttpServletRequest request, HttpServletResponse response)
    throws Exception {
    if (!namesAPath(request)) {
      final int status = 404;
      entities()
        .write(
          response,
          status,
          new ErrorMessage(status, WebApplicationException.defaultMessage(status))
        );
      return;
    }
    super.doService(request, response);
  }

  /** Whether the request target is written as a path under the root the API answers on. */
  private static boolean namesAPath(HttpServletRequest request) {
    final String root = request.getContextPath();
    final String target = request.getRequestURI();
    return root == null || root.isEmpty() || (target != null && target.startsWith(root));
  }

  /** How the API writes the entity of an answer it makes without reaching a handler. */
  private ErrorEntities entities() {
    final org.springframework.web.context.WebApplicationContext context = getWebApplicationContext();
    if (context == null) {
      throw new IllegalStateException("the API is not built yet");
    }
    return context.getBean(ErrorEntities.class);
  }
}
