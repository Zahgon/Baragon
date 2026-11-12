package com.hubspot.baragon.service.platform.http;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.Nullable;

/**
 * What a handler answers with when it chooses the status, the headers or the entity itself: a status
 * code, an optional entity and headers to set.
 */
public class Response {

  /** The status codes the API answers with by name. */
  public enum Status {
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    NOT_MODIFIED(304, "Not Modified"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    CONFLICT(409, "Conflict"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    private final int code;
    private final String reason;

    Status(int code, String reason) {
      this.code = code;
      this.reason = reason;
    }

    public int getStatusCode() {
      return code;
    }

    public String getReasonPhrase() {
      return reason;
    }

    @Nullable
    public static Status fromStatusCode(int code) {
      for (Status status : values()) {
        if (status.code == code) {
          return status;
        }
      }
      return null;
    }
  }

  /** The headers of an answer, each with every value it was given. */
  public static class Headers extends LinkedHashMap<String, List<Object>> {
    private static final long serialVersionUID = 1L;

    public void add(String name, Object value) {
      computeIfAbsent(name, key -> new ArrayList<>()).add(value);
    }

    @Nullable
    public Object getFirst(String name) {
      final List<Object> values = get(name);
      return values == null || values.isEmpty() ? null : values.get(0);
    }
  }

  /** Collects what an answer is made of. */
  public static class ResponseBuilder {
    private int status = 200;
    private Object entity;
    private String contentType;
    private final Headers headers = new Headers();

    public ResponseBuilder status(int status) {
      this.status = status;
      return this;
    }

    public ResponseBuilder status(Status status) {
      return status(status.getStatusCode());
    }

    public ResponseBuilder entity(@Nullable Object entity) {
      this.entity = entity;
      return this;
    }

    public ResponseBuilder type(@Nullable String contentType) {
      this.contentType = contentType;
      return this;
    }

    public ResponseBuilder header(String name, Object value) {
      headers.add(name, value);
      return this;
    }

    public ResponseBuilder location(URI location) {
      return header(HttpHeaders.LOCATION, location);
    }

    public Response build() {
      return new Response(status, entity, contentType, headers);
    }
  }

  private final int status;
  private final Object entity;
  private final String contentType;
  private final Headers headers;

  private Response(
    int status,
    @Nullable Object entity,
    @Nullable String contentType,
    Headers headers
  ) {
    this.status = status;
    this.entity = entity;
    this.contentType = contentType;
    this.headers = new Headers();
    this.headers.putAll(headers);
  }

  public int getStatus() {
    return status;
  }

  @Nullable
  public Object getEntity() {
    return entity;
  }

  @Nullable
  public String getContentType() {
    return contentType;
  }

  public Headers getHeaders() {
    return headers;
  }

  public static ResponseBuilder ok() {
    return new ResponseBuilder().status(Status.OK);
  }

  public static ResponseBuilder ok(@Nullable Object entity) {
    return ok().entity(entity);
  }

  public static ResponseBuilder created(URI location) {
    return new ResponseBuilder().status(Status.CREATED).location(location);
  }

  public static ResponseBuilder noContent() {
    return new ResponseBuilder().status(Status.NO_CONTENT);
  }

  public static ResponseBuilder notModified() {
    return new ResponseBuilder().status(Status.NOT_MODIFIED);
  }

  public static ResponseBuilder serverError() {
    return new ResponseBuilder().status(Status.INTERNAL_SERVER_ERROR);
  }

  public static ResponseBuilder status(Status status) {
    return new ResponseBuilder().status(status);
  }

  public static ResponseBuilder status(int status) {
    return new ResponseBuilder().status(status);
  }
}
