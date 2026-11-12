package com.hubspot.baragon.service.web;

import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.platform.ConnectorFactory;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * Builds the application connectors the configuration file names, replacing the single connector the
 * web server starts with, compresses answers as the service has always compressed them, and leaves
 * the API to answer for every path, as it always has.
 */
public class JettyConfiguration
  implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {
  /** The smallest answer that is worth compressing. */
  private static final int MINIMUM_ENTITY_SIZE = 256;
  /** How much of a compressed request entity is read at a time. */
  private static final int INFLATE_BUFFER_SIZE = 8192;

  private final BaragonConfiguration config;

  public JettyConfiguration(BaragonConfiguration config) {
    this.config = config;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    final List<ConnectorFactory> connectorFactories = new ArrayList<>(
      config.getServerFactory().getApplicationConnectors()
    );
    if (!connectorFactories.isEmpty()) {
      factory.setPort(connectorFactories.get(0).getPort());
    }
    factory.addServerCustomizers(
      server -> {
        applyThreadPool(server);
        asTheContainerAnswers(server);
        final List<Connector> connectors = new ArrayList<>();
        for (int i = 0; i < connectorFactories.size(); i++) {
          connectors.add(connectorFactories.get(i).build(server, "application-" + i));
        }
        for (Connector existing : server.getConnectors()) {
          server.removeConnector(existing);
        }
        connectors.forEach(server::addConnector);
        server.setHandler(compressing(server.getHandler()));
      }
    );
  }

  /**
   * The API answers for every path, including the ones a web application would keep to itself, and a
   * failure it does not answer itself is told by the container in the container's own words.
   */
  private static void asTheContainerAnswers(Server server) {
    final WebAppContext context = server.getChildHandlerByClass(WebAppContext.class);
    if (context == null) {
      return;
    }
    context.setProtectedTargets(new String[0]);
    // the web server puts an error handler of its own on the context as the context starts, so the
    // container's own goes on once that has happened
    context.addLifeCycleListener(
      new AbstractLifeCycle.AbstractLifeCycleListener() {

        @Override
        public void lifeCycleStarted(LifeCycle event) {
          final ErrorHandler errors = new ErrorHandler();
          errors.setShowStacks(false);
          context.setErrorHandler(errors);
        }
      }
    );
  }

  private void applyThreadPool(Server server) {
    if (server.getThreadPool() instanceof QueuedThreadPool) {
      final QueuedThreadPool pool = (QueuedThreadPool) server.getThreadPool();
      pool.setMaxThreads(config.getServerFactory().getMaxThreads());
      pool.setMinThreads(config.getServerFactory().getMinThreads());
    }
  }

  /** Wraps the handler so that answers are compressed when the caller takes them. */
  static Handler compressing(Handler handler) {
    final GzipHandler gzip = new GzipHandler();
    gzip.setMinGzipSize(MINIMUM_ENTITY_SIZE);
    gzip.setInflateBufferSize(INFLATE_BUFFER_SIZE);
    // no user agent is excluded, so an answer varies by Accept-Encoding alone
    gzip.setExcludedAgentPatterns();
    gzip.setHandler(handler);
    return gzip;
  }
}
