package Version2;

import battlecode.common.*;

import static Version2.RobotPlayer.*;

public class Soldier
{
    public static void runSoldier(RobotController rc) throws GameActionException {
        boolean hasDirection = false;
        //blank declaration, will be set by something
        Direction dir = Direction.CENTER;
        //if we have an enemy flag, bring it to the closest area
        MapLocation closestSpawnLoc = findClosestSpawnLocation(rc);
        if(closestSpawnLoc != null){
            dir = rc.getLocation().directionTo(closestSpawnLoc);
            if(rc.canMove(dir)) {
                rc.move(dir);
                hasDirection = true;
            }
        }
        if(!hasDirection) {
            //if we can see a flag, go towards it
            MapLocation closestFlagLoc = findClosestSpawnLocation(rc);
            if(closestFlagLoc != null){
                dir = rc.getLocation().directionTo(closestFlagLoc);
                hasDirection = true;
            }
        }
        if(!hasDirection) {
            //finally, find the closest enemy broadcasted flag
            MapLocation closestBroadcasted = findClosestBroadcastFlags(rc);
            if(closestBroadcasted != null){
                dir = rc.getLocation().directionTo(closestBroadcasted);
                hasDirection = true;
            }
        }
        if(hasDirection && rc.senseNearbyRobots(-1, rc.getTeam()).length > rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length){
            if(rc.canMove(dir))
                rc.move(dir);
        }
        else if(hasDirection){
            if(rc.canMove(dir.opposite()))
                rc.move(dir.opposite());
        }
        //pickup enemy flag if we can
        if (rc.canPickupFlag(rc.getLocation()) && rc.getRoundNum() > GameConstants.SETUP_ROUNDS){
            rc.pickupFlag(rc.getLocation());
        }
        //attack
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(4, rc.getTeam().opponent());
        RobotInfo[] allyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        if (enemyRobots.length > 0)
        {
            MapLocation toAttack = lowestHealth(enemyRobots);
            if(rc.canAttack(toAttack))
                rc.attack(toAttack);
        }
        if(enemyRobots.length == 0 && allyRobots.length > 0)
        {
            for (RobotInfo allyRobot : allyRobots) {
                if (rc.canHeal(allyRobot.getLocation())) {
                    rc.heal(allyRobot.getLocation());
                    break;
                }
            }
        }
    }
}
