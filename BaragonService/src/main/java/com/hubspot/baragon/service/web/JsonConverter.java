package com.hubspot.baragon.service.web;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Reads and writes the API's JSON entities, for the media types JSON has always been read and
 * written as. They are always written as UTF-8, whatever character set a caller named in the media
 * type it asked for, so a character set the parser cannot read is still one the API answers with.
 */
public class JsonConverter extends MappingJackson2HttpMessageConverter {
  private static final MediaType TEXT_JSON = MediaType.valueOf("text/json");

  public JsonConverter(ObjectMapper objectMapper) {
    super(objectMapper);
    setSupportedMediaTypes(
      Arrays.asList(
        MediaType.APPLICATION_JSON,
        TEXT_JSON,
        MediaType.valueOf("application/*+json")
      )
    );
  }

  @Override
  protected JsonEncoding getJsonEncoding(MediaType contentType) {
    return JsonEncoding.UTF8;
  }

  /** The character set of the media type asked for says nothing about what can be written. */
  @Override
  public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return super.canWrite(clazz, withoutCharset(mediaType));
  }

  private static MediaType withoutCharset(MediaType mediaType) {
    if (mediaType == null || mediaType.getCharset() == null) {
      return mediaType;
    }
    final Map<String, String> parameters = new LinkedHashMap<>(mediaType.getParameters());
    parameters.remove("charset");
    return new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters);
  }
}
