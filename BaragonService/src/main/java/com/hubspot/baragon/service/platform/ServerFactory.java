package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code server} section of the configuration file, in either of the two forms it is written in:
 * the full form, which names the connectors of the API and the connectors of the admin interface
 * separately, and the short form, which names one connector that both answer on.
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "type",
  defaultImpl = DefaultServerFactory.class
)
@JsonSubTypes(
  {
    @JsonSubTypes.Type(value = DefaultServerFactory.class, name = "default"),
    @JsonSubTypes.Type(value = SimpleServerFactory.class, name = "simple")
  }
)
public abstract class ServerFactory {
  @JsonProperty
  private int maxThreads = 1024;

  @JsonProperty
  private int minThreads = 8;

  @JsonProperty
  private String rootPath = "/*";

  private final Map<String, Object> other = new LinkedHashMap<>();

  @JsonAnySetter
  public void set(String name, Object value) {
    other.put(name, value);
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public int getMinThreads() {
    return minThreads;
  }

  public String getRootPath() {
    return rootPath;
  }

  /** The connectors the API answers on. */
  public abstract List<ConnectorFactory> getApplicationConnectors();

  /** The connectors the admin interface answers on; empty when it shares the API's connector. */
  public abstract List<ConnectorFactory> getAdminConnectors();

  public abstract String getApplicationContextPath();

  public abstract String getAdminContextPath();

  /** Whether the admin interface answers on the API's own connector. */
  public abstract boolean isSharedConnector();
}
