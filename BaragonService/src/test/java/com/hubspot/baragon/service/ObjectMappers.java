package com.hubspot.baragon.service;

import com.fasterxml.jackson.databind.ObjectMapper;

/** The object mapper the API is serialised with, for a test to read answers with. */
public final class ObjectMappers {

  private ObjectMappers() {}

  public static ObjectMapper forApi() {
    return new ObjectMapperConfiguration().objectMapper();
  }
}
