package com.hubspot.baragon.service.platform.http;

import javax.annotation.Nullable;

/** Thrown to answer with 403. */
public class ForbiddenException extends ClientErrorException {
  private static final long serialVersionUID = 1L;

  public ForbiddenException() {
    super(null, 403);
  }

  public ForbiddenException(@Nullable String message) {
    super(message, 403);
  }
}
