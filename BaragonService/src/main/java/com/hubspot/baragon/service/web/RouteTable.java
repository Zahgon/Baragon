package com.hubspot.baragon.service.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The request methods each path of the API declares. The answer to an {@code OPTIONS} request and
 * the {@code Allow} header of a refused method are built from it: a path allows the methods that the
 * most exactly written of the paths matching it declares, together with {@code OPTIONS}, and an
 * {@code OPTIONS} answer also names {@code HEAD} where the path declares {@code GET}. Two paths
 * written alike but for the names of the parameters in them are one path.
 */
public class RouteTable {
  private static final Pattern PARAMETER = Pattern.compile("\\{[^/]*}");

  private final Map<String, Set<String>> methodsByPattern = new LinkedHashMap<>();
  private final PathMatcher matcher = new AntPathMatcher();

  public RouteTable(RequestMappingHandlerMapping mapping) {
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping
      .getHandlerMethods()
      .entrySet()) {
      final Set<String> patterns = patternsOf(entry.getKey());
      final Set<RequestMethod> requestMethods = entry
        .getKey()
        .getMethodsCondition()
        .getMethods();
      for (String pattern : patterns) {
        final Set<String> methods = methodsByPattern.computeIfAbsent(
          pattern,
          key -> new HashSet<>()
        );
        if (requestMethods.isEmpty()) {
          for (HttpMethod method : HttpMethod.values()) {
            methods.add(method.name());
          }
        } else {
          requestMethods.forEach(method -> methods.add(method.name()));
        }
      }
    }
  }

  /** The paths a mapping answers on, however the mapping expresses them. */
  private static Set<String> patternsOf(RequestMappingInfo mapping) {
    final org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition parsed = mapping.getPathPatternsCondition();
    if (parsed != null) {
      return parsed.getPatternValues();
    }
    final org.springframework.web.servlet.mvc.condition.PatternsRequestCondition written = mapping.getPatternsCondition();
    return written == null ? Collections.<String>emptySet() : written.getPatterns();
  }

  /** How exactly a path is written: the letters of it that are not a parameter, then its parameters. */
  private static long exactness(String pattern) {
    final String literal = PARAMETER.matcher(pattern).replaceAll("");
    final java.util.regex.Matcher parameters = PARAMETER.matcher(pattern);
    long count = 0;
    while (parameters.find()) {
      count++;
    }
    return (long) literal.length() * 1000 - count;
  }

  /** The declared methods of the path, or nothing when no path of the API matches it. */
  @Nullable
  public Set<String> declaredMethods(String path) {
    final Set<String> exact = methodsByPattern.get(path);
    if (exact != null) {
      return exact;
    }
    long best = Long.MIN_VALUE;
    final Set<String> declared = new HashSet<>();
    for (Map.Entry<String, Set<String>> entry : methodsByPattern.entrySet()) {
      if (!matcher.match(entry.getKey(), path)) {
        continue;
      }
      final long exactness = exactness(entry.getKey());
      if (exactness > best) {
        best = exactness;
        declared.clear();
      }
      if (exactness == best) {
        // paths written as exactly as each other answer for the same path, so they are one path
        declared.addAll(entry.getValue());
      }
    }
    return declared.isEmpty() ? null : declared;
  }

  /** The methods named in the {@code Allow} header of a refused method. */
  @Nullable
  public Set<String> allowedMethods(String path) {
    final Set<String> declared = declaredMethods(path);
    if (declared == null) {
      return null;
    }
    final Set<String> allowed = new HashSet<>(declared);
    allowed.add("OPTIONS");
    return allowed;
  }

  /** The methods named in the answer to an {@code OPTIONS} request. */
  @Nullable
  public Set<String> optionsMethods(String path) {
    final Set<String> declared = declaredMethods(path);
    if (declared == null) {
      return null;
    }
    final Set<String> allowed = new HashSet<>(declared);
    if (declared.contains("GET")) {
      allowed.add("HEAD");
    }
    allowed.add("OPTIONS");
    return allowed;
  }
}
