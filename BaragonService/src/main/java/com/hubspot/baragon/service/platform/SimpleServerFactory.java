package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The short form of the {@code server} section: one connector, which both the API and the admin
 * interface answer on, each under a root path of its own.
 */
@JsonTypeName("simple")
public class SimpleServerFactory extends ServerFactory {
  @JsonProperty
  private ConnectorFactory connector = defaultConnector();

  @JsonProperty
  private String applicationContextPath = "/application";

  @JsonProperty
  private String adminContextPath = "/admin";

  private static ConnectorFactory defaultConnector() {
    final HttpConnectorFactory http = new HttpConnectorFactory();
    http.setPort(8080);
    return http;
  }

  public ConnectorFactory getConnector() {
    return connector;
  }

  @Override
  public List<ConnectorFactory> getApplicationConnectors() {
    return Arrays.asList(connector);
  }

  @Override
  public List<ConnectorFactory> getAdminConnectors() {
    return Collections.emptyList();
  }

  @Override
  public String getApplicationContextPath() {
    return applicationContextPath;
  }

  @Override
  public String getAdminContextPath() {
    return adminContextPath;
  }

  @Override
  public boolean isSharedConnector() {
    return true;
  }
}
