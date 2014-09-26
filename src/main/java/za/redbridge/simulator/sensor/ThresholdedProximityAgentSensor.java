package za.redbridge.simulator.sensor;

import za.redbridge.simulator.sensor.sensedobjects.SensedObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by shsu on 2014/09/15.
 */
public class ThresholdedProximityAgentSensor extends ProximityAgentSensor {

    private static final int readingSize = 1;
    private double sensitivity;

    public ThresholdedProximityAgentSensor(float bearing, float orientation, float range, float fieldOfView) {
        super(bearing, orientation, range, fieldOfView);

        sensitivity = 0.0;
    }

    @Override
    protected void provideObjectReading(List<SensedObject> objects, List<Double> output) {
        double reading = 0.0;
        if (!objects.isEmpty()) {
            reading = 1 - Math.min(objects.get(0).getDistance() / range, 1.0);
        }

        if (reading >= (1 - sensitivity)) {
           output.add(reading);
        }
        else {
            output.add(0.0);
        }
    }

    @Override
    public void readAdditionalConfigs(Map<String, Object> map) throws ParseException {

        additionalConfigs = map;

        Number sens = (Number) map.get("sensitivity");
        if (checkFieldPresent(sens, "sensitivity")) {
            double sensValue = sens.doubleValue();

            if (sensValue > 1 || sensValue < 0) {
                throw new ParseException("Sensitivity value for ThresholdedProximityAgentSensor must be between 0 and 1", 0);
            }

            this.sensitivity = sensValue;
        }
        else {
            throw new ParseException("No sensitivity value found for ThresholdedProximityAgentSensor.", 0);
        }

    }

    @Override
    public int getReadingSize() { return readingSize; }

    @Override
    public ThresholdedProximityAgentSensor clone() {

        ThresholdedProximityAgentSensor cloned = new ThresholdedProximityAgentSensor(bearing, orientation, range, fieldOfView);

        try {
            cloned.readAdditionalConfigs(additionalConfigs);
        }
        catch (ParseException p) {
            System.out.println("Clone failed.");
            p.printStackTrace();
            System.exit(-1);
        }

        return cloned;
    }

    public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }

    public double getSensitivity() { return sensitivity; }
}