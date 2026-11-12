package com.hubspot.baragon.service.platform.tasks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.ImmutableMultimap;
import java.io.PrintWriter;
import org.slf4j.LoggerFactory;

/** Sets the level of one or more loggers while the service runs. */
public class LogConfigurationTask extends Task {
  private final LoggerContext context;

  public LogConfigurationTask() {
    super("log-level");
    final org.slf4j.ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    this.context = factory instanceof LoggerContext ? (LoggerContext) factory : null;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
    if (context == null) {
      return;
    }
    final Level level = parameters.containsKey("level")
      ? Level.valueOf(parameters.get("level").asList().get(0))
      : null;
    for (String name : parameters.get("logger")) {
      final Logger logger = context.getLogger(name);
      logger.setLevel(level);
      output.println(
        String.format("Configured logging level for %s to %s", name, level)
      );
    }
  }
}
