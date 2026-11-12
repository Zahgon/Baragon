package com.hubspot.baragon.service.platform.tasks;

import com.google.common.collect.ImmutableMultimap;
import java.io.PrintWriter;

/** An operation the admin interface can be asked to carry out by name. */
public abstract class Task {
  private final String name;

  protected Task(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract void execute(
    ImmutableMultimap<String, String> parameters,
    PrintWriter output
  ) throws Exception;
}
