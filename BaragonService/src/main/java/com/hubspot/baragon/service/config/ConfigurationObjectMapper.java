package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.hubspot.baragon.service.jackson.FuzzyEnumModule;

/**
 * The object mapper a configuration file of the service is read with: the data types such a file is
 * written in, enumerated values read however they are written, and a reading that passes over what the
 * service does not know. It is not the mapper the API's own entities are written with.
 */
public final class ConfigurationObjectMapper {

  private ConfigurationObjectMapper() {}

  public static ObjectMapper newObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JodaModule());
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new FuzzyEnumModule());
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return objectMapper;
  }
}
