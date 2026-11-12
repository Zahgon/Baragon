package com.hubspot.baragon.service.platform.tasks;

import com.google.common.collect.ImmutableMultimap;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lists the tasks of the admin interface and runs the one a request names. */
public class TaskServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(TaskServlet.class);

  private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();

  public void add(Task task) {
    tasks.put('/' + task.getName(), task);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, javax.servlet.ServletException {
    final String path = request.getPathInfo();
    if (path != null) {
      refuse(path, request, response, false);
      return;
    }
    response.setContentType("text/plain");
    response.getWriter().print(listing());
  }

  /**
   * A request for the headers of the listing alone: its length, and the character set the connector
   * names on the answers it writes itself, rather than the one it writes the listing in.
   */
  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response)
    throws IOException, javax.servlet.ServletException {
    final String path = request.getPathInfo();
    if (path != null) {
      refuse(path, request, response, true);
      return;
    }
    response.setContentType("text/plain");
    response.setCharacterEncoding("utf-8");
    response.setContentLength(listing().getBytes(StandardCharsets.UTF_8).length);
  }

  /**
   * A path under the listing: a task of that name is one this request method cannot ask for, and a name that
   * is no task at all is a path the listing does not have.
   */
  private void refuse(
    String path,
    HttpServletRequest request,
    HttpServletResponse response,
    boolean headersOnly
  ) throws IOException, javax.servlet.ServletException {
    if (!tasks.containsKey(path)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if (headersOnly) {
      super.doHead(request, response);
    } else {
      super.doGet(request, response);
    }
  }

  /** The names of the tasks of the admin interface, one to a line. */
  private String listing() {
    final StringBuilder names = new StringBuilder();
    for (Map.Entry<String, Task> entry : new TreeMap<>(tasks).entrySet()) {
      names.append(entry.getKey().substring(1)).append('\n');
    }
    return names.toString();
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    final String path = request.getPathInfo();
    final Task task = path == null ? null : tasks.get(path);
    if (task == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    response.setContentType("text/plain");
    response.setCharacterEncoding("utf-8");
    final SentAsWritten sent = new SentAsWritten(response.getWriter());
    final PrintWriter output = new PrintWriter(sent);
    try {
      task.execute(parameters(request), output);
      // the answer of a task is sent as the task writes it
      output.flush();
    } catch (Exception e) {
      LOG.error("Error running {}", task.getName(), e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      output.println();
      e.printStackTrace(output);
    } finally {
      output.close();
    }
  }

  /**
   * Passes on what a task writes, and passes on a request to send it only once something has been
   * written: the answer of a task that says nothing names a length of nothing, and the answer of one
   * that says something is sent as it is written.
   */
  private static final class SentAsWritten extends FilterWriter {
    private boolean wrote;

    SentAsWritten(Writer out) {
      super(out);
    }

    @Override
    public void write(int c) throws IOException {
      wrote = true;
      super.write(c);
    }

    @Override
    public void write(char[] buffer, int off, int len) throws IOException {
      wrote = wrote || len > 0;
      super.write(buffer, off, len);
    }

    @Override
    public void write(String text, int off, int len) throws IOException {
      wrote = wrote || len > 0;
      super.write(text, off, len);
    }

    @Override
    public void flush() throws IOException {
      if (wrote) {
        super.flush();
      }
    }
  }

  private static ImmutableMultimap<String, String> parameters(HttpServletRequest request) {
    final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      for (String value : entry.getValue()) {
        builder.put(entry.getKey(), value);
      }
    }
    return builder.build();
  }
}
