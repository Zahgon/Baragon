package com.hubspot.baragon.service.platform.tasks;

import com.google.common.collect.ImmutableMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;

/** Runs the garbage collector, as many times as the request asks for. */
public class GarbageCollectionTask extends Task {

  public GarbageCollectionTask() {
    super("gc");
  }

  @Override
  @SuppressFBWarnings(
    value = "DM_GC",
    justification = "collecting garbage is what this task is asked to do"
  )
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
    final int runs = parseRuns(parameters);
    for (int i = 0; i < runs; i++) {
      output.println("Running GC...");
      output.flush();
      Runtime.getRuntime().gc();
    }
    output.println("Done!");
  }

  private static int parseRuns(ImmutableMultimap<String, String> parameters) {
    final ImmutableMultimap<String, String> values = parameters;
    if (!values.containsKey("runs")) {
      return 1;
    }
    try {
      return Integer.parseInt(values.get("runs").asList().get(0));
    } catch (NumberFormatException notANumber) {
      return 1;
    }
  }
}
