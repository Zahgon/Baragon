package com.hubspot.baragon.service.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Reads the media types a request will take from its {@code Accept} header, leaving out the entries
 * that are not media types at all: the API has always answered such a request with one of the types
 * it could read, rather than refusing the request over the part it could not read. A header that
 * names nothing the API can read at all is a request the API refuses.
 */
public class LenientAcceptStrategy implements ContentNegotiationStrategy {

  @Override
  public List<MediaType> resolveMediaTypes(NativeWebRequest request)
    throws HttpMediaTypeNotAcceptableException {
    final HttpServletRequest servletRequest = request.getNativeRequest(
      HttpServletRequest.class
    );
    if (servletRequest == null) {
      return MEDIA_TYPE_ALL_LIST;
    }
    final List<MediaType> accepted = new ArrayList<>();
    boolean asked = false;
    final Enumeration<String> headers = servletRequest.getHeaders(HttpHeaders.ACCEPT);
    while (headers != null && headers.hasMoreElements()) {
      for (String entry : headers.nextElement().split(",")) {
        final String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        asked = true;
        try {
          accepted.add(MediaType.parseMediaType(trimmed));
        } catch (RuntimeException notAMediaType) {
          // an entry that is not a media type says nothing about what the caller will take
        }
      }
    }
    if (accepted.isEmpty()) {
      if (asked) {
        // the caller named what it will take, and none of it is a media type the API knows
        throw new HttpMediaTypeNotAcceptableException(
          "no media type among the ones asked for"
        );
      }
      return MEDIA_TYPE_ALL_LIST;
    }
    MediaType.sortBySpecificityAndQuality(accepted);
    return Collections.unmodifiableList(accepted);
  }
}
