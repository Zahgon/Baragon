package com.hubspot.baragon.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.hubspot.baragon.service.jackson.FuzzyEnumModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The two object mappers the service has always had: the one its own components read and write the
 * state store with, and the one its API's entities are read and written with. They are not the same
 * mapper and they do not know the same data types, so an entity the API answers with is written as the
 * second one writes it, whatever the first one could make of it.
 */
@Configuration
public class ObjectMapperConfiguration {
  /** The name the mapper of the API's entities is known by. */
  public static final String API_OBJECT_MAPPER = "baragon.api.objectMapper";

  /** The mapper the service's own components are given. */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new Jdk8Module());

    return objectMapper;
  }

  /** The mapper the API's entities are read and written with. */
  @Bean(name = API_OBJECT_MAPPER)
  public ObjectMapper apiObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JodaModule());
    objectMapper.registerModule(new AfterburnerModule());
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new FuzzyEnumModule());
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    return objectMapper;
  }
}
