package Version9;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;

import static Version9.RobotPlayer.findClosestSpawnLocation;
import static Version9.RobotPlayer.*;


//Current Builder strategy
//if a builder is spawned on a flag it sits there and places bombs around
//if there a task to dp the builder goes to the task location and build a different trap based on context
//Otherwise it wanders around and places traps wherever
//in addition if a teammate current has a flag they don't place any bombs so that the bomb carrier can live.
public class Builder {
    final static int ROUND_TO_BUILD_EXPLOSION_BORDER = 0;
static int radius = 0;
    public static void runBuilder(RobotController rc) throws GameActionException {
        radius = 7;

        if (builderBombCircleCenter == null && rc.getRoundNum() >= 3) {
            int[] distances = new int[3];
            distances[0] = rc.getLocation().distanceSquaredTo(Utilities.convertIntToLocation(rc.readSharedArray(0)));
            distances[1] = rc.getLocation().distanceSquaredTo(Utilities.convertIntToLocation(rc.readSharedArray(1)));
            distances[2] = rc.getLocation().distanceSquaredTo(Utilities.convertIntToLocation(rc.readSharedArray(2)));
            int lowestIndex = 0;
            if (distances[0] > distances[1]) {
                lowestIndex = 1;
            }
            if (distances[lowestIndex] > distances[2]) {
                lowestIndex = 2;
            }
            builderBombCircleCenter = Utilities.convertIntToLocation(rc.readSharedArray(lowestIndex));


        }
        if (turnCount == 150) {
            for (int i = 6; i < 28; i++) {
                Utilities.clearTask(rc, i);
            }
        }
        if (!rc.isSpawned()) {
            builderBombCircleCenter = null;
            return;
        }
        Task t = Utilities.readTask(rc);
        if (SittingOnFlag) {
            //sitting where flag should be, but cant see any flags...
            //if we still cant see a flag 50 turns later, then until we do see one we're gonna assume this location should essentially be shut down
            if (rc.senseNearbyFlags(-1, rc.getTeam()).length == 0) {
                //shut down this spawn location for now
                if (countSinceSeenFlag > 40) {
                    rc.setIndicatorString("Dont come help me!");
                    return;
                } else {
                    countSinceSeenFlag++;
                }
            } else {
                countSinceSeenFlag = 0;
            }
            if (countSinceLocked != 0) {
                countSinceLocked++;
            }
            if (countSinceLocked >= 20) {
                countSinceLocked = 0;
                Utilities.editBitSharedArray(rc, 1021, false);
            }
            //check if nearby enemies are coming to attack, call for robots to prioritize spawning at ur flag
            if (rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, rc.getTeam().opponent()).length > rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, rc.getTeam()).length) {
                //spawn everyone in 0-8 if possible, also lock so it wont cycle
                if (rc.getLocation().equals(Utilities.convertIntToLocation(rc.readSharedArray(0)))) {
                    Utilities.editBitSharedArray(rc, 1022, false);
                    Utilities.editBitSharedArray(rc, 1023, false);
                    //lock
                    Utilities.editBitSharedArray(rc, 1021, true);
                } else if (rc.getLocation().equals(Utilities.convertIntToLocation(rc.readSharedArray(1)))) {
                    Utilities.editBitSharedArray(rc, 1022, false);
                    Utilities.editBitSharedArray(rc, 1023, true);
                    //lock
                    Utilities.editBitSharedArray(rc, 1021, true);
                } else {
                    Utilities.editBitSharedArray(rc, 1022, true);
                    Utilities.editBitSharedArray(rc, 1023, false);
                    //lock
                    Utilities.editBitSharedArray(rc, 1021, true);
                }
                countSinceLocked++;
            }
            if (rc.getRoundNum() > ROUND_TO_BUILD_EXPLOSION_BORDER) {
                UpdateExplosionBorder(rc);
            }
        } else if (/*t != null*/ false) {//there is a task to do
            Pathfinding.bugNav2(rc, t.location);
            if (locationIsActionable(rc, t.location)) {
                TrapType toBeBuilt = TrapType.STUN;
                if (rc.getCrumbs() > 3500)
                    toBeBuilt = TrapType.EXPLOSIVE;
                if (rc.canBuild(toBeBuilt, t.location))
                    rc.build(toBeBuilt, t.location);
                Utilities.clearTask(rc, t.arrayIndex);
            }
        } else//When there is no active task
        {
            MapLocation center = builderBombCircleCenter;//Will orbit around the flag
            if (center == null) {
                center = findClosestSpawnLocation(rc);
            }


            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemies.length > 0) {
                Pathfinding.tryToMove(rc, rc.adjacentLocation(rc.getLocation().directionTo(Utilities.averageRobotLocation(enemies)).opposite()));
            }
            UpdateExplosionBorder(rc);
            rc.setIndicatorLine(rc.getLocation(), center, 255, 255, 255);
            if (rc.getLocation().distanceSquaredTo(center) < Math.pow(radius - 1, 2)) {
                Pathfinding.tryToMove(rc, rc.adjacentLocation(center.directionTo(rc.getLocation())));
            } else if (rc.getLocation().distanceSquaredTo(center) > Math.pow(radius + 1, 2)) {
                Pathfinding.tryToMove(rc, rc.adjacentLocation(center.directionTo(rc.getLocation()).opposite()));

            } else if (true) {

                if ((rc.getRoundNum() / 40) % 2 == 0) {
                    if (rc.canFill(rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateLeft().rotateLeft())))
                        rc.fill((rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateLeft().rotateLeft())));
                    Pathfinding.tryToMove(rc, rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateLeft().rotateLeft()));
                } else {
                    if (rc.canFill(rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateRight().rotateRight())))
                        rc.fill((rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateRight().rotateRight())));
                    Pathfinding.tryToMove(rc, rc.adjacentLocation(center.directionTo(rc.getLocation()).rotateRight().rotateRight()));

                }
            }
        }
    }



    private static int numBombsNearby(RobotController rc) throws GameActionException {
        int out = 0;
        for(MapInfo i: rc.senseNearbyMapInfos(36)){
            if(!i.getTrapType().equals(TrapType.NONE)){
                out++;
            }
        }
        return out;
    }

    private static Direction directionToMove(RobotController rc) {
        return directions[rng.nextInt(8)];
    }

    private static int distanceFromNearestSpawnLocation(RobotController rc){
        return 0;
    }

    public static boolean locationIsActionable(RobotController rc, MapLocation m) throws GameActionException {
        MapInfo[] ActionableTiles = rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED);
        for (MapInfo curr : ActionableTiles)
        {
            if (m.equals(curr.getMapLocation()))
            {
                return true;
            }
        }
        return false;
    }

    public static void UpdateExplosionBorder(RobotController rc) throws GameActionException {

        if(rc.getCrumbs()<300 && !SittingOnFlag || builderBombCircleCenter!=null && rc.getLocation().distanceSquaredTo(builderBombCircleCenter)<=Math.pow(radius-3,2)){
            return;
        }
        int trapsPlaced = 0;
        boolean out = false;
        for (MapInfo t : rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED))
        {
            if(t.getMapLocation().directionTo(findClosestSpawnLocation(rc))==findClosestSpawnLocation(rc).directionTo(new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2)))
                continue;

            TrapType toBeBuilt = TrapType.STUN;
            if (rc.getCrumbs() > 3500)
                toBeBuilt = TrapType.EXPLOSIVE;
            if(rng.nextInt(10)==1)
                toBeBuilt = TrapType.WATER;
            if (rc.canBuild(toBeBuilt, t.getMapLocation())) {
                rc.build(toBeBuilt, t.getMapLocation());
                trapsPlaced++;
                out=true;

            }
            if(trapsPlaced>1)
                return;
        }
    }

    public static void runBuild(RobotController rc, RobotInfo[] enemies) throws GameActionException
    {
        PriorityQueue<MapLocationWithDistance> bestTrapLocations = getBestBombLocations(rc, enemies);
        while (!bestTrapLocations.isEmpty())
        {
            MapLocation currentTryLocation = bestTrapLocations.remove().location;
            if (currentTryLocation != null && rc.canBuild(TrapType.STUN, currentTryLocation) && rc.getCrumbs() > 300) {
                rc.build(TrapType.STUN, currentTryLocation);
            }
        }
    }

    public static PriorityQueue<MapLocationWithDistance> getBestBombLocations(RobotController rc, RobotInfo[] enemies)
    {
        PriorityQueue<MapLocationWithDistance> bestLocations = new PriorityQueue<>();
        if(enemies.length == 0)
        {
            for(Direction direction : directions)
            {
                MapLocation tempLocation = rc.adjacentLocation(direction);
                MapLocation closestFlag = findClosestBroadcastFlags(rc);
                if(closestFlag != null && rc.canBuild(TrapType.STUN, tempLocation))
                {
                    bestLocations.add(new MapLocationWithDistance(tempLocation, tempLocation.distanceSquaredTo(closestFlag)));
                }
            }
        }
        else
        {
            MapLocation averageEnemyLocation = Utilities.averageRobotLocation(enemies);
            for(Direction direction : directions)
            {
                MapLocation tempLocation = rc.adjacentLocation(direction);
                if(rc.canBuild(TrapType.STUN, tempLocation))
                {
                    bestLocations.add(new MapLocationWithDistance(tempLocation, tempLocation.distanceSquaredTo(averageEnemyLocation)));
                }
            }
        }
        return bestLocations;
    }
}

class MapLocationWithDistance implements Comparable
{
    public MapLocation location;
    public int distanceSquared;

    public MapLocationWithDistance(MapLocation ml, int distanceSquared)
    {
        location = ml;
        this.distanceSquared = distanceSquared;
    }
    public int compareTo(Object other)
    {
        return Integer.compare(distanceSquared,((MapLocationWithDistance) other).distanceSquared);
    }
}
