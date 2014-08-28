package za.redbridge.simulator.interfaces;

import org.jbox2d.dynamics.World;

import za.redbridge.simulator.PlacementArea;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.TargetAreaObject;

import java.util.List;

public interface RobotFactory {
    void placeInstances(PlacementArea.ForType<RobotObject> placementArea, World world, int quantity,
                        SimConfig.Direction targetAreaPlacement);
}
