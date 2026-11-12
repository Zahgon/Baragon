package com.hubspot.baragon.service.platform;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

/**
 * Reads a configuration file of the service and checks that it holds what the service needs. What is wrong
 * with a file is told as the service has always told it: the name of the file, whether there is one thing
 * wrong with it or several, and one line for each - where the text is not a document at all, where in the
 * file that is, and what the service needs that the file does not give it.
 */
public class ConfigurationFactory<T> {

  /** Thrown when a configuration file is unreadable or incomplete. */
  public static class ConfigurationException extends IOException {
    private static final long serialVersionUID = 1L;

    public ConfigurationException(String path, Collection<String> errors) {
      super(formatMessage(path, errors));
    }

    public ConfigurationException(String path, Collection<String> errors, Throwable cause) {
      super(formatMessage(path, errors), cause);
    }

    /** The name of the file, and what is wrong with it, one to a line. */
    static String formatMessage(String path, Collection<String> errors) {
      final StringBuilder message = new StringBuilder(path);
      message.append(errors.size() == 1 ? " has an error:" : " has the following errors:");
      for (String error : errors) {
        message.append(String.format("%n  * %s", error));
      }
      return message.toString();
    }
  }

  /** The name the service calls the configuration it reads when no file was named. */
  public static final String DEFAULT_CONFIGURATION = "default configuration";

  private final Class<T> klass;
  private final ObjectMapper mapper;
  private final Validator validator;
  private final YAMLFactory yaml = new YAMLFactory();

  public ConfigurationFactory(Class<T> klass, ObjectMapper objectMapper, Validator validator) {
    this.klass = klass;
    this.mapper = objectMapper.copy();
    this.validator = validator;
  }

  /** The configuration the service runs on when no file was named: everything at its default. */
  public T build() throws IOException {
    return check(mapper.readValue("{}", klass), DEFAULT_CONFIGURATION);
  }

  public T build(ConfigurationSourceProvider provider, String path) throws IOException {
    final JsonNode node;
    try (InputStream source = provider.open(path)) {
      node = mapper.readTree(yaml.createParser(source));
    } catch (JsonParseException e) {
      throw new ConfigurationException(path, Collections.singletonList(malformed(e)), e);
    }
    final T configuration;
    try {
      configuration =
        mapper.readValue(
          new com.fasterxml.jackson.databind.node.TreeTraversingParser(
            node == null ? NullNode.getInstance() : node,
            mapper
          ),
          klass
        );
    } catch (JsonMappingException e) {
      throw new ConfigurationException(path, Collections.singletonList(e.getMessage()), e);
    }
    return check(configuration, path);
  }

  /** What is wrong with the text of the file, and where in the file that is. */
  private static String malformed(JsonParseException failure) {
    final StringBuilder error = new StringBuilder("Malformed YAML");
    final JsonLocation location = failure.getLocation();
    if (location != null && location.getLineNr() > -1) {
      error.append(" at line: ").append(location.getLineNr() + 1);
      if (location.getColumnNr() > -1) {
        error.append(", column: ").append(location.getColumnNr() + 1);
      }
    }
    return error.append("; ").append(failure.getMessage()).toString();
  }

  /** What the service needs that the configuration does not give it. */
  private T check(T configuration, String path) throws ConfigurationException {
    if (validator == null) {
      return configuration;
    }
    final Set<ConstraintViolation<T>> violations = validator.validate(configuration);
    if (violations.isEmpty()) {
      return configuration;
    }
    final List<String> errors = new ArrayList<>();
    for (ConstraintViolation<T> violation : violations) {
      errors.add(violation.getPropertyPath() + " " + violation.getMessage());
    }
    Collections.sort(errors);
    throw new ConfigurationException(path, errors);
  }

  @Nullable
  static String nameOf(@Nullable String path) {
    return path == null ? DEFAULT_CONFIGURATION : path;
  }
}
