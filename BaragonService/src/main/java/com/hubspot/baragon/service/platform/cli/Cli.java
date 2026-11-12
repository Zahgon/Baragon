package com.hubspot.baragon.service.platform.cli;

import com.hubspot.baragon.service.platform.Configuration;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

/**
 * Reads the command line of the service's executable and runs the command it names. The executable run with
 * nothing on its command line says how it is used, as does the one asked for help; a command line it cannot
 * read is answered with what is wrong with it and with how the executable is used.
 *
 * @param <T> the configuration type of the service
 */
public class Cli<T extends Configuration> {
  private static final String COMMAND_NAME_ATTR = "command";
  private static final String NO_VERSION =
    "No application version detected. Add a Implementation-Version entry to your JAR's manifest to enable this.";

  private final Bootstrap<T> bootstrap;
  private final Map<String, Command> commands = new LinkedHashMap<>();
  private final ArgumentParser parser;
  private final PrintStream stdOut;
  private final PrintStream stdErr;

  public Cli(JarLocation location, Bootstrap<T> bootstrap) {
    this(location, bootstrap, System.out, System.err);
  }

  public Cli(JarLocation location, Bootstrap<T> bootstrap, PrintStream stdOut, PrintStream stdErr) {
    this.bootstrap = bootstrap;
    this.stdOut = stdOut;
    this.stdErr = stdErr;
    this.parser = buildParser(location);
    final Subparsers subparsers = parser.addSubparsers();
    subparsers.help("available commands");
    for (Command command : bootstrap.getCommands()) {
      commands.put(command.getName(), command);
      final Subparser subparser = subparsers.addParser(command.getName(), false);
      command.configure(subparser);
      addHelp(subparser);
      subparser
        .description(command.getDescription())
        .setDefault(COMMAND_NAME_ATTR, command.getName())
        .defaultHelp(true);
    }
  }

  private static ArgumentParser buildParser(JarLocation location) {
    final ArgumentParser parser = ArgumentParsers
      .newFor("java -jar " + location)
      .addHelp(false)
      .build();
    parser.version(location.getVersion().orElse(NO_VERSION));
    addHelp(parser);
    parser
      .addArgument("-v", "--version")
      .action(Arguments.version())
      .help("show the application version and exit");
    return parser;
  }

  private static void addHelp(ArgumentParser parser) {
    parser
      .addArgument("-h", "--help")
      .action(Arguments.help())
      .help("show this help message and exit")
      .setDefault(Arguments.SUPPRESS);
  }

  /** What the executable says how it is used on, in the character set it says everything else in. */
  private static PrintWriter writer(PrintStream stream) {
    try {
      return new PrintWriter(
        new java.io.OutputStreamWriter(stream, StandardCharsets.UTF_8.name()),
        true
      );
    } catch (UnsupportedEncodingException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  /** Runs the command the arguments name; answers false when the command did not run through. */
  public boolean run(String... arguments) {
    final Namespace namespace;
    try {
      // an executable run with nothing on its command line says how it is used
      if (arguments.length == 0) {
        parser.printHelp(writer(stdOut));
        return true;
      }
      namespace = parser.parseArgs(arguments);
    } catch (HelpScreenException ignored) {
      // the executable was asked how it is used, and has said so
      return true;
    } catch (ArgumentParserException e) {
      stdErr.println(e.getMessage());
      parser.printHelp(writer(stdErr));
      return false;
    }
    final Command command = commands.get(namespace.getString(COMMAND_NAME_ATTR));
    if (command == null) {
      parser.printHelp(writer(stdOut));
      return true;
    }
    try {
      command.run(bootstrap, namespace);
      return true;
    } catch (Exception e) {
      command.onError(stdErr, e);
      return false;
    }
  }
}
