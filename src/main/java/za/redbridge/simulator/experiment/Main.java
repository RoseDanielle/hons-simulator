package za.redbridge.simulator.experiment;

import org.epochx.gp.op.init.RampedHalfAndHalfInitialiser;
import org.epochx.gp.representation.GPCandidateProgram;
import org.epochx.life.GenerationListener;
import org.epochx.life.Life;
import org.epochx.op.selection.TournamentSelector;
import org.epochx.representation.CandidateProgram;
import org.epochx.stats.StatField;
import org.epochx.stats.Stats;
import org.epochx.tools.eval.MalformedProgramException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.ExperimentConfig;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HeterogeneousRobotFactory;
import za.redbridge.simulator.gp.AgentModel;
import za.redbridge.simulator.gp.CustomStatFields;
import za.redbridge.simulator.khepera.BottomProximitySensor;
import za.redbridge.simulator.khepera.UltrasonicSensor;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.phenotype.GPPhenotype;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.TypedProximityAgentSensor;

import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

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
        sensors.add(new UltrasonicSensor(0f, 0f));
        sensors.add(new UltrasonicSensor((float)Math.PI/2, 0f));
        //sensors.add(new UltrasonicSensor((float)Math.PI, 0f));
        sensors.add(new UltrasonicSensor((float)(3*Math.PI/2), 0f));

        List<Class> detectables = new ArrayList<>();
        detectables.add(RobotObject.class);
        detectables.add(ResourceObject.class);
        detectables.add(WallObject.class);

        sensors.add(new TypedProximityAgentSensor(detectables, (float)Math.PI/4, 0.0f, 1f, 0.2f));
        sensors.add(new TypedProximityAgentSensor(detectables, (float)(7*Math.PI/4), 0.0f, 1f, 0.2f));

        sensors.add(new BottomProximitySensor());

        AgentModel model = new AgentModel(sensors, simulationConfiguration, experimentConfiguration);
        model.setNoGenerations(200);
        model.setMaxInitialDepth(5);
        model.setMaxDepth(7);
        model.setPopulationSize(1000);
        model.setPoolSize(model.getPopulationSize() / 2);
        model.setProgramSelector(new TournamentSelector(model, 4));
        model.setNoRuns(1);
        model.setNoElites(model.getPopulationSize() / 4);
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
                System.out.println("Best Individual Fitness: " + min);
                System.out.println("Best Team fitness: " + (Double)s.getStat(CustomStatFields.GEN_TEAM_FITNESS_MIN));
                List<CandidateProgram> bestTeam = (List<CandidateProgram>)s.getStat(CustomStatFields.GEN_FITTEST_TEAM);
                System.out.println("Best team: {\"" + bestTeam.stream().map(o -> o.toString()).collect(Collectors.joining("\", \"")) + "\"}");
                System.out.println("Avg: " + avg);

                List<CandidateProgram> pop = (List<CandidateProgram>) s.getStat(StatField.GEN_POP_SORTED_DESC);
                List<String> distinctPop = (List<String>) pop.stream().map(c -> c.toString()).distinct().collect(Collectors.toList());
                DoubleStream fitnesses = pop.stream().mapToDouble(p -> p.getFitness());
                Double stddev = Math.sqrt(fitnesses.map(f -> Math.pow(f - avg, 2)).average().orElse(0.0));
                System.out.println("Stddev: " + stddev);
                System.out.println("Distinct programs: " + distinctPop.size());

                s.print(StatField.GEN_FITTEST_PROGRAM);
                System.out.println("Best 20 individuals: {\"" + distinctPop.stream().limit(20).collect(Collectors.joining("\", \"")) + "\"}");
                Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime);
                System.out.println("Elapsed: " + elapsed.toString());

                //stop if diversity is low enough
                if(stddev < 0.2 && distinctPop.size() < pop.size()/4.0){
                    System.out.println("Diversity below threshold; stopping.");
                    model.setTerminationFitness(0.0);
                }
                counter++;
            }
            @Override
            public void onGenerationStart(){}
        }
        Life.get().addGenerationListener(new GenerationTrackingListener());

        //if we need to show a visualisation
        if (options.showVisuals()) {
            String[] trees = {"WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 IF(READINGPRESENT(PS1) B1.57 B4.71))))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B3.14)) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) BEARINGFROMCOORDINATE(READINGTOCOORDINATE(PS1)) B1.57) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B4.71) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B3.14 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) IF(READINGPRESENT(PS1) B1.57 B4.71) B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B.00) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) B.00 IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) B4.71 IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B5.50)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B4.71 IF(READINGPRESENT(PS1) B1.57 B4.71)) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) B4.71 IF(READINGPRESENT(PS1) B1.57 B4.71)) B3.14)) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) BEARINGFROMCOORDINATE(READINGTOCOORDINATE(PS1)) B1.57) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) B4.71 IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) BEARINGFROMCOORDINATE(READINGTOCOORDINATE(PS1)) B1.57) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B3.14 B1.57) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) RANDOMBEARING() B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) B.00 B3.14)) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) BEARINGFROMCOORDINATE(READINGTOCOORDINATE(PS1)) B1.57) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B3.14)) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B1.57 B4.71) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(PS1) B1.57 B4.71) IF(READINGPRESENT(PS1) B1.57 B4.71)))", "WHEELDRIVEFROMBEARING(IF(READINGPRESENT(TS3) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)) IF(READINGPRESENT(PS1) IF(READINGPRESENT(TS3) IF(READINGISOFTYPE(TS3 ROBOT) B4.71 B.00) IF(READINGPRESENT(PS1) B1.57 B4.71)) B4.71)))"};
            /*
            try {
                FileWriter fw = new FileWriter("tree.dot");
                fw.write(new EpoxRenderer().dotRender(root));
                fw.close();
            }catch(IOException e){
                System.err.print("Could not write dot file");
            }
            */
            List<Phenotype> phenotypes = new ArrayList<>();
            for(String t : trees){
                GPPhenotype p = new GPPhenotype(sensors.stream().map(sen -> sen.clone()).collect(Collectors.toList()),new GPCandidateProgram(model.getParser().parse(t), model), model.getInputs());
                phenotypes.add(p);
            }
            HeterogeneousRobotFactory robotFactory = new HeterogeneousRobotFactory(phenotypes, simulationConfiguration.getRobotMass(),
                    simulationConfiguration.getRobotRadius(), simulationConfiguration.getRobotColour());

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
        }

    }

    public String getExperimentConfig() { return experimentConfig; }
    public String getSimulationConfig() { return simulationConfig; }
    public boolean showVisuals() { return showVisuals; }


}
