package com.hubspot.baragon.service.web;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.platform.errors.ErrorMessage;
import com.hubspot.baragon.service.platform.errors.ValidationErrorMessage;
import com.hubspot.baragon.service.platform.http.HttpHeaders;
import com.hubspot.baragon.service.platform.http.Response;
import com.hubspot.baragon.service.platform.http.WebApplicationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Turns an error into the answer the API makes of it. An answer the thrower built itself is sent as
 * it was built; one that only names a status carries a JSON entity holding that status and the
 * message of the failure, and a refused method also names which methods the path takes. A request
 * whose entity could not be read is refused in the words the API uses for that, a request the API
 * refused over what it holds is answered with what was wrong with it, and any other failure is
 * logged under an identifier that the message repeats, with nothing else of it told to the caller.
 */
public class BaragonExceptionResolver implements HandlerExceptionResolver, Ordered {
  private static final Logger LOG = LoggerFactory.getLogger(
    BaragonExceptionResolver.class
  );
  private static final String JSON_ERROR = "Unable to process JSON";
  private static final String GENERIC_ERROR =
    "There was an error processing your request. It has been logged (ID %016x).";

  private final ErrorEntities entities;
  private final RouteTable routes;
  private final BaragonExceptionNotifier notifier;
  private final java.security.SecureRandom random = new java.security.SecureRandom();

