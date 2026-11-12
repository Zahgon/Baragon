package com.hubspot.baragon.service.views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.hubspot.baragon.service.config.BaragonConfiguration;

public class IndexView {
  private final String appRoot;
  private final String staticRoot;
  private final String apiRoot;
  private final String title;
  private final boolean authEnabled;
  private final boolean elbEnabled;

  /** The name of the template this page is rendered from. */
  public static final String TEMPLATE = "index";

  public IndexView(
    String baragonUriBase,
    String appRoot,
    BaragonConfiguration configuration
  ) {
    checkNotNull(baragonUriBase, "baragonUriBase is null");

    String rawAppRoot = String.format("%s%s", baragonUriBase, appRoot);

    // TEMP
    this.appRoot =
      (rawAppRoot.endsWith("/"))
        ? rawAppRoot.substring(0, rawAppRoot.length() - 1)
        : rawAppRoot;
    this.staticRoot = String.format("%s/static", baragonUriBase);
    this.apiRoot = String.format("%s", baragonUriBase);
    this.title = configuration.getUiConfiguration().getTitle();
    this.authEnabled =
      (
        configuration.getAuthConfiguration().getKey().isPresent() &&
        configuration.getAuthConfiguration().isEnabled()
      );
    this.elbEnabled = configuration.getElbConfiguration().isPresent();
  }

  public String getAppRoot() {
    return appRoot;
  }

  public String getStaticRoot() {
    return staticRoot;
  }

  public String getApiRoot() {
    return apiRoot;
  }

  public String getTitle() {
    return title;
  }

  public boolean isAuthEnabled() {
    return authEnabled;
  }

  public boolean isElbEnabled() {
    return elbEnabled;
  }

  @Override
  public String toString() {
    return (
      "IndexView [appRoot=" +
      appRoot +
      ", staticRoot=" +
      staticRoot +
      ", apiRoot=" +
      apiRoot +
      ", title=" +
      title +
      ", authEnabled=" +
      authEnabled +
      ", elbEnabled" +
      elbEnabled +
      "]"
    );
  }
}
