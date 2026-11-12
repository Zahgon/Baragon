package com.hubspot.baragon.service.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Collects the entity of an answer before it is sent, so that every answer carries the length of its
 * entity instead of being sent in chunks. The API has always answered with a length, because the
 * framework it was written on collected the entity the same way, and a request asking for the
 * headers alone is told the length of the entity it did not ask for.
 */
public class EntityBufferingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    if (!(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }
    final boolean headersOnly =
      request instanceof HttpServletRequest &&
      "HEAD".equals(((HttpServletRequest) request).getMethod());
    final BufferedResponse buffered = new BufferedResponse(
      (HttpServletResponse) response
    );
    chain.doFilter(request, buffered);
    buffered.send(headersOnly);
  }

  /** An answer whose entity is collected until the whole of it is known. */
  private static final class BufferedResponse extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream entity = new ByteArrayOutputStream();
    private ServletOutputStream stream;
    private PrintWriter writer;
    private boolean errorSent;

    BufferedResponse(HttpServletResponse response) {
      super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() {
      if (writer != null) {
        throw new IllegalStateException("the answer is already being written as characters");
      }
      if (stream == null) {
        stream =
          new ServletOutputStream() {

            @Override
            public boolean isReady() {
              return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {}

            @Override
            public void write(int b) {
              entity.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
              entity.write(b, off, len);
            }
          };
      }
      return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      if (stream != null) {
        throw new IllegalStateException("the answer is already being written as bytes");
      }
      if (writer == null) {
        final String encoding = getCharacterEncoding();
        writer =
          new PrintWriter(
            new OutputStreamWriter(entity, encoding == null ? "ISO-8859-1" : encoding)
          );
      }
      return writer;
    }

    @Override
    public void flushBuffer() {
      // the answer is sent whole, so a request to flush it part-way is not one
    }

    @Override
    public void setContentLength(int length) {
      // the length of the collected entity is the one that is sent
    }

    @Override
    public void setContentLengthLong(long length) {
      // the length of the collected entity is the one that is sent
    }

    @Override
    public void reset() {
      super.reset();
      entity.reset();
    }

    @Override
    public void resetBuffer() {
      super.resetBuffer();
      entity.reset();
    }

    @Override
    public void sendError(int status) throws IOException {
      errorSent = true;
      super.sendError(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
      errorSent = true;
      super.sendError(status, message);
    }

    /**
     * Sends the collected entity, with its length; a caller asking only for the headers is told the
     * length of the entity without being sent it, and an answer the container wrote itself is left
     * alone.
     */
    void send(boolean headersOnly) throws IOException {
      if (errorSent) {
        return;
      }
      if (writer != null) {
        writer.flush();
      }
      final byte[] bytes = entity.toByteArray();
      if (!getResponse().isCommitted()) {
        super.setContentLength(bytes.length);
      }
      if (bytes.length > 0 && !headersOnly) {
        getResponse().getOutputStream().write(bytes);
      }
      getResponse().getOutputStream().flush();
    }
  }

  @Override
  public void init(FilterConfig config) {}

  @Override
  public void destroy() {}
}
