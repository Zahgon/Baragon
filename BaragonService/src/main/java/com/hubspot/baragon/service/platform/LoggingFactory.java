package com.hubspot.baragon.service.platform;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code logging} section of the configuration file. The appenders and their settings are read
 * and the levels are applied; what the appenders write is not part of the service's API.
 */
public class LoggingFactory {

  /** One entry of the {@code appenders} list, named by its {@code type} property. */
  public static class AppenderFactory {
    @JsonProperty
    private String type = "console";

    @JsonProperty
    private String threshold;

    @JsonProperty
    private String logFormat;

    private final Map<String, Object> other = new LinkedHashMap<>();

    @JsonAnySetter
    public void set(String name, Object value) {
      other.put(name, value);
    }

    public String getType() {
      return type;
    }

    public String getThreshold() {
      return threshold;
    }

    public String getLogFormat() {
      return logFormat;
    }
  }

  @JsonProperty
  private String level = "INFO";

  @JsonProperty
  private Map<String, String> loggers = new LinkedHashMap<>();

  @JsonProperty
  private List<AppenderFactory> appenders = new ArrayList<>();

  private final Map<String, Object> other = new LinkedHashMap<>();

  @JsonAnySetter
  public void set(String name, Object value) {
    other.put(name, value);
  }

  public String getLevel() {
    return level;
  }

  public Map<String, String> getLoggers() {
    return loggers;
  }

  public List<AppenderFactory> getAppenders() {
    return appenders;
  }

  /**
   * The level everything is logged at before a configuration file has been read, so that reading one does
   * not fill the executable's output with what the libraries it reads with have to say.
   */
  public static void bootstrap() {
    apply(Logger.ROOT_LOGGER_NAME, "WARN");
  }

  /** Applies the configured levels to the logging back end. */
  public void configure() {
    apply(Logger.ROOT_LOGGER_NAME, level);
    loggers.forEach(LoggingFactory::apply);
  }

  private static void apply(String name, String levelName) {
    if (levelName == null) {
      return;
    }
    final Logger logger = LoggerFactory.getLogger(name);
    if (logger instanceof ch.qos.logback.classic.Logger) {
      ((ch.qos.logback.classic.Logger) logger).setLevel(
          Level.toLevel(levelName, Level.INFO)
        );
    }
  }
}
