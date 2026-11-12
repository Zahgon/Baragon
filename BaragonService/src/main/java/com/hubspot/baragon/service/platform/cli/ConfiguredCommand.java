package com.hubspot.baragon.service.platform.cli;

import com.hubspot.baragon.service.platform.Configuration;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * A command that is given the service's configuration, read from the file named on the command line.
 *
 * @param <T> the configuration type of the service
 */
public abstract class ConfiguredCommand<T extends Configuration> extends Command {

  protected ConfiguredCommand(String name, String description) {
    super(name, description);
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("file").nargs("?").help("application configuration file");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void run(Bootstrap<?> wildcardBootstrap, Namespace namespace) throws Exception {
    final Bootstrap<T> bootstrap = (Bootstrap<T>) wildcardBootstrap;
    final String path = namespace.getString("file");
    final T configuration = bootstrap.configuration(path);
    configuration.getLoggingFactory().configure();
    run(bootstrap, namespace, configuration);
  }

  protected abstract void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration)
    throws Exception;
}
