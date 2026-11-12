package com.hubspot.baragon.service.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.StreamUtils;

/**
 * Reads and writes the answers of the API that are plain text. The media type names no character set,
 * as the API has always named it on such an answer, and the text is written as UTF-8 either way.
 */
public class PlainTextConverter extends AbstractHttpMessageConverter<String> {

  public PlainTextConverter() {
    super(MediaType.TEXT_PLAIN, MediaType.ALL);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return String.class == clazz;
  }

  @Override
  protected String readInternal(Class<? extends String> clazz, HttpInputMessage message)
    throws IOException {
    return StreamUtils.copyToString(message.getBody(), StandardCharsets.UTF_8);
  }

  @Override
  protected Long getContentLength(String text, MediaType contentType) {
    return (long) text.getBytes(StandardCharsets.UTF_8).length;
  }

  /** The media type of the answer, without a character set of its own. */
  @Override
  protected void addDefaultHeaders(
    org.springframework.http.HttpHeaders headers,
    String text,
    MediaType contentType
  ) throws IOException {
    super.addDefaultHeaders(headers, text, contentType);
    final MediaType written = headers.getContentType();
    if (written != null && written.getCharset() != null) {
      headers.setContentType(new MediaType(written.getType(), written.getSubtype()));
    }
  }

  @Override
  protected void writeInternal(String text, HttpOutputMessage message) throws IOException {
    message.getBody().write(text.getBytes(StandardCharsets.UTF_8));
  }
}
