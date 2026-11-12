package com.hubspot.baragon.service.platform.cli;

import com.hubspot.baragon.service.platform.Configuration;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Reads the configuration file and says nothing when it holds what the service needs.
 *
 * @param <T> the configuration type of the service
 */
public class CheckCommand<T extends Configuration> extends ConfiguredCommand<T> {

  public CheckCommand() {
    super("check", "Parses and validates the configuration file");
  }

  @Override
  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) {
    // a configuration file the service can run on is a file this command says nothing about
  }

  /** What is wrong with the configuration file, and nothing else of the failure. */
  @Override
  public void onError(java.io.PrintStream stdErr, Throwable failure) {
    stdErr.println(failure.getMessage());
  }
}
