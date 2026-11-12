package com.hubspot.baragon.service.platform.http;

import javax.annotation.Nullable;

/**
 * Thrown by a handler to answer with a status of its choosing. Without a message of its own it
 * carries the one the API states for that status, {@code HTTP <code> <reason>}.
 */
public class WebApplicationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int status;
  private final Response response;

  public WebApplicationException(int status) {
    this(null, status);
  }

  public WebApplicationException(@Nullable String message, int status) {
    super(message == null ? defaultMessage(status) : message);
    this.status = status;
    this.response = null;
  }

  public WebApplicationException(@Nullable String message, Response.Status status) {
    this(message, status.getStatusCode());
  }

  public WebApplicationException(Response.Status status) {
    this(null, status.getStatusCode());
  }

  public WebApplicationException(Response response) {
    this(null, response);
  }

  public WebApplicationException(@Nullable String message, Response response) {
    super(message == null ? defaultMessage(response.getStatus()) : message);
    this.status = response.getStatus();
    this.response = response;
  }

  public int getStatus() {
    return status;
  }

  @Nullable
  public Response getResponse() {
    return response;
  }

  /** The message the API answers with when the thrower gave none. */
  public static String defaultMessage(int status) {
    final Response.Status named = Response.Status.fromStatusCode(status);
    return "HTTP " + status + " " + (named == null ? "" : named.getReasonPhrase());
  }
}
