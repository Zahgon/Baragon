package com.hubspot.baragon.service.web;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;

/**
 * Reads the numbers a path or query parameter holds exactly as they are written: text with anything
 * else in it is not a number, and the API answers such a request as it always has. A value of nothing
 * at all, or of blanks, is no value, and the number read from it is the one a request that does not
 * carry the parameter is read as carrying.
 */
public final class StrictNumberConverters {

  private StrictNumberConverters() {}

  public static void register(FormatterRegistry registry) {
    registry.addConverter(
      new Converter<String, Long>() {

        @Override
        public Long convert(String source) {
          return source.trim().isEmpty() ? Long.valueOf(0L) : Long.valueOf(source);
        }
      }
    );
    registry.addConverter(
      new Converter<String, Integer>() {

        @Override
        public Integer convert(String source) {
          return source.trim().isEmpty() ? Integer.valueOf(0) : Integer.valueOf(source);
        }
      }
    );
    registry.addConverter(
      new Converter<String, Boolean>() {

        @Override
        public Boolean convert(String source) {
          return Boolean.valueOf(source);
        }
      }
    );
  }
}
