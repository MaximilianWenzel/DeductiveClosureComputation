package benchmark;

import enums.SaturationApproach;
import picocli.CommandLine;
import util.ConsoleUtils;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CommandLine.Command(name = "benchmark", subcommands = {CommandLine.HelpCommand.class},
        description = "Executes a variety of different saturation benchmarks.")
public class SaturationBenchmarkCLApp {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SaturationBenchmarkCLApp()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Executes a benchmark where the transitive closure of a given binary tree is computed.")
    public void binaryTree(
            @CommandLine.Option(
                    names = {"-a", "--includedApproaches"},
                    arity = "1..*",
                    paramLabel = "APPROACHES",
                    required = true)
                    SaturationApproach[] approaches,
            @CommandLine.Option(
                    names = {"-w", "--numberOfWorkers"},
                    arity = "1..*",
                    paramLabel = "NUM_WORKERS",
                    required = true)
                    Integer[] numberOfWorkers,
            @CommandLine.Option(
                    names = {"-d", "--depth"},
                    arity = "1..*",
                    paramLabel = "DEPTH",
                    required = true)
                    Integer[] binaryTreeDepth,
            @CommandLine.Option(names = {"-o", "--outputDirectory"},
                    required = true)
                    File outputDirectory) {
        IndividualExperiments.binaryTreeBenchmark(
                outputDirectory,
                Arrays.stream(approaches).collect(Collectors.toSet()),
                Arrays.stream(binaryTreeDepth).collect(Collectors.toSet()),
                Arrays.stream(numberOfWorkers).collect(Collectors.toSet())
        );
    }

    @CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Executes a benchmark where the transitive closure of a given chain graph is computed.")
    public void chainGraph(
            @CommandLine.Option(
                    names = {"-a", "--includedApproaches"},
                    arity = "1..*",
                    paramLabel = "APPROACHES",
                    required = true)
                    SaturationApproach[] approaches,
            @CommandLine.Option(
                    names = {"-w", "--numberOfWorkers"},
                    arity = "1..*",
                    paramLabel = "NUM_WORKERS",
                    required = true)
                    Integer[] numberOfWorkers,
            @CommandLine.Option(
                    names = {"-d", "--chainGraphDepth"},
                    arity = "1..*",
                    paramLabel = "DEPTH",
                    required = true)
                    Integer[] binaryTreeDepth,
            @CommandLine.Option(names = {"-o", "--outputDirectory"},
                    required = true)
                    File outputDirectory) {
        IndividualExperiments.chainGraphBenchmark(
                outputDirectory,
                Arrays.stream(approaches).collect(Collectors.toSet()),
                Arrays.stream(binaryTreeDepth).collect(Collectors.toSet()),
                Arrays.stream(numberOfWorkers).collect(Collectors.toSet())
        );
    }

    @CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class},
            description = "Executes an echo benchmark where all initial axioms are echoed a single time between the workers.")
    public void echo(
            @CommandLine.Option(
                    names = {"-a", "--includedApproaches"},
                    arity = "1..*",
                    paramLabel = "APPROACHES",
                    required = true)
                    SaturationApproach[] approaches,
            @CommandLine.Option(
                    names = {"-w", "--numberOfWorkers"},
                    arity = "1..*",
                    paramLabel = "NUM_WORKERS",
                    required = true)
                    Integer[] numberOfWorkers,
            @CommandLine.Option(
                    names = {"-e", "--initialEchoAxioms"},
                    arity = "1..*",
                    paramLabel = "DEPTH",
                    required = true)
                    Integer[] binaryTreeDepth,
            @CommandLine.Option(names = {"-o", "--outputDirectory"},
                    required = true)
                    File outputDirectory) {
        IndividualExperiments.echoBenchmark(
                outputDirectory,
                Arrays.stream(approaches).collect(Collectors.toSet()),
                Arrays.stream(binaryTreeDepth).collect(Collectors.toSet()),
                Arrays.stream(numberOfWorkers).collect(Collectors.toSet())
        );
    }

}
