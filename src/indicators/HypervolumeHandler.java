/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indicators;

import jmetal.core.SolutionSet;
import jmetal.qualityIndicator.Hypervolume;
import jmetal.qualityIndicator.util.MetricsUtil;

/**
 *
 * @author giovaniguizzo
 */
public class HypervolumeHandler {

    private SolutionSet population;
    private final MetricsUtil metricUtil;
    private final Hypervolume hypervolume;

    public HypervolumeHandler() {
        this.population = new SolutionSet();
        this.metricUtil = new MetricsUtil();
        this.hypervolume = new Hypervolume();
    }

    public void addParetoFront(SolutionSet front) {
        population = population.union(front);
    }

    public void addParetoFront(String path) {
        population = population.union(metricUtil.readSolutionSet(path));
    }

    public double getHypervolume(String front, int numberOfObjectives) {
        return getHypervolume(metricUtil.readSolutionSet(front), numberOfObjectives);
    }

    public double getHypervolume(SolutionSet front, int numberOfObjectives) {
        double[][] populationMatrix = population.writeObjectivesToMatrix();
        if (populationMatrix.length != 0) {
            double[] max = metricUtil.getMaximumValues(populationMatrix, numberOfObjectives);
            double[][] worstFront = new double[numberOfObjectives][numberOfObjectives];
            for (int i = 0; i < numberOfObjectives; i++) {
                double objective = max[i];
                worstFront[i][i] = objective;
                for (int j = 0; j < numberOfObjectives; j++) {
                    if (i != j) {
                        worstFront[i][j] = 0;
                    }
                }
            }
            return hypervolume.hypervolume(front.writeObjectivesToMatrix(), worstFront, numberOfObjectives);
        }
        return 0D;
    }

}
