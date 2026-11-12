package com.hubspot.baragon.service.web;

import javax.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/** A handler that answers nothing is answered with 204, as the API has always answered it. */
public class NoContentReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    return (
      void.class.equals(returnType.getParameterType()) ||
      Void.class.equals(returnType.getParameterType())
    );
  }

  @Override
  public void handleReturnValue(
    Object returnValue,
    MethodParameter returnType,
    ModelAndViewContainer container,
    NativeWebRequest webRequest
  ) {
    container.setRequestHandled(true);
    final HttpServletResponse response = webRequest.getNativeResponse(
      HttpServletResponse.class
    );
    if (response != null) {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
  }
}
