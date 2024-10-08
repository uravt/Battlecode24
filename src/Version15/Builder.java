package Version15;

import battlecode.common.*;

import java.awt.*;
import java.util.Map;
import java.util.PriorityQueue;

import static Version15.RobotPlayer.*;


//Current Builder strategy
//if a builder is spawned on a flag it sits there and places bombs around
//if there a task to dp the builder goes to the task location and build a different trap based on context
//Otherwise it wanders around and places traps wherever
//in addition if a teammate current has a flag they don't place any bombs so that the bomb carrier can live.
public class Builder {
    //used for flagsitters
    static boolean isActive = true;
    final static int ROUND_TO_BUILD_EXPLOSION_BORDER = 0;
static int radius = 0;
    public static void runBuilder(RobotController rc) throws GameActionException {
        if(turnCount > 1000 && !SittingOnFlag) {
            role = roles.soldier;
            return;
        }
        radius = 7;

        if (SittingOnFlag) {
            //update where we want soldiers to spawn
            if(!Utilities.readBitSharedArray(rc, 1021)){
                int x;
                if(Soldier.knowFlag(rc))
                    x = RobotPlayer.findClosestSpawnLocationToCoordinatedTarget(rc);
                else if (Utilities.getClosestCluster(rc) != null) {
                    x = RobotPlayer.findClosestSpawnLocationToCluster(rc);
                } else {
                    x = RobotPlayer.findClosestSpawnLocationToCoordinatedBroadcast(rc);
                }
                if (x != -1){
                    //00
                    if(x == 0){
                        Utilities.editBitSharedArray(rc, 1023, false);
                        Utilities.editBitSharedArray(rc, 1022, false);
                    }
                    //01
                    else if(x == 1){
                        Utilities.editBitSharedArray(rc, 1022, false);
                        Utilities.editBitSharedArray(rc, 1023, true); }
                    //x == 2, desire 10
                    else if (x == 2){
                        Utilities.editBitSharedArray(rc, 1022, true);
                        Utilities.editBitSharedArray(rc, 1023, false);
                    }
                }
            }
            if(isActive)
                UpdateExplosionBorder2(rc);
            RobotInfo toHeal = Utilities.bestHeal(rc, rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam()));
            if(toHeal != null && rc.canHeal(toHeal.getLocation())){
                rc.heal(toHeal.getLocation());
            }
            if (rc.isActionReady()) {
                MapLocation toAttack = lowestHealth(rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent()));
                if (toAttack != null && rc.canAttack(toAttack)) {
                    if (rc.senseRobotAtLocation(toAttack).health <= rc.getAttackDamage()&&!rc.senseRobotAtLocation(toAttack).team.isPlayer())
                        Utilities.addKillToKillsArray(rc, turnsWithKills);
                    rc.attack(toAttack);
                }
            }
            //sitting where flag should be, but cant see any flags...
            //if we still cant see a flag 50 turns later, then until we do see one we're gonna assume this location should essentially be shut down
            if (rc.senseNearbyFlags(-1, rc.getTeam()).length == 0) {
                //shut down this spawn location for now
                if (countSinceSeenFlag > 30) {
                    rc.setIndicatorString("Dont come help me!");
                    isActive = false;
                    countSinceSeenFlag++;
                    if(countSinceSeenFlag > 40){
                        if(rc.getCrumbs() > 1000 || rc.getLevel(SkillType.BUILD) >= 4)
                            role = roles.offensiveBuilder;
                        else
                            role = roles.soldier;
                        return;
                    }
                    int locInt = Utilities.convertLocationToInt(rc.getLocation());
                    if(rc.readSharedArray(0) == locInt){
                        Utilities.editBitSharedArray(rc, 1018, false);
                    }
                    else if(rc.readSharedArray(1) == locInt){
                        Utilities.editBitSharedArray(rc, 1019, false);
                    }
                    else if(rc.readSharedArray(2) == locInt){
                        Utilities.editBitSharedArray(rc, 1020, false);
                    }
                    return;
                } else {
                    countSinceSeenFlag++;
                }
            } else {
                isActive = true;
                countSinceSeenFlag = 0;
                int locInt = Utilities.convertLocationToInt(rc.getLocation());
                if(rc.readSharedArray(0) == locInt){
                    Utilities.editBitSharedArray(rc, 1018, true);
                }
                else if(rc.readSharedArray(1) == locInt){
                    Utilities.editBitSharedArray(rc, 1019, true);
                }
                else if(rc.readSharedArray(2) == locInt){
                    Utilities.editBitSharedArray(rc, 1020, true);
                }
            }
            if (countSinceLocked != 0) {
                countSinceLocked++;
            }
            if (countSinceLocked >= 30) {
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


        } else//When there is no active task
        {
            MapLocation center = builderBombCircleCenter;//Will orbit around the flag
            if (center == null) {
                center = findClosestSpawnLocation(rc);
            }


            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if (enemies.length > 0) {
                MapLocation ops = Utilities.averageRobotLocation(enemies);
                Pathfinding.tryToMove(rc, rc.adjacentLocation(rc.getLocation().directionTo(ops).opposite()));
                if(rc.canBuild(TrapType.EXPLOSIVE,rc.adjacentLocation(rc.getLocation().directionTo(ops)))){
                    rc.canBuild(TrapType.EXPLOSIVE,rc.adjacentLocation(rc.getLocation().directionTo(ops)));
                }

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


    public static void UpdateExplosionBorder2(RobotController rc) throws GameActionException {//For flag sitters
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED);
        for (MapInfo t : mapInfos) {
            TrapType toBeBuilt = TrapType.STUN;
            if (rc.getCrumbs() > 3500)
                toBeBuilt = TrapType.EXPLOSIVE;
            if (!adjacentSpawnTrap(rc, t.getMapLocation()) && rc.canBuild(toBeBuilt, t.getMapLocation())) {
                rc.build(toBeBuilt, t.getMapLocation());
            }
        }
    }
    public static boolean adjacentSpawnTrap(RobotController rc, MapLocation location) throws GameActionException {
        int x = location.x;
        int y = location.y;
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                if(dx == 0 && dy == 0 || x + dx >= width || x + dx < 0 || y + dy >= height || y + dy < 0)
                    continue;
                MapLocation temp = new MapLocation(x + dx, y + dy);
                MapInfo tempInfo = rc.senseMapInfo(temp);
                if(tempInfo.isSpawnZone() && tempInfo.getTrapType() != TrapType.NONE)
                    return true;
            }
        }
        return false;
    }
    public static void UpdateExplosionBorder(RobotController rc) throws GameActionException {//For normal

        if(builderBombCircleCenter!=null && rc.getLocation().distanceSquaredTo(builderBombCircleCenter)<=Math.pow(radius-3,2)){
            return;
        }
        if(rc.getCrumbs()<350){
            return;
        }

        for (MapInfo t : rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED))
        {

            if(t.getMapLocation().directionTo(findClosestSpawnLocation(rc))==findClosestSpawnLocation(rc).directionTo(new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2)))
                continue;

            TrapType toBeBuilt = TrapType.STUN;
            if (rc.getCrumbs() > 3500)
                toBeBuilt = TrapType.EXPLOSIVE;
            /*if(rng.nextInt(30)==1)
                toBeBuilt = TrapType.WATER;*/
            if (rc.canBuild(toBeBuilt, t.getMapLocation())) {
                rc.build(toBeBuilt, t.getMapLocation());

            }

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
    public static MapLocation getAverageTrapLocation(RobotController rc, MapInfo[] possibleTraps)
    {
        int count = 0;
        int x = 0;
        int y = 0;
        for(MapInfo info : possibleTraps)
        {
            if(!info.getTrapType().equals(TrapType.NONE))
            {
                x += info.getMapLocation().x;
                y += info.getMapLocation().y;
                count++;
            }
        }

        MapLocation averageTrapLocation = null;
        if(count != 0)
        {
            averageTrapLocation = new MapLocation(x / count, y / count);
        }
        return averageTrapLocation;
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
