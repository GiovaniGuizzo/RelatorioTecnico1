/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import indicators.HypervolumeHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.moead.MOEAD_DRA;
import jmetal.metaheuristics.nsgaII.NSGAII;
import jmetal.metaheuristics.spea2.SPEA2;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.DTLZ.DTLZ1;
import jmetal.problems.DTLZ.DTLZ7;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.qualityIndicator.R2;
import jmetal.util.JMException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Experiment {

    private static Problem[] PROBLEMS = new Problem[]{
        new DTLZ1("Real", 6, 2),
        new DTLZ1("Real", 7, 3),
        new DTLZ7("Real", 21, 2),
        new DTLZ7("Real", 22, 3)
    };

    private static String[] ALGORITHMS = new String[]{
        "NSGAII",
        "SPEA2",
        "MOEAD_DRA"
    };

    public static void main(String[] args) {
        boolean algorithms = false;
        boolean indicators = false;
        boolean tests = false;
        if (args.length >= 1) {
            for (String string : args) {
                switch (string) {
                    case "algorithms":
                        algorithms = true;
                        break;
                    case "indicators":
                        indicators = true;
                        break;
                    case "tests":
                        tests = true;
                        break;
                    case "all":
                        algorithms = true;
                        indicators = true;
                        tests = true;
                        break;
                    case "-h":
                        System.out.println("Write the following in any order to determine what you want to execute:");
                        System.out.println("\talgorithms - Execute the algorithms;");
                        System.out.println("\tindicators - Execute the collect data from indicators;");
                        System.out.println("\ttests - Execute statistical tests;");
                        System.out.println("\tall - Execute everything;");
                        System.exit(0);
                        break;
                }
            }
        } else if (args.length == 0) {
            System.out.println("Write the following in any order to determine what you want to execute:");
            System.out.println("\talgorithms - Execute the algorithms;");
            System.out.println("\tindicators - Execute the collect data from indicators;");
            System.out.println("\ttests - Execute statistical tests;");
            System.out.println("\tall - Execute everything;");
            System.exit(0);
        }
        try {
            if (algorithms) {
                System.out.println("Executing Algorithms:");
                execute();
                System.out.println();
            }
            if (indicators) {
                System.out.println("Calculating Indicator Statistics:");
                gatherMetricsAverageAndStdDev("HYPERVOLUME", "IGD", "R2", "TIME");
                System.out.println();
                System.out.println();
            }
            if (tests) {
                System.out.println("Running Statistical Tests:");
                runStatisticalComparsion("HYPERVOLUME", "IGD", "R2");
                System.out.println();
                System.out.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(Experiment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void execute() throws JMException, SecurityException, IOException, ClassNotFoundException {
        Operator crossover; // Crossover operator
        Operator mutation; // Mutation operator
        Operator selection; // Selection operator

        HashMap parameters; // Operator parameters

        for (Problem problem : PROBLEMS) {

            QualityIndicator igdIndicator = new QualityIndicator(problem, "truePareto/" + problem.getName() + "." + problem.getNumberOfObjectives() + "D.pf");

            R2 r2Indicator;
            if (problem.getNumberOfObjectives() == 2) {
                r2Indicator = new R2();
            } else {
                r2Indicator = new R2(problem.getNumberOfObjectives(), "weight/W" + problem.getNumberOfObjectives() + "D.dat");
            }

            algorithmLoop:
            for (String algorithmString : ALGORITHMS) {

                String dirPath = "experiment/" + problem.getName() + "_" + problem.getNumberOfObjectives() + "/" + algorithmString + "/";
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try (FileWriter r2 = new FileWriter(dirPath + "R2.txt"); FileWriter igd = new FileWriter(dirPath + "IGD.txt"); FileWriter time = new FileWriter(dirPath + "TIME.txt")) {
                    System.out.println("Algorithm " + algorithmString + " for problem " + problem.getName() + "_" + problem.getNumberOfObjectives() + ":");
                    printProgress(0);
                    for (int execution = 0; execution < 30; execution++) {

                        Algorithm algorithm;

                        switch (algorithmString) {
                            case "NSGAII":
                                algorithm = new NSGAII(problem);
                                break;
                            case "SPEA2":
                                algorithm = new SPEA2(problem);
                                break;
                            case "MOEAD_DRA":
                                algorithm = new MOEAD_DRA(problem);
                                break;
                            default:
                                System.out.println("Algoritmo inválido!");
                                break algorithmLoop;
                        }

                        // Algorithm parameters
                        algorithm.setInputParameter("populationSize", 100);
                        algorithm.setInputParameter("archiveSize", 100);
                        algorithm.setInputParameter("maxEvaluations", 10100);
                        algorithm.setInputParameter("finalSize", 100);
                        algorithm.setInputParameter("T", 20);
                        algorithm.setInputParameter("delta", 0.9);
                        algorithm.setInputParameter("nr", 2);
                        algorithm.setInputParameter("dataDirectory", "weight");

                        // Mutation and Crossover for Real codification
                        parameters = new HashMap();
                        parameters.put("probability", 0.9);
                        parameters.put("distributionIndex", 20.0);
                        parameters.put("CR", 1.0);
                        parameters.put("F", 0.5);
                        if (algorithmString.equals("MOEAD_DRA")) {
                            crossover = CrossoverFactory.getCrossoverOperator("DifferentialEvolutionCrossover", parameters);
                        } else {
                            crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);
                        }

                        parameters = new HashMap();
                        parameters.put("probability", 1.0 / problem.getNumberOfVariables());
                        parameters.put("distributionIndex", 20.0);
                        mutation = MutationFactory.getMutationOperator("PolynomialMutation", parameters);

                        // Selection Operator
                        parameters = null;
                        selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

                        // Add the operators to the algorithm
                        algorithm.addOperator("crossover", crossover);
                        algorithm.addOperator("mutation", mutation);
                        algorithm.addOperator("selection", selection);

                        // Execute the Algorithm
                        long initTime = System.currentTimeMillis();
                        SolutionSet population = algorithm.execute();
                        long estimatedTime = System.currentTimeMillis() - initTime;

                        // Result messages
                        population.printVariablesToFile(dirPath + "VAR_" + execution + ".txt");
                        population.printObjectivesToFile(dirPath + "FUN_" + execution + ".txt");

                        time.write(estimatedTime + "\n");
                        igd.write(igdIndicator.getIGD(population) + "\n");
                        r2.write(r2Indicator.R2(population) + "\n");

                        double percentage = ((((double) execution) + 1D) / 30D) * 100D;
                        printProgress(percentage);
                    }
                }
                System.out.println();
            }

            HypervolumeHandler hypervolume = new HypervolumeHandler();

            for (String algorithmString : ALGORITHMS) {
                String outputDir = "experiment/" + problem.getName() + "_" + problem.getNumberOfObjectives() + "/" + algorithmString + "/";
                for (int execution = 0; execution < 30; execution++) {
                    hypervolume.addParetoFront(outputDir + "FUN_" + execution + ".txt");
                }
            }

            for (String algorithmString : ALGORITHMS) {
                String outputDir = "experiment/" + problem.getName() + "_" + problem.getNumberOfObjectives() + "/" + algorithmString + "/";
                try (FileWriter hypervolumeWriter = new FileWriter(outputDir + "HYPERVOLUME.txt")) {
                    for (int execution = 0; execution < 30; execution++) {
                        hypervolumeWriter.write(hypervolume.getHypervolume(outputDir + "FUN_" + execution + ".txt", problem.getNumberOfObjectives()) + "\n");
                    }
                }
            }
        }
    }

    private static void gatherMetricsAverageAndStdDev(String... metrics) throws FileNotFoundException, IOException {
        printProgress(0);
        int i = 0;
        for (Problem problem : PROBLEMS) {
            int j = 0;
            for (String metric : metrics) {
                String dir = "experiment/" + problem.getName() + "_" + problem.getNumberOfObjectives() + "/";
                try (FileWriter fileWriter = new FileWriter(dir + metric.toUpperCase() + ".txt")) {
                    StringBuilder algorithmStringBuilder = new StringBuilder();
                    StringBuilder valuesStringBuilder = new StringBuilder();
                    for (String algorithm : ALGORITHMS) {
                        algorithmStringBuilder.append(algorithm).append(", ");
                        Scanner scanner = new Scanner(new File(dir + algorithm + "/" + metric.toUpperCase() + ".txt"));
                        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
                        while (scanner.hasNextDouble()) {
                            descriptiveStatistics.addValue(scanner.nextDouble());
                        }
                        valuesStringBuilder.append(descriptiveStatistics.getMean()).append(" (").append(descriptiveStatistics.getStandardDeviation()).append("), ");
                    }

                    algorithmStringBuilder.deleteCharAt(algorithmStringBuilder.length() - 1).deleteCharAt(algorithmStringBuilder.length() - 1);
                    valuesStringBuilder.deleteCharAt(valuesStringBuilder.length() - 1).deleteCharAt(valuesStringBuilder.length() - 1);
                    fileWriter.write(algorithmStringBuilder.toString() + "\n");
                    fileWriter.write(valuesStringBuilder.toString());
                }
                j++;
                double percentage = ((double) i * (double) metrics.length + (double) j) / ((double) metrics.length * (double) PROBLEMS.length) * 100D;
                printProgress(percentage);
            }
            i++;
        }
    }

    private static void runStatisticalComparsion(String... metrics) throws IOException, InterruptedException {
        printProgress(0);
        int i = 0;
        for (Problem problem : PROBLEMS) {
            int j = 0;
            for (String metric : metrics) {
                String dir = "experiment/" + problem.getName() + "_" + problem.getNumberOfObjectives() + "/";
                try (FileWriter friedman = new FileWriter(dir + metric + "_script.txt")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String algorithm : ALGORITHMS) {
                        stringBuilder.append(algorithm).append("<- c(");
                        Scanner scanner = new Scanner(new FileInputStream(dir + algorithm + "/" + metric.toUpperCase() + ".txt"));
                        while (scanner.hasNextLine()) {
                            String value = scanner.nextLine().trim();
                            if (!value.isEmpty()) {
                                stringBuilder.append(value).append(", ");
                            }
                        }
                        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
                        stringBuilder.append(")\n");
                        stringBuilder.append("\n");
                    }

                    stringBuilder.append("require(pgirmess)\n");
                    stringBuilder.append("AR1 <-cbind(");

                    StringBuilder contextNames = new StringBuilder();
                    for (String algorithm : ALGORITHMS) {
                        contextNames.append(algorithm).append(", ");
                    }
                    contextNames.delete(contextNames.length() - 2, contextNames.length());

                    stringBuilder.append(contextNames.toString()).append(")\n");
                    stringBuilder.append("result<-friedman.test(AR1)\n");
                    stringBuilder.append("\n");
                    stringBuilder.append("m<-data.frame(result$statistic,result$p.value)\n");
                    stringBuilder.append("write.csv2(m,file=\"./").append(dir).append(metric).append(".csv\")\n");
                    stringBuilder.append("\n");
                    stringBuilder.append("pos_teste<-friedmanmc(AR1)\n");
                    stringBuilder.append("write.csv2(pos_teste,file=\"./").append(dir).append(metric).append(".csv\")\n");
                    stringBuilder.append("png(file=\"./").append(dir).append(metric).append("boxplot.png\", width=1440, height=500)\n");
                    stringBuilder.append("boxplot(").append(contextNames.toString());

                    contextNames = new StringBuilder();
                    for (String algorithm : ALGORITHMS) {
                        contextNames.append("\"").append(algorithm).append("\", ");
                    }
                    contextNames.delete(contextNames.length() - 2, contextNames.length());

                    stringBuilder.append(", names=c(").append(contextNames.toString()).append("))");

                    friedman.write(stringBuilder.toString());
                }

                ProcessBuilder processBuilder = new ProcessBuilder("R", "--no-save");
                Process process = processBuilder.redirectInput(new File("./" + dir + metric + "_script.txt")).start();
                process.waitFor();
                j++;
                double percentage = ((double) i * (double) metrics.length + (double) j) / ((double) metrics.length * (double) PROBLEMS.length) * 100D;
                printProgress(percentage);
            }
            i++;
        }
    }

    private static void printProgress(double percentage) {
        String dots = "";
        for (int i = 1; i <= 50; i++) {
            if (i <= percentage / 2) {
                dots += ".";
            } else {
                dots += " ";
            }
        }
        System.out.printf("\rProgress [" + dots + "] %.0f%s", percentage, "%");
    }
}
