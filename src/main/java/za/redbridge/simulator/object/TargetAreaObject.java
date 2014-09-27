package za.redbridge.simulator.object;

import org.jbox2d.collision.AABB;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sim.engine.SimState;
import sim.util.Double2D;
import za.redbridge.simulator.ea.FitnessFunction;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.Collideable;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;

/**
 * Created by shsu on 2014/08/13.
 */
public class TargetAreaObject extends PhysicalObject implements Collideable {

    private static final boolean ALLOW_REMOVAL = true;

    private int width, height;
    private final AABB aabb;

    private FitnessFunction fitnessFunction;

    //total fitness value for the agents in this simulation. unfortunately fitness is dead tied to forage area and
    //how much stuff is in there.
    private double totalFitness;

    //hash set so that object values only get added to forage area once
    private final Set<ResourceObject> containedObjects = new HashSet<>();
    private final List<Fixture> watchedFixtures = new ArrayList<>();

    //keeps track of what has been pushed into this place

    public TargetAreaObject(World world, Double2D pos, int width, int height,
            FitnessFunction fitnessFunction) {
        super(createPortrayal(width, height), createBody(world, pos, width, height));

        this.fitnessFunction = fitnessFunction;
        totalFitness = 0;
        this.width = width;
        this.height = height;

        aabb = getBody().getFixtureList().getAABB(0);
    }

    protected static Portrayal createPortrayal(int width, int height) {
        Paint areaColour = new Color(31, 110, 11, 100);
        return new RectanglePortrayal(width, height, areaColour, true);
    }

    protected static Body createBody(World world, Double2D position, int width, int height) {
        BodyBuilder bb = new BodyBuilder();
        return bb.setBodyType(BodyType.STATIC)
                .setPosition(position)
                .setRectangular(width, height)
                .setSensor(true)
                .build(world);
    }

    @Override
    public void step(SimState simState) {
        super.step(simState);

        // Check if any objects have passed into the target area completely or have left
        for (Fixture fixture : watchedFixtures) {
            ResourceObject resource = (ResourceObject) fixture.getBody().getUserData();
            if (aabb.contains(fixture.getAABB(0))) {
                // Object moved completely into the target area
                if (containedObjects.add(resource)) {
                    resource.setCollected(true);
                    resource.getPortrayal().setPaint(Color.CYAN);
                    incrementTotalObjectValue(resource);
                }
            } else if (ALLOW_REMOVAL) {
                // Object moved out of completely being within the target area
                if (containedObjects.remove(resource)) {
                    resource.setCollected(false);
                    resource.getPortrayal().setPaint(Color.MAGENTA);
                    decrementTotalObjectValue(resource);
                }
            }
        }
    }

    //these also update the overall fitness value
    private void incrementTotalObjectValue(ResourceObject resource) {
        totalFitness += fitnessFunction.calculateFitness(resource);
    }

    private void decrementTotalObjectValue(ResourceObject resource) {
        totalFitness -= fitnessFunction.calculateFitness(resource);
    }

    public double getTotalFitness() {
        return totalFitness;
    }

    public int getNumberOfContainedResources() {
        return containedObjects.size();
    }

    public AABB getAabb() {
        return aabb;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void handleBeginContact(Contact contact, Fixture otherFixture) {
        if (!watchedFixtures.contains(otherFixture)) {
            watchedFixtures.add(otherFixture);
        }
    }

    @Override
    public void handleEndContact(Contact contact, Fixture otherFixture) {
        // Remove from watch list
        watchedFixtures.remove(otherFixture);

        // Remove from the score
        if (ALLOW_REMOVAL) {
            ResourceObject resource = (ResourceObject) otherFixture.getBody().getUserData();
            if (containedObjects.remove(resource)) {
                resource.setCollected(false);
                resource.getPortrayal().setPaint(Color.MAGENTA);
                decrementTotalObjectValue(resource);
            }
        }
    }

    @Override
    public boolean isRelevantObject(PhysicalObject object) {
        return object instanceof ResourceObject;
    }

}