  public BaragonExceptionResolver(
    ErrorEntities entities,
    RouteTable routes,
    BaragonExceptionNotifier notifier
  ) {
    this.entities = entities;
    this.routes = routes;
    this.notifier = notifier;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public ModelAndView resolveException(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler,
    Exception exception
  ) {
    final BadMessageException unreadable = unreadableRequest(exception);
    if (unreadable != null) {
      // a request the container gave up on is refused by the container, in its own words
      LOG.warn("Unreadable request", exception);
      throw unreadable;
    }
    try {
      if (exception instanceof EntityArgumentProcessor.ValidationFailure) {
        // the check of an entity failed, so the request failed, and the container says so
        LOG.error("Unable to check a request entity", exception.getCause());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request failed.");
        return new ModelAndView();
      }
      if (exception instanceof EmptyOptionalException) {
        // an answer that names no resource carries no entity of the API's own
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return new ModelAndView();
      }
      final List<String> refusals = refusals(exception);
      if (refusals != null) {
        entities.write(response, 422, new ValidationErrorMessage(refusals));
        return new ModelAndView();
      }
      if (exception instanceof WebApplicationException) {
        final Response built = ((WebApplicationException) exception).getResponse();
        if (built != null && built.getEntity() != null) {
          // an answer the thrower built itself is sent as it was built
          notifyIfFailure(built.getStatus(), exception);
          sendBuilt(response, built);
          return new ModelAndView();
        }
      }
      final ErrorMessage message = describe(request, response, exception);
      notifyIfFailure(message.getCode(), exception);
      entities.write(response, message.getCode(), message);
    } catch (IOException e) {
      LOG.warn("Unable to write the error answer", e);
    }
    return new ModelAndView();
  }

  /** A failure the service was asked to be told about is told about once its answer is known. */
  private void notifyIfFailure(int status, Exception exception) {
    if (status >= 500 && notifier != null) {
      notifier.notify(exception, Collections.<String, String>emptyMap());
    }
  }

  private void sendBuilt(HttpServletResponse response, Response built) throws IOException {
    if (response.isCommitted()) {
      return;
    }
    response.setStatus(built.getStatus());
    for (java.util.Map.Entry<String, List<Object>> header : built
      .getHeaders()
      .entrySet()) {
      for (Object value : header.getValue()) {
        response.addHeader(header.getKey(), String.valueOf(value));
      }
    }
    final byte[] entity = built.getEntity() instanceof String
      ? ((String) built.getEntity()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
      : entities.bytes(built.getEntity());
    response.setContentType(
      built.getContentType() == null
        ? MediaType.APPLICATION_JSON_VALUE
        : built.getContentType()
    );
    response.setContentLength(entity.length);
    response.getOutputStream().write(entity);
  }

  /** What a request entity the API refused is wrong about, or nothing when it was not refused for that. */
  private static List<String> refusals(Exception exception) {
    if (exception instanceof EntityArgumentProcessor.EntityRefusedException) {
      return ((EntityArgumentProcessor.EntityRefusedException) exception).getRefusals();
    }
    if (exception instanceof MethodArgumentNotValidException) {
      final List<String> messages = new ArrayList<>();
      for (FieldError error : ((MethodArgumentNotValidException) exception).getBindingResult()
        .getFieldErrors()) {
        messages.add(error.getField() + " " + error.getDefaultMessage());
      }
      for (ObjectError error : ((MethodArgumentNotValidException) exception).getBindingResult()
        .getGlobalErrors()) {
        messages.add(error.getDefaultMessage());
      }
      Collections.sort(messages);
      return messages;
    }
    if (exception instanceof ConstraintViolationException) {
      final List<String> messages = new ArrayList<>();
      for (ConstraintViolation<?> violation : ((ConstraintViolationException) exception).getConstraintViolations()) {
        final String path = String.valueOf(violation.getPropertyPath());
        final int dot = path.lastIndexOf('.');
        messages.add(
          (dot < 0 ? path : path.substring(dot + 1)) + " " + violation.getMessage()
        );
      }
      Collections.sort(messages);
      return messages;
    }
    return null;
  }

  /** The status and the message the API answers an error with. */
  private ErrorMessage describe(
    HttpServletRequest request,
    HttpServletResponse response,
    Exception exception
  ) {
    if (exception instanceof WebApplicationException) {
      final WebApplicationException webException = (WebApplicationException) exception;
      final Response built = webException.getResponse();
      if (built != null) {
        for (java.util.Map.Entry<String, List<Object>> header : built
          .getHeaders()
          .entrySet()) {
          for (Object value : header.getValue()) {
            response.addHeader(header.getKey(), String.valueOf(value));
          }
        }
      }
      return new ErrorMessage(webException.getStatus(), webException.getMessage());
    }
    if (exception instanceof NoHandlerFoundException) {
      return new ErrorMessage(404, WebApplicationException.defaultMessage(404));
    }
    if (exception instanceof HttpRequestMethodNotSupportedException) {
      final Set<String> allowed = routes.allowedMethods(OptionsFilter.path(request));
      if (allowed != null) {
        response.setHeader(HttpHeaders.ALLOW, String.join(",", allowed));
      }
      return new ErrorMessage(405, WebApplicationException.defaultMessage(405));
    }
    if (exception instanceof HttpMediaTypeNotAcceptableException) {
      return new ErrorMessage(406, WebApplicationException.defaultMessage(406));
    }
    if (exception instanceof HttpMediaTypeNotSupportedException) {
      return new ErrorMessage(415, WebApplicationException.defaultMessage(415));
    }
    if (exception instanceof MethodArgumentTypeMismatchException) {
      // a parameter the API cannot read names no resource
      return new ErrorMessage(404, WebApplicationException.defaultMessage(404));
    }
    final JsonProcessingException json = jsonCause(exception);
    if (json != null) {
      if (
        json instanceof JsonGenerationException || json instanceof InvalidDefinitionException
      ) {
        return logged(exception);
      }
      return new ErrorMessage(400, JSON_ERROR);
    }
    return logged(exception);
  }

  private ErrorMessage logged(Exception exception) {
    final long id = random.nextLong();
    LOG.error(String.format("Error handling a request: %016x", id), exception);
    return new ErrorMessage(500, String.format(GENERIC_ERROR, id));
  }

  /** The container's refusal of a request it gave up on reading, somewhere under this failure. */
  private static BadMessageException unreadableRequest(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof BadMessageException) {
        return (BadMessageException) cause;
      }
      if (cause instanceof EofException) {
        return new BadMessageException(HttpStatus.BAD_REQUEST_400);
      }
      if (cause == cause.getCause()) {
        break;
      }
    }
    return null;
  }

  private static JsonProcessingException jsonCause(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof JsonProcessingException) {
        return (JsonProcessingException) cause;
      }
      if (cause == cause.getCause()) {
        break;
      }
    }
    return null;
  }


}
