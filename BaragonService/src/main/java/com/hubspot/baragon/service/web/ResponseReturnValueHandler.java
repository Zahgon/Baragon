package com.hubspot.baragon.service.web;

import com.hubspot.baragon.service.platform.http.HttpHeaders;
import com.hubspot.baragon.service.platform.http.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor;

/**
 * Writes the answer a handler built itself: its status, the headers it set, and its entity written
 * through the same converters as any other answer, so that the media type is still negotiated and an
 * unacceptable one is still refused. An answer that refuses the request without an entity of its own
 * is left to the container, which tells it in its own words.
 */
public class ResponseReturnValueHandler extends AbstractMessageConverterMethodProcessor {

  public ResponseReturnValueHandler(
    List<HttpMessageConverter<?>> converters,
    ContentNegotiationManager negotiation
  ) {
    super(converters, negotiation);
  }

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    return Response.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return false;
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    ModelAndViewContainer container,
    NativeWebRequest request,
    WebDataBinderFactory factory
  ) {
    throw new UnsupportedOperationException();
  }

  /**
   * Names the media type the handler writes on an answer that holds no entity, as the API has always
   * named it on an answer to a request for the headers alone.
   */
  @SuppressWarnings("unchecked")
  private static void namedMediaType(
    NativeWebRequest webRequest,
    ServletServerHttpResponse outputMessage
  ) {
    final HttpServletRequest request = webRequest.getNativeRequest(
      HttpServletRequest.class
    );
    if (
      request == null ||
      !"HEAD".equals(request.getMethod()) ||
      outputMessage.getHeaders().getContentType() != null
    ) {
      return;
    }
    final Object declared = request.getAttribute(
      HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
    );
    if (declared instanceof Set) {
      for (MediaType type : (Set<MediaType>) declared) {
        if (type.isConcrete()) {
          outputMessage.getHeaders().setContentType(type);
          return;
        }
      }
    }
  }

  /** A relative location names the resource against the request it answers. */
  static String absolute(HttpServletRequest request, String location) {
    if (request == null || location.contains("://")) {
      return location;
    }
    return URI.create(request.getRequestURL().toString()).resolve(location).toString();
  }

  @Override
  public void handleReturnValue(
    Object returnValue,
    MethodParameter returnType,
    ModelAndViewContainer container,
    NativeWebRequest webRequest
  ) throws IOException, HttpMediaTypeNotAcceptableException {
    container.setRequestHandled(true);
    final Response response = (Response) returnValue;
    if (response == null) {
      return;
    }
    final ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
    outputMessage
      .getServletResponse()
      .setStatus(response.getStatus());
    final HttpServletRequest request = webRequest.getNativeRequest(
      HttpServletRequest.class
    );
    for (Map.Entry<String, List<Object>> header : response.getHeaders().entrySet()) {
      for (Object value : header.getValue()) {
        outputMessage
          .getServletResponse()
          .addHeader(
            header.getKey(),
            HttpHeaders.LOCATION.equalsIgnoreCase(header.getKey())
              ? absolute(request, String.valueOf(value))
              : String.valueOf(value)
          );
      }
    }
    if (response.getContentType() != null) {
      outputMessage
        .getHeaders()
        .setContentType(MediaType.parseMediaType(response.getContentType()));
    }
    final Object entity = response.getEntity();
    if (entity == null) {
      if (response.getStatus() >= 400) {
        // an answer that refuses the request and holds no entity of its own is told by the container
        outputMessage.getServletResponse().sendError(response.getStatus());
        return;
      }
      namedMediaType(webRequest, outputMessage);
      outputMessage.getBody();
      outputMessage.flush();
      return;
    }
    writeWithMessageConverters(
      entity,
      returnType,
      createInputMessage(webRequest),
      outputMessage
    );
  }
}
