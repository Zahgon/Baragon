package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/**
 * One entry of the {@code connector}, {@code applicationConnectors} or {@code adminConnectors}
 * property of the configuration file, named by its {@code type} property.
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "type",
  defaultImpl = HttpConnectorFactory.class
)
@JsonSubTypes({ @JsonSubTypes.Type(value = HttpConnectorFactory.class, name = "http") })
public interface ConnectorFactory {
  Connector build(Server server, String name);

  int getPort();
}
