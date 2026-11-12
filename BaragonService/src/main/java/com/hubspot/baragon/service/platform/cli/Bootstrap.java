package com.hubspot.baragon.service.platform.cli;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.baragon.service.platform.Configuration;
import com.hubspot.baragon.service.platform.ConfigurationFactory;
import com.hubspot.baragon.service.platform.ConfigurationSourceProvider;
import com.hubspot.baragon.service.platform.FileConfigurationSourceProvider;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * What the executable has set up before a command runs: the name it answers to, the object mapper
 * its configuration is read with, where configuration files are read from, the metric registry and
 * the commands it takes.
 *
 * @param <T> the configuration type of the service
 */
public class Bootstrap<T extends Configuration> {
  private final String name;
  private final Class<T> configurationClass;
  private final ObjectMapper objectMapper;
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private final List<Command> commands = new ArrayList<>();
  private final Validator validator = Validation
    .buildDefaultValidatorFactory()
    .getValidator();

  private ConfigurationSourceProvider configurationSourceProvider = new FileConfigurationSourceProvider();
  private T configuration;
  private String configurationPath;

  public Bootstrap(String name, Class<T> configurationClass, ObjectMapper objectMapper) {
    this.name = name;
    this.configurationClass = configurationClass;
    this.objectMapper = objectMapper;
  }

  public String getName() {
    return name;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  public Validator getValidator() {
    return validator;
  }

  public ConfigurationSourceProvider getConfigurationSourceProvider() {
    return configurationSourceProvider;
  }

  public void setConfigurationSourceProvider(ConfigurationSourceProvider provider) {
    this.configurationSourceProvider = provider;
  }

  public void addCommand(Command command) {
    commands.add(command);
  }

  public List<Command> getCommands() {
    return commands;
  }

  /**
   * Reads the configuration the command line names, once per run of the executable; a command line that
   * names no file is given the configuration the service runs on by default.
   */
  public synchronized T configuration(String path) throws java.io.IOException {
    if (configuration == null || !java.util.Objects.equals(path, configurationPath)) {
      final ConfigurationFactory<T> factory = new ConfigurationFactory<>(
        configurationClass,
        objectMapper,
        validator
      );
      configuration =
        path == null ? factory.build() : factory.build(configurationSourceProvider, path);
      configurationPath = path;
    }
    return configuration;
  }

  public synchronized String getConfigurationPath() {
    return configurationPath;
  }
}
