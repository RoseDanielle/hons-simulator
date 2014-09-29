package za.redbridge.simulator.gp.functions;

import org.epochx.epox.Node;
import org.epochx.tools.util.TypeUtils;
import za.redbridge.simulator.gp.types.Bearing;
import za.redbridge.simulator.gp.types.ProximityReading;
import za.redbridge.simulator.gp.types.RelativeCoordinate;

/**
 * Created by xenos on 9/10/14.
 */
public class ReadingToDistance extends Node {

    protected float range;

    public ReadingToDistance(float range){
        this(null, range);
    }

    public ReadingToDistance(final Node c1, float range){
        super(c1);
        this.range = range;
    }
    @Override
    public String getIdentifier(){
        return "READINGTODISTANCE";
    }

    public Float evaluate(){
        Object reading = getChild(0).evaluate();
        if(reading.getClass() == ProximityReading.class) {
            float c = ((ProximityReading)reading).getValue();
            return (1.0f - c) * range;
        }
        else return null;
    }

    @Override
    public Class<?> getReturnType(final Class<?> ... inputTypes) {
        if (inputTypes.length == 1 && inputTypes[0] == ProximityReading.class) {
            return Float.class;
        } else{
            return null;
        }
    }

    @Override
    public ReadingToDistance clone(){
        final ReadingToDistance clone = (ReadingToDistance) super.clone();
        clone.range = range;
        return clone;
    }

    @Override
    public ReadingToDistance newInstance() {
        return clone();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ReadingToDistance)) {
            return false;
        }

        return ((ReadingToDistance) obj).range == range;
    }
}