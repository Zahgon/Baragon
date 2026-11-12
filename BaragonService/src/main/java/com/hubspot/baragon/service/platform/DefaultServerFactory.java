package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The full form of the {@code server} section: the API and the admin interface on connectors of their own. */
@JsonTypeName("default")
public class DefaultServerFactory extends ServerFactory {
  @JsonProperty
  private List<ConnectorFactory> applicationConnectors = new ArrayList<>(
    Arrays.asList(connector(8080))
  );

  @JsonProperty
  private List<ConnectorFactory> adminConnectors = new ArrayList<>(
    Arrays.asList(connector(8081))
  );

  @JsonProperty
  private String applicationContextPath = "/";

  @JsonProperty
  private String adminContextPath = "/";

  private static ConnectorFactory connector(int port) {
    final HttpConnectorFactory http = new HttpConnectorFactory();
    http.setPort(port);
    return http;
  }

  @Override
  public List<ConnectorFactory> getApplicationConnectors() {
    return applicationConnectors;
  }

  @Override
  public List<ConnectorFactory> getAdminConnectors() {
    return adminConnectors;
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
    return false;
  }
}
