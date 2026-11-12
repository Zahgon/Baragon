package com.hubspot.baragon.service.platform.http;

/** The names of the headers the service reads and writes by name. */
public final class HttpHeaders {
  public static final String ACCEPT = "Accept";
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  public static final String ALLOW = "Allow";
  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String ETAG = "ETag";
  public static final String IF_NONE_MATCH = "If-None-Match";
  public static final String LOCATION = "Location";
  public static final String VARY = "Vary";

  private HttpHeaders() {}
}
