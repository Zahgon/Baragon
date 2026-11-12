package com.hubspot.baragon.service;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.ConfigurationObjectMapper;
import com.hubspot.baragon.service.config.MergingConfigProvider;
import com.hubspot.baragon.service.platform.LoggingFactory;
import com.hubspot.baragon.service.platform.cli.Bootstrap;
import com.hubspot.baragon.service.platform.cli.CheckCommand;
import com.hubspot.baragon.service.platform.cli.Cli;
import com.hubspot.baragon.service.platform.cli.JarLocation;

public class BaragonService<T extends BaragonConfiguration> {
  private static final String BARAGON_DEFAULT_CONFIG_LOCATION =
    "baragonDefaultConfiguration";

  public String getName() {
    return "baragon";
  }

  /** Registers the commands the executable takes. */
  public void initialize(Bootstrap<T> bootstrap) {
    if (!Strings.isNullOrEmpty(System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION))) {
      bootstrap.setConfigurationSourceProvider(
        new MergingConfigProvider(
          bootstrap.getConfigurationSourceProvider(),
          System.getProperty(BARAGON_DEFAULT_CONFIG_LOCATION),
          bootstrap.getObjectMapper(),
          new YAMLFactory()
        )
      );
    }
    bootstrap.addCommand(new ServerCommand());
    bootstrap.addCommand(new CheckCommand<BaragonConfiguration>());
  }

  /** Runs the command the arguments name; answers false when no command could be run. */
  @SuppressWarnings("unchecked")
  public boolean run(String[] args) throws Exception {
    LoggingFactory.bootstrap();
    final Bootstrap<T> bootstrap = new Bootstrap<>(
      getName(),
      (Class<T>) BaragonConfiguration.class,
      ConfigurationObjectMapper.newObjectMapper()
    );
    initialize(bootstrap);
    return new Cli<>(new JarLocation(getClass()), bootstrap).run(args);
  }


  public static void main(String[] args) throws Exception {
    final BaragonService<BaragonConfiguration> service = new BaragonService<>();
    if (!service.run(args)) {
      System.exit(1);
    }
  }
}
