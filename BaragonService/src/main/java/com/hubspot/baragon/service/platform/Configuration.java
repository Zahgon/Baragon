package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * What every configuration file of the service holds, whatever else it adds: the connectors to
 * answer on and the logging settings.
 */
public class Configuration {
  @Valid
  @NotNull
  @JsonProperty("server")
  private ServerFactory server = new DefaultServerFactory();

  @Valid
  @NotNull
  @JsonProperty("logging")
  private LoggingFactory logging = new LoggingFactory();

  public ServerFactory getServerFactory() {
    return server;
  }

  public void setServerFactory(ServerFactory server) {
    this.server = server;
  }

  public LoggingFactory getLoggingFactory() {
    return logging;
  }

  public void setLoggingFactory(LoggingFactory logging) {
    this.logging = logging;
  }
}
