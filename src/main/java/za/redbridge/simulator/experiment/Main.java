package za.redbridge.simulator.experiment;

import org.epochx.epox.Node;
import org.epochx.gp.op.init.RampedHalfAndHalfInitialiser;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.life.GenerationListener;
import org.epochx.life.Life;
import org.epochx.op.selection.FitnessProportionateSelector;
import org.epochx.op.selection.TournamentSelector;
import org.epochx.stats.StatField;
import org.epochx.stats.Stats;
import org.epochx.tools.eval.MalformedProgramException;
import org.jbox2d.dynamics.Fixture;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.ExperimentConfig;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.gp.AgentModel;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.phenotype.GPPhenotype;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.ProximityAgentSensor;

//entry point into simulator

/**
 * Created by racter on 2014/08/19.
 */
public class Main {

    //config files for this experiment

    @Option(name="--experiment-config", usage="Filename for experiment configuration", metaVar="<experiment config>")
    private String experimentConfig;

    @Option (name="--simulation-config", usage="Filename for simulation configuration", metaVar="<simulation config>")
    private String simulationConfig;

    @Option (name="--show-visuals", aliases="-v", usage="Show visualisation for simulation")
    private boolean showVisuals = false;

    public static void main (String[] args) throws MalformedProgramException{

        Main options = new Main();
        CmdLineParser parser = new CmdLineParser(options);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException c) {
            System.out.println("Error parsing command-line arguments.");
            System.exit(1);
        }

        ExperimentConfig experimentConfiguration = new ExperimentConfig(options.getExperimentConfig());
        SimConfig simulationConfiguration = new SimConfig(options.getSimulationConfig());

        //TODO: work with multiple morphology configs (specifically, filter sensitivities)
        MorphologyConfig morphologyConfig = null;

        try {
            morphologyConfig = new MorphologyConfig(experimentConfiguration.getMorphologyConfigFile());
        }
        catch(ParseException p) {
            System.out.println("Error parsing morphology file.");
            p.printStackTrace();
        }

        List<AgentSensor> sensors = new ArrayList<>();
        AgentSensor leftSensor = new ResourceProximitySensor((float) ((7 / 4.0f) * Math.PI), 0f, 1f, 0.2f);
        AgentSensor forwardSensor = new ResourceProximitySensor(0f, 0f, 1f, 0.2f);
        AgentSensor rightSensor = new ResourceProximitySensor((float) (Math.PI/4), 0f, 1f, 0.2f);

        sensors.add(leftSensor);
        sensors.add(forwardSensor);
        sensors.add(rightSensor);

        AgentModel model = new AgentModel(sensors, simulationConfiguration, experimentConfiguration);
        model.setNoGenerations(100);
        model.setMaxInitialDepth(5);
        model.setMaxDepth(7);
        model.setPopulationSize(200);
        model.setPoolSize(model.getPopulationSize() / 2);
        model.setProgramSelector(new TournamentSelector(model, 5));
        model.setNoRuns(1);
        model.setNoElites(model.getPopulationSize() / 10);
        model.setInitialiser(new RampedHalfAndHalfInitialiser(model));
        model.setMutationProbability(0.1);
        model.setCrossoverProbability(0.9);
        model.setTerminationFitness(Double.NEGATIVE_INFINITY);
        class GenerationTrackingListener implements GenerationListener{
            private int counter = 0;
            private Long startTime = null;
            @Override
            public void onGenerationEnd() {
                if(startTime == null) startTime = System.currentTimeMillis();
                Stats s = Stats.get();
                System.out.println();
                System.out.println("Generation " + (counter+1));
                Double min = (Double)s.getStat(StatField.GEN_FITNESS_MIN);
                Double avg = (Double)s.getStat(StatField.GEN_FITNESS_AVE);
                System.out.println(); //newline after the dots
                System.out.println("Fitness: " + min);
                System.out.println("Avg: " + avg);
                s.print(StatField.GEN_FITTEST_PROGRAM);
                Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime);
                System.out.println("Elapsed: " + elapsed.toString());
                counter++;
            }
            @Override
            public void onGenerationStart(){}
        }
        Life.get().addGenerationListener(new GenerationTrackingListener());

        //if we need to show a visualisation
        if (options.showVisuals()) {
            String tree = "WHEELDRIVEFROMFLOATS(1.0 IF(IF(GT(0.0 0.0) GT(1.0 0.0) GT(1.0 0.0)) IF(GT(1.0 0.0) READINGTODISTANCE(S2) 0.0) 0.0))";
            Node root = model.getParser().parse(tree);
            GPCandidateProgram cand = new GPCandidateProgram(root, model);
            HomogeneousRobotFactory robotFactory = new HomogeneousRobotFactory(
                    new GPPhenotype(sensors, cand, model.getInputs()), simulationConfiguration.getRobotMass(),
                    simulationConfiguration.getRobotRadius(), simulationConfiguration.getRobotColour(), simulationConfiguration.getObjectsRobots());

            Simulation simulation = new Simulation(simulationConfiguration, robotFactory);
            SimulationGUI video = new SimulationGUI(simulation);

            //new console which displays this simulation
            Console console = new Console(video);
            console.setVisible(true);
        }
        else {
            System.out.println("Commencing experiment");
            //headless option
            model.run();
            System.out.println("Experiment finished. Best fitness: ");
            Stats s = Stats.get();
            s.print(StatField.ELITE_FITNESS_MIN);
            System.out.println("Best programs: ");
            s.print(StatField.GEN_FITTEST_PROGRAMS);
        }

    }

    private static class ResourceProximitySensor extends ProximityAgentSensor {

        public ResourceProximitySensor(float bearing) {
            super(bearing);
        }

        public ResourceProximitySensor(float bearing, float orientation, float range, float fieldOfView) {
            super(bearing, orientation, range, fieldOfView);
        }

        @Override
        public boolean isRelevantObject(PhysicalObject otherObject) {
            return otherObject instanceof ResourceObject;
        }

        @Override
        protected boolean filterOutObject(PhysicalObject object) {
            if(object instanceof ResourceObject) return ((ResourceObject) object).isCollected();
            else return false;
        }
    }

    public String getExperimentConfig() { return experimentConfig; }
    public String getSimulationConfig() { return simulationConfig; }
    public boolean showVisuals() { return showVisuals; }


}
