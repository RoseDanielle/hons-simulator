package za.redbridge.simulator.interfaces;

import sim.util.Double2D;
import za.redbridge.simulator.engine.Sensor;
import za.redbridge.simulator.engine.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface to the agent.
 */
public interface Phenotype {

    /**
     * Returns the list of sensors this bot has on it
     * @return a List of Sensor objects
     */
    List<Sensor> getSensors();

    /**
     * Process the sensor inputs and provide actuator outputs
     * @param list the current environment state
     * @return The vector for driving the actuators
     */
    Double2D step(List<SensorReading> list);

    /**
     * Represents a controller that outputs a one vector. For testing.
     */
    public static final Phenotype DUMMY_PHENOTYPE = new Phenotype() {
        private final Double2D one = new Double2D(1.0, 1.0);

        @Override
        public Double2D step(List<SensorReading> l) {
            return one;
        }

        public @Override List<Sensor> getSensors() {
            return new ArrayList<Sensor>();
        }
    };
}
