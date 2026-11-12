package com.hubspot.baragon.service.platform.http;

import javax.annotation.Nullable;

/** Thrown by a handler to answer with a status in the 4xx range. */
public class ClientErrorException extends WebApplicationException {
  private static final long serialVersionUID = 1L;

  public ClientErrorException(int status) {
    super(null, status);
  }

  public ClientErrorException(@Nullable String message, int status) {
    super(message, status);
  }

  public ClientErrorException(@Nullable String message, Response response) {
    super(message, response);
  }
}
