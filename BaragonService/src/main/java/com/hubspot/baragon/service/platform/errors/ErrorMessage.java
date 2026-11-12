package com.hubspot.baragon.service.platform.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.annotation.Nullable;

/** The entity of an error answer: a status code, a message and an optional detail. */
@JsonPropertyOrder({ "code", "message", "details" })
public class ErrorMessage {
  private final int code;
  private final String message;
  private final String details;

  public ErrorMessage(String message) {
    this(500, message, null);
  }

  public ErrorMessage(int code, String message) {
    this(code, message, null);
  }

  public ErrorMessage(int code, String message, @Nullable String details) {
    this.code = code;
    this.message = message;
    this.details = details;
  }

  @JsonProperty("code")
  public int getCode() {
    return code;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("details")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Nullable
  public String getDetails() {
    return details;
  }
}
