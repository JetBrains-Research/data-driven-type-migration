package org.jetbrains.research.ddtc.evaluation;

import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public class EvaluationRunner implements ApplicationStarter {
    private static final Logger LOG = Logger.getInstance(EvaluationRunner.class);

    @Override
    public @NonNls
    String getCommandName() {
        return "evaluation";
    }

    @Override
    public void main(@NotNull List<String> args) {
        try {
            CommandLine parsedArgs = parseArgs(args.toArray(new String[0]));
            Path pathToProjects = Path.of(parsedArgs.getOptionValue("src-projects-dir"));
            System.out.println(pathToProjects);

        } catch (Throwable e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    private CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option input = new Option(
                "s",
                "src-projects-dir",
                true,
                "path to the directory with projects for evaluation"
        );
        input.setRequired(true);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("evaluation", options);
            System.exit(1);
        }
        return cmd;
    }
}
