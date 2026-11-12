package com.hubspot.baragon.service.platform;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.Validator;

/**
 * What the running service hands to its own components: the object mapper its configuration is read
 * with, the metric and health check registries, the components to start and stop, and the admin
 * interface's servlets and tasks.
 */
public class Environment {
  private final String name;
  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final MetricRegistry metricRegistry;
  private final HealthCheckRegistry healthCheckRegistry;
  private final LifecycleEnvironment lifecycleEnvironment;
  private final AdminEnvironment adminEnvironment;

  public Environment(
    String name,
    ObjectMapper objectMapper,
    Validator validator,
    MetricRegistry metricRegistry
  ) {
    this.name = name;
    this.objectMapper = objectMapper;
    this.validator = validator;
    this.metricRegistry = metricRegistry;
    this.healthCheckRegistry = new HealthCheckRegistry();
    this.healthCheckRegistry.register("deadlocks", new ThreadDeadlockHealthCheck());
    this.lifecycleEnvironment = new LifecycleEnvironment();
    this.adminEnvironment = new AdminEnvironment();
  }

  public String getName() {
    return name;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public Validator getValidator() {
    return validator;
  }

  public MetricRegistry metrics() {
    return metricRegistry;
  }

  public HealthCheckRegistry healthChecks() {
    return healthCheckRegistry;
  }

  public void healthCheck(String name, HealthCheck healthCheck) {
    healthCheckRegistry.register(name, healthCheck);
  }

  public LifecycleEnvironment lifecycle() {
    return lifecycleEnvironment;
  }

  public AdminEnvironment admin() {
    return adminEnvironment;
  }
}
