package com.hubspot.baragon.service.platform;

import java.io.IOException;
import java.io.InputStream;

/** Where the text of a configuration file is read from. */
public interface ConfigurationSourceProvider {
  InputStream open(String path) throws IOException;
}
