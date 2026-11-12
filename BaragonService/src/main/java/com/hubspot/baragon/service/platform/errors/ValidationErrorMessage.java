package com.hubspot.baragon.service.platform.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The entity of an answer that refuses a request because of what it holds. */
public class ValidationErrorMessage {
  private final List<String> errors;

  public ValidationErrorMessage(List<String> errors) {
    this.errors = errors;
  }

  @JsonProperty("errors")
  public List<String> getErrors() {
    return errors;
  }
}
