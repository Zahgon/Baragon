package com.hubspot.baragon.service.web;

import java.util.Comparator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Chooses the handler of a request as the API has always chosen it: the most exactly written of the
 * paths that match decides, and a request whose method that path does not declare is refused there,
 * rather than being answered by a path written less exactly. Of the handlers of one path, the one
 * that names the request's own method answers it.
 */
public class BaragonHandlerMapping extends RequestMappingHandlerMapping {
  private volatile RouteTable routes;

  /** The paths of the API and the methods each declares, read once they are all registered. */
  public RouteTable routes() {
    RouteTable table = routes;
    if (table == null) {
      synchronized (this) {
        table = routes;
        if (table == null) {
          table = new RouteTable(this);
          routes = table;
        }
      }
    }
    return table;
  }

  @Override
  protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request)
    throws Exception {
    final Set<String> declared = routes().declaredMethods(lookupPath);
    final String method = request.getMethod();
    if (
      declared != null &&
      !declared.contains(method) &&
      !("HEAD".equals(method) && declared.contains("GET")) &&
      !"OPTIONS".equals(method)
    ) {
      throw new HttpRequestMethodNotSupportedException(
        method,
        routes().allowedMethods(lookupPath)
      );
    }
    return super.lookupHandlerMethod(lookupPath, request);
  }

  @Override
  protected Comparator<RequestMappingInfo> getMappingComparator(HttpServletRequest request) {
    final Comparator<RequestMappingInfo> inherited = super.getMappingComparator(request);
    final String method = request.getMethod();
    return (left, right) -> {
      final int named = Boolean.compare(names(right, method), names(left, method));
      return named != 0 ? named : inherited.compare(left, right);
    };
  }

  /** Whether the handler of a mapping names this request method itself. */
  private static boolean names(RequestMappingInfo mapping, String method) {
    for (RequestMethod declared : mapping.getMethodsCondition().getMethods()) {
      if (declared.name().equals(method)) {
        return true;
      }
    }
    return false;
  }
}
