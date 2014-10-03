package za.redbridge.simulator.gp.functions;

import org.epochx.epox.Node;
import za.redbridge.simulator.gp.types.WheelDrive;

/**
 * Created by xenos on 9/10/14.
 */
public class WheelDriveSpotTurnLeft extends Node {

    public WheelDriveSpotTurnLeft(){
        this(null);
    }

    public WheelDriveSpotTurnLeft(final Node c1){
        super(c1);
    }
    @Override
    public String getIdentifier(){
        return "WHEELDRIVESPOTTURNLEFT";
    }

    public WheelDrive evaluate(){
        return new WheelDrive(-1.0f, 1.0f);
    }

    @Override
    public Class<?> getReturnType(final Class<?> ... inputTypes) {
        if (inputTypes.length == 0) return WheelDrive.class;
        else return null;
    }
}