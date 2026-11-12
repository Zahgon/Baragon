package com.hubspot.baragon.service.platform.cli;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/** One of the commands the service's executable takes as its first argument. */
public abstract class Command {
  private final String name;
  private final String description;

  protected Command(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /** Declares the arguments this command takes beyond the ones every command takes. */
  public void configure(Subparser subparser) {}

  public abstract void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception;

  /** What the executable says when this command did not run through. */
  public void onError(java.io.PrintStream stdErr, Throwable failure) {
    failure.printStackTrace(stdErr);
  }
}
