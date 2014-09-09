package za.redbridge.simulator.ea;

import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;

import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.config.ExperimentConfig;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.factories.RobotFactory;

/**
 * Created by shsu on 2014/08/13.
 */

public class ScoreCalculator implements CalculateScore {

    private RobotFactory robotFactory;
    private ResourceFactory resourceFactory;
    private SimConfig config;

    public ScoreCalculator(ExperimentConfig experimentConfig, SimConfig config) {
        this.config = config;
    }

    @Override
    public double calculateScore(MLMethod method) {
        Simulation currentSimulation = new Simulation(config);
        return currentSimulation.getFitness();
    }

    @Override
    public boolean shouldMinimize() {
        return false;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }


}
