package com.hubspot.baragon.service.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

/**
 * Reads a request as the API has always read it. A request whose {@code Content-Type} is not a media
 * type at all is refused by the container, before anything of the request is read. A request that
 * carries no entity at all says nothing about the media type it carries, so it reaches the handler of
 * its path whatever that handler reads, and is answered by whatever the handler makes of an entity of
 * nothing. A parameter a request names more than once is read as the first of the values it was given.
 */
public class RequestReadingFilter implements Filter {
  /** Marks a request that said nothing about the media type of what it carries. */
  public static final String SAID_NOTHING = "baragon.request.saidNothing";


  @Override
  public void doFilter(
    ServletRequest servletRequest,
    ServletResponse servletResponse,
    FilterChain chain
  ) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final String contentType = request.getContentType();
    if (contentType != null) {
      try {
        MediaType.parseMediaType(contentType);
      } catch (RuntimeException notAMediaType) {
        ((HttpServletResponse) servletResponse).sendError(
            HttpServletResponse.SC_BAD_REQUEST
          );
        return;
      }
    }
    if (contentType == null) {
      request.setAttribute(SAID_NOTHING, Boolean.TRUE);
    }
    chain.doFilter(new AsRead(request, contentType == null), servletResponse);
  }

  /** Whether the request carries an entity at all, whatever it says about it. */
  public static boolean carriesEntity(HttpServletRequest request) {
    final String framing = request.getHeader("Transfer-Encoding");
    return request.getContentLengthLong() > 0 || framing != null;
  }

  @Override
  public void init(FilterConfig config) {}

  @Override
  public void destroy() {}

  /** The request the API reads: the entity it says it carries, and the first value of each parameter. */
  private static final class AsRead extends HttpServletRequestWrapper {
    /** The media type the API reads, which a request that says nothing is read as carrying. */
    private static final String READ_AS = MediaType.APPLICATION_JSON_VALUE;

    private final boolean saysNothing;

    AsRead(HttpServletRequest request, boolean saysNothing) {
      super(request);
      this.saysNothing = saysNothing;
    }

    @Override
    public String getContentType() {
      return saysNothing ? READ_AS : super.getContentType();
    }

    @Override
    public String getHeader(String name) {
      return saysNothing && "content-type".equalsIgnoreCase(name)
        ? READ_AS
        : super.getHeader(name);
    }

    @Override
    public String[] getParameterValues(String name) {
      final String[] values = super.getParameterValues(name);
      return values == null || values.length < 2 ? values : new String[] { values[0] };
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      final Map<String, String[]> first = new LinkedHashMap<>();
      super
        .getParameterMap()
        .forEach(
          (name, values) ->
            first.put(
              name,
              values == null || values.length < 2 ? values : new String[] { values[0] }
            )
        );
      return first;
    }
  }
}
