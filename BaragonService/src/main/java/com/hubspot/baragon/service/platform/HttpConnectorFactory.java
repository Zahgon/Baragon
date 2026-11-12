package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/** A connector that speaks plain HTTP. */
@JsonTypeName("http")
public class HttpConnectorFactory implements ConnectorFactory {
  @JsonProperty
  private int port = 8080;

  @JsonProperty
  private String bindHost;

  @JsonProperty
  private int headerCacheSize = 512;

  @JsonProperty
  private int outputBufferSize = 32768;

  @JsonProperty
  private int maxRequestHeaderSize = 8192;

  @JsonProperty
  private int maxResponseHeaderSize = 8192;

  @JsonProperty
  private boolean useForwardedHeaders;

  private final Map<String, Object> other = new LinkedHashMap<>();

  @JsonAnySetter
  public void set(String name, Object value) {
    other.put(name, value);
  }

  @Override
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getBindHost() {
    return bindHost;
  }

  public void setBindHost(String bindHost) {
    this.bindHost = bindHost;
  }

  /** The wire-level settings of the connector. */
  protected HttpConfiguration buildHttpConfiguration() {
    final HttpConfiguration http = new HttpConfiguration();
    http.setHeaderCacheSize(headerCacheSize);
    http.setOutputBufferSize(outputBufferSize);
    http.setRequestHeaderSize(maxRequestHeaderSize);
    http.setResponseHeaderSize(maxResponseHeaderSize);
    http.setSendDateHeader(true);
    http.setSendServerVersion(false);
    if (useForwardedHeaders) {
      http.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
    }
    return http;
  }

  @Override
  public Connector build(Server server, String name) {
    final ServerConnector connector = new ServerConnector(
      server,
      new HttpConnectionFactory(buildHttpConfiguration())
    );
    connector.setPort(port);
    if (bindHost != null) {
      connector.setHost(bindHost);
    }
    connector.setName(name);
    return connector;
  }
}
