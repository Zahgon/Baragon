package com.hubspot.baragon.service.platform.cli;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where the service runs from: the name of its jar, which the command line tells the caller to run, and the
 * version that jar was built as.
 */
public class JarLocation {
  private static final Pattern JAR_URL = Pattern.compile("^(?:jar:)?file:(/.*?\\.jar)(?:!.*)?$");

  private final Class<?> klass;

  public JarLocation(Class<?> klass) {
    this.klass = klass;
  }

  /** The version the jar was built as, when it says. */
  public Optional<String> getVersion() {
    return Optional.ofNullable(klass.getPackage().getImplementationVersion());
  }

  @Override
  public String toString() {
    try {
      final URL location = klass.getProtectionDomain().getCodeSource().getLocation();
      final Matcher jar = JAR_URL.matcher(location.toString());
      if (jar.matches()) {
        final String path = jar.group(1);
        return path.substring(path.lastIndexOf('/') + 1);
      }
    } catch (RuntimeException notAJar) {
      // the service is not running from a jar, so there is no name to tell the caller
    }
    return "project.jar";
  }
}
