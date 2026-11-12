package com.hubspot.baragon.service.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Reads a configuration file from the file system. */
public class FileConfigurationSourceProvider implements ConfigurationSourceProvider {

  @Override
  public InputStream open(String path) throws IOException {
    final File file = new File(path);
    if (!file.exists()) {
      throw new FileNotFoundException("File " + file + " not found");
    }
    return new FileInputStream(file);
  }
}
