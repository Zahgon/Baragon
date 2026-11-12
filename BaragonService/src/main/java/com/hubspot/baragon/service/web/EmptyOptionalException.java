package com.hubspot.baragon.service.web;

/** Thrown when a handler answers with an optional value that holds nothing. */
public class EmptyOptionalException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public EmptyOptionalException() {
    super("the optional value holds nothing");
  }
}
