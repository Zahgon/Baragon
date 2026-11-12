package com.hubspot.baragon.service.platform.http;

import javax.annotation.Nullable;

/** Thrown to answer with 404. */
public class NotFoundException extends ClientErrorException {
  private static final long serialVersionUID = 1L;

  public NotFoundException() {
    super(null, 404);
  }

  public NotFoundException(@Nullable String message) {
    super(message, 404);
  }
}
