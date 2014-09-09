package za.redbridge.simulator.config;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by shsu on 2014/08/19.
 */

//parameters for experiment configuration
public class ExperimentConfig extends Config {

    private static final long DEFAULT_MAX_EPOCHS = 1000;
    private static final EvolutionaryAlgorithm DEFAULT_CONTROLLER_EA = EvolutionaryAlgorithm.NEAT;

    private static final String DEFAULT_MORPHOLOGY_FILEPATH= "sensorlist.yml";

    public enum EvolutionaryAlgorithm {
        NEAT, EVOLUTIONARY_STRATEGY, GENETIC_PROGRAMMING;
    }

    protected long maxEpochs;
    protected EvolutionaryAlgorithm algorithm;
    protected String robotFactory;
    protected String morphologyConfigFile;

    public ExperimentConfig() {
        this.maxEpochs = 100;
        this.algorithm = EvolutionaryAlgorithm.NEAT;
    }

    public ExperimentConfig(String filepath) {

        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //default values
        long maxEpochs = DEFAULT_MAX_EPOCHS;
        ExperimentConfig.EvolutionaryAlgorithm controllerEA = DEFAULT_CONTROLLER_EA;
        String morphologyFile = DEFAULT_MORPHOLOGY_FILEPATH;

        Map control = (Map) config.get("control");
        if (checkFieldPresent(control, "control")) {

            Number epochs = (Number) control.get("maxEpochs");
            if (checkFieldPresent(epochs, "control:maxEpochs")) {
                maxEpochs = epochs.longValue();
            }
        }

        Map phenotype = (Map) config.get("phenotype");
        if (checkFieldPresent(phenotype, "phenotype")) {

            String fact = (String) phenotype.get("factory");
            if (checkFieldPresent(fact, "phenotype:factory")) {
                factory = fact;
            }
        }

        Map ea = (Map) config.get("evolutionaryAlgorithm");
        if (checkFieldPresent(ea, "evolutionaryAlgorithm")) {

            String EA = (String) phenotype.get("controllerEA");
            if (checkFieldPresent(EA, "evolutionaryAlgorithm:controllerEA")) {

                if (EA.trim().equals(EvolutionaryAlgorithm.NEAT.name())) {
                    controllerEA = Enum.valueOf(EvolutionaryAlgorithm.class, EA);
                }
                else {
                    System.out.println("Only NEAT is supported in this version: using NEAT algorithm.");
                }
            }

        }

        Map morphology = (Map) config.get("morphology");
        if (checkFieldPresent(morphology, "morphology")) {

            String morphFile = (String) morphology.get("morphologyFileName");
            if (checkFieldPresent(morphFile, "morphology:morphologyFileName")) {
                morphologyFile = morphFile;
            }
        }

        this.maxEpochs = maxEpochs;
        this.algorithm = controllerEA;
        this.morphologyConfigFile = morphologyFile;
    }

    public ExperimentConfig(long maxEpochs, EvolutionaryAlgorithm algorithm,
                            String robotFactory, String morphologyConfigFile) {

        this.maxEpochs = maxEpochs;
        this.algorithm = algorithm;
        this.robotFactory = robotFactory;
        this.morphologyConfigFile = morphologyConfigFile;
    }

    public long getMaxEpochs() { return maxEpochs; }

    public EvolutionaryAlgorithm getEvolutionaryAlgorithm() { return algorithm; }

    public String getRobotFactory() { return robotFactory; }

    public String getMorphologyConfigFile() { return morphologyConfigFile; }

}
