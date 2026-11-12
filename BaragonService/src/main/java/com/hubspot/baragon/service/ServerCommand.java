package com.hubspot.baragon.service;

import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.platform.Environment;
import com.hubspot.baragon.service.platform.cli.Bootstrap;
import com.hubspot.baragon.service.platform.cli.ConfiguredCommand;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs the service: its API, its admin interface and everything they are built from. */
public class ServerCommand extends ConfiguredCommand<BaragonConfiguration> {
  private static final Logger LOG = LoggerFactory.getLogger(ServerCommand.class);

  public ServerCommand() {
    super("server", "Runs the Baragon service");
  }

  @Override
  protected void run(
    Bootstrap<BaragonConfiguration> bootstrap,
    Namespace namespace,
    BaragonConfiguration config
  ) {
    final Environment environment = new Environment(
      "baragon",
      bootstrap.getObjectMapper().copy(),
      bootstrap.getValidator(),
      bootstrap.getMetricRegistry()
    );

    final ConfigurableApplicationContext context = new SpringApplicationBuilder(
      BaragonApplication.class
    )
      .web(WebApplicationType.SERVLET)
      .bannerMode(Banner.Mode.OFF)
      .registerShutdownHook(true)
      .initializers(
        applicationContext -> {
          applicationContext.getBeanFactory().registerSingleton("baragonConfig", config);
          applicationContext
            .getBeanFactory()
            .registerSingleton("baragonEnvironment", environment);
        }
      )
      .properties(
        "spring.main.allow-bean-definition-overriding=true",
        "spring.mvc.throw-exception-if-no-handler-found=true",
        "spring.web.resources.add-mappings=false",
        "spring.jmx.enabled=false",
        "server.servlet.encoding.enabled=false",
        "server.servlet.context-path=" + contextPath(config),
        "server.jetty.threads.max=" + config.getServerFactory().getMaxThreads()
      )
      .run();

    registerHealthChecks(context, environment);
    try {
      environment.lifecycle().start();
    } catch (Exception e) {
      LOG.error("A component of the service did not start", e);
      context.close();
      throw new IllegalStateException(e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(environment.lifecycle()::stop));
  }

  /** The health checks and the components to start and stop that the running service holds. */
  private static void registerHealthChecks(
    ConfigurableApplicationContext context,
    Environment environment
  ) {
    context
      .getBeansOfType(com.codahale.metrics.health.HealthCheck.class)
      .forEach(
        (name, check) ->
          environment.healthChecks().register(check.getClass().getSimpleName(), check)
      );
    for (com.hubspot.baragon.service.platform.Managed managed : context
      .getBeansOfType(com.hubspot.baragon.service.platform.Managed.class)
      .values()) {
      environment.lifecycle().manage(managed);
    }
  }

  private static String contextPath(BaragonConfiguration config) {
    final String path = config.getServerFactory().getApplicationContextPath();
    return "/".equals(path) ? "" : path;
  }
}
