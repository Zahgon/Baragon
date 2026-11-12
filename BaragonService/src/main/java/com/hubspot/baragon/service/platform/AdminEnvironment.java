package com.hubspot.baragon.service.platform;

import com.hubspot.baragon.service.platform.tasks.Task;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;

/** What the service adds to its admin interface: servlets of its own and tasks. */
public class AdminEnvironment {

  /** A servlet of the admin interface together with the paths it answers on. */
  public static class ServletRegistration {
    private final String name;
    private final Servlet servlet;
    private final List<String> mappings = new ArrayList<>();

    ServletRegistration(String name, Servlet servlet) {
      this.name = name;
      this.servlet = servlet;
    }

    public ServletRegistration addMapping(String... urlPatterns) {
      for (String pattern : urlPatterns) {
        mappings.add(pattern);
      }
      return this;
    }

    public String getName() {
      return name;
    }

    public Servlet getServlet() {
      return servlet;
    }

    public List<String> getMappings() {
      return mappings;
    }
  }

  private final Map<String, ServletRegistration> servlets = new LinkedHashMap<>();
  private final Map<String, Task> tasks = new LinkedHashMap<>();

  public ServletRegistration addServlet(String name, Servlet servlet) {
    final ServletRegistration registration = new ServletRegistration(name, servlet);
    servlets.put(name, registration);
    return registration;
  }

  public void addTask(Task task) {
    tasks.put(task.getName(), task);
  }

  public Map<String, ServletRegistration> getServlets() {
    return servlets;
  }

  public Map<String, Task> getTasks() {
    return tasks;
  }
}
