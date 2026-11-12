package com.hubspot.baragon.service.web;

import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.platform.AdminEnvironment;
import com.hubspot.baragon.service.platform.ConnectorFactory;
import com.hubspot.baragon.service.platform.Environment;
import com.hubspot.baragon.service.platform.tasks.GarbageCollectionTask;
import com.hubspot.baragon.service.platform.tasks.LogConfigurationTask;
import com.hubspot.baragon.service.platform.tasks.Task;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * The admin interface: the operational menu and the metric, health check, thread dump and ping pages,
 * the tasks the service takes while it runs, and the servlets the service adds itself. It runs beside
 * the application connectors, on the ports the configuration file names.
 */
public class AdminServer implements SmartLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(AdminServer.class);

  private final BaragonConfiguration config;
  private final Environment environment;
  private Server server;

  public AdminServer(BaragonConfiguration config, Environment environment) {
    this.config = config;
    this.environment = environment;
  }

  @Override
  public void start() {
    final List<ConnectorFactory> connectorFactories = config
      .getServerFactory()
      .getAdminConnectors();
    if (connectorFactories.isEmpty()) {
      return;
    }
    server = new Server();
    for (int i = 0; i < connectorFactories.size(); i++) {
      server.addConnector(connectorFactories.get(i).build(server, "admin-" + i));
    }
    server.setHandler(JettyConfiguration.compressing(context()));
    try {
      server.start();
    } catch (Exception e) {
      throw new IllegalStateException("the admin interface did not start", e);
    }
  }

  /** The pages, the tasks and the servlets of the admin interface, under the path it answers on. */
  public ServletContextHandler context() {
    final ServletContextHandler context = new ServletContextHandler();
    context.setContextPath(config.getServerFactory().getAdminContextPath());
    context.setAttribute(MetricsServlet.METRICS_REGISTRY, environment.metrics());
    context.setAttribute(
      HealthCheckServlet.HEALTH_CHECK_REGISTRY,
      environment.healthChecks()
    );
    context.addFilter(
      new FilterHolder(new RefusedMethodFilter()),
      "/*",
      EnumSet.of(DispatcherType.REQUEST)
    );
    context.addServlet(new ServletHolder(new AdminServlet()), "/*");

    final com.hubspot.baragon.service.platform.tasks.TaskServlet tasks = new com.hubspot.baragon.service.platform.tasks.TaskServlet();
    tasks.add(new GarbageCollectionTask());
    tasks.add(new LogConfigurationTask());
    for (Task task : environment.admin().getTasks().values()) {
      tasks.add(task);
    }
    context.addServlet(new ServletHolder(tasks), "/tasks/*");

    for (Map.Entry<String, AdminEnvironment.ServletRegistration> entry : environment
      .admin()
      .getServlets()
      .entrySet()) {
      final ServletHolder holder = new ServletHolder(
        entry.getKey(),
        entry.getValue().getServlet()
      );
      for (String mapping : entry.getValue().getMappings()) {
        context.addServlet(holder, mapping);
      }
    }
    return context;
  }

  @Override
  public void stop() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        LOG.warn("Unable to stop the admin interface", e);
      }
      server = null;
    }
  }

  @Override
  public boolean isRunning() {
    return server != null && server.isRunning();
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 1;
  }
}
