package com.hubspot.baragon.service.web;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor;

/**
 * Writes the answer of a handler that answers with an optional value: the value it holds, written as
 * any other entity is, and when it holds nothing, the answer the API has always made of that, which
 * names no resource and carries no entity of its own.
 */
public class OptionalReturnValueHandler extends AbstractMessageConverterMethodProcessor {

  public OptionalReturnValueHandler(
    List<HttpMessageConverter<?>> converters,
    ContentNegotiationManager negotiation
  ) {
    super(converters, negotiation);
  }

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    return Optional.class.isAssignableFrom(returnType.getParameterType());
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

  @Override
  public void handleReturnValue(
    Object returnValue,
    MethodParameter returnType,
    ModelAndViewContainer container,
    NativeWebRequest webRequest
  ) throws IOException, HttpMediaTypeNotAcceptableException {
    container.setRequestHandled(true);
    final Optional<?> optional = (Optional<?>) returnValue;
    if (optional == null || !optional.isPresent()) {
      throw new EmptyOptionalException();
    }
    writeWithMessageConverters(
      optional.get(),
      returnType,
      createInputMessage(webRequest),
      createOutputMessage(webRequest)
    );
  }
}
