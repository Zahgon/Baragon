package com.hubspot.baragon.service.web;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * Reads the entity a handler is given as the API has always read it: a request that carries no entity
 * at all gives the handler nothing, rather than being refused; a handler that states its entity may
 * not be nothing refuses such a request in the words the API uses for that; and a failure while the
 * entity is being checked against what the handler states about it is a failure of the request, not an
 * answer of the API's own.
 */
public class EntityArgumentProcessor extends RequestResponseBodyMethodProcessor {
  /** What the API answers with when a handler that states an entity is given none. */
  static final String MISSING_ENTITY = "The request body may not be null";

  public EntityArgumentProcessor(
    List<HttpMessageConverter<?>> converters,
    org.springframework.web.accept.ContentNegotiationManager negotiation
  ) {
    super(converters, negotiation);
  }

  /**
   * A request that carries no entity is read as an entity of nothing, unless the handler states that
   * its entity may not be nothing, which such a request is refused for.
   */
  @Override
  protected boolean checkRequired(MethodParameter parameter) {
    if (parameter.getParameterAnnotation(NotNull.class) != null) {
      throw new EntityRefusedException(Collections.singletonList(MISSING_ENTITY));
    }
    return false;
  }

  /** The entity of a request that said nothing about what it carries is one the API cannot read. */
  private static void refuseUnclaimedEntity(
    org.springframework.web.context.request.NativeWebRequest request
  ) throws org.springframework.web.HttpMediaTypeNotSupportedException {
    final javax.servlet.http.HttpServletRequest servletRequest = request.getNativeRequest(
      javax.servlet.http.HttpServletRequest.class
    );
    if (
      servletRequest != null &&
      Boolean.TRUE.equals(servletRequest.getAttribute(RequestReadingFilter.SAID_NOTHING)) &&
      RequestReadingFilter.carriesEntity(servletRequest)
    ) {
      throw new org.springframework.web.HttpMediaTypeNotSupportedException(
        "the request says nothing about the media type of what it carries"
      );
    }
  }

  /** An entity holding no JSON at all is an entity of nothing. */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "NP_NONNULL_RETURN_VIOLATION",
    justification = "an entity of nothing is what a handler is given for a request that carries none"
  )
  protected <T> Object readWithMessageConverters(
    org.springframework.web.context.request.NativeWebRequest request,
    MethodParameter parameter,
    java.lang.reflect.Type type
  ) throws IOException, org.springframework.web.HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
    refuseUnclaimedEntity(request);
    try {
      return super.readWithMessageConverters(request, parameter, type);
    } catch (HttpMessageNotReadableException unreadable) {
      if (holdsNothing(unreadable)) {
        checkRequired(parameter);
        return null;
      }
      throw unreadable;
    }
  }

  /** Whether the entity held no JSON at all, rather than JSON the API could not read. */
  private static boolean holdsNothing(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException) {
        final String message = ((com.fasterxml.jackson.core.JsonProcessingException) cause).getOriginalMessage();
        return message != null && message.startsWith("No content");
      }
      if (cause == cause.getCause()) {
        break;
      }
    }
    return false;
  }

  /** A handler that answers with nothing is answered as the API answers that. */
  @Override
  public void handleReturnValue(
    Object returnValue,
    MethodParameter returnType,
    org.springframework.web.method.support.ModelAndViewContainer container,
    org.springframework.web.context.request.NativeWebRequest request
  ) throws IOException, org.springframework.web.HttpMediaTypeNotAcceptableException {
    if (returnValue == null) {
      container.setRequestHandled(true);
      final javax.servlet.http.HttpServletResponse response = request.getNativeResponse(
        javax.servlet.http.HttpServletResponse.class
      );
      if (response != null) {
        response.setStatus(javax.servlet.http.HttpServletResponse.SC_NO_CONTENT);
      }
      return;
    }
    super.handleReturnValue(returnValue, returnType, container, request);
  }

  /**
   * Checks the entity against what the handler states about it; a failure of the check itself is a
   * failure of the request.
   */
  @Override
  protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
    try {
      super.validateIfApplicable(binder, parameter);
    } catch (RuntimeException failed) {
      throw new ValidationFailure(failed);
    }
  }

  /** The refusals of an entity the API could read but would not take. */
  public static class EntityRefusedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final List<String> refusals;

    public EntityRefusedException(List<String> refusals) {
      super(String.join(", ", refusals));
      this.refusals = refusals;
    }

    public List<String> getRefusals() {
      return refusals;
    }
  }

  /** A failure while the entity was being checked, which the API does not answer for itself. */
  public static class ValidationFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationFailure(Throwable cause) {
      super(cause);
    }
  }


}
