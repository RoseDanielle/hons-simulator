package za.redbridge.simulator.factories;

import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.ea.hetero.CCHIndividual;
import za.redbridge.simulator.phenotype.HeteroNEATPhenotype;
import za.redbridge.simulator.phenotype.Phenotype;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by racter on 2014/10/05.
 * Fixed-morphology, variable controller team factory.
 */
public class TeamPhenotypeFactory {

    private final MorphologyConfig morphologyConfig;
    private final Set<CCHIndividual> controllers;

    public TeamPhenotypeFactory(MorphologyConfig morphologyConfig, Set<CCHIndividual> controllers) {

        this.morphologyConfig = morphologyConfig;
        this.controllers = controllers;
    }

    public Set<Phenotype> generatePhenotypeTeam() {

        Set<Phenotype> team = new HashSet<>();

        for (CCHIndividual network: controllers) {

            team.add(new HeteroNEATPhenotype(morphologyConfig.getSensors(), network,
                    morphologyConfig.getTotalReadingSize()));
        }

        return team;
    }
}