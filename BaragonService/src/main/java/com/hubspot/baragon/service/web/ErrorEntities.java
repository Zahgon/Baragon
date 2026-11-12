package com.hubspot.baragon.service.web;

import com.hubspot.baragon.service.platform.http.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;

/** Writes the entity of an error answer through the converters every other answer is written with. */
public class ErrorEntities {
  private final List<HttpMessageConverter<?>> converters;

  public ErrorEntities(List<HttpMessageConverter<?>> converters) {
    this.converters = converters;
  }

  /** Writes the answer whole: its status, the media type of the API's error entities and the entity. */
  public void write(HttpServletResponse response, int status, Object message)
    throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(status);
    final byte[] entity = bytes(message);
    response.setContentType(MediaType.APPLICATION_JSON);
    response.setContentLength(entity.length);
    response.getOutputStream().write(entity);
  }

  @SuppressWarnings("unchecked")
  public byte[] bytes(Object message) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (HttpMessageConverter<?> converter : converters) {
      if (
        converter.canWrite(
          message.getClass(),
          org.springframework.http.MediaType.APPLICATION_JSON
        )
      ) {
        ((HttpMessageConverter<Object>) converter).write(
            message,
            org.springframework.http.MediaType.APPLICATION_JSON,
            new HttpOutputMessage() {

              @Override
              public OutputStream getBody() {
                return out;
              }

              @Override
              public org.springframework.http.HttpHeaders getHeaders() {
                return new org.springframework.http.HttpHeaders();
              }
            }
          );
        break;
      }
    }
    return out.toByteArray();
  }
}
