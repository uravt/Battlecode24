package Version5;

import battlecode.common.*;

import static Version5.RobotPlayer.*;
import static Version5.Utilities.bestHeal;

public class Soldier
{
    public static void runSoldier(RobotController rc) throws GameActionException {

        //blank declaration, will be set by something
        Direction dir; // = Pathfinding.basicPathfinding(rc, new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2), false);
        //try to move towards any seen opponent flags
        if(rc.readSharedArray(58) != 0)
        {
            Pathfinding.tryToMove(rc, Utilities.convertIntToLocation(rc.readSharedArray(58)));
            rc.setIndicatorString("Helping teammate @ " + Utilities.convertIntToLocation(rc.readSharedArray(58)));
        }
        FlagInfo[] nearbyOppFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if(nearbyOppFlags.length != 0 && !nearbyOppFlags[0].isPickedUp()){
            Pathfinding.bugNav2(rc, nearbyOppFlags[0].getLocation());
        }
        //pickup enemy flag if we can
        if (rc.canPickupFlag(rc.getLocation()) && rc.getRoundNum() > GameConstants.SETUP_ROUNDS) {
            rc.pickupFlag(rc.getLocation());
        }
        //if we have an enemy flag, bring it to closest area
        MapLocation closestSpawnLoc = findClosestSpawnLocation(rc);
        //method returns null if we dont have a flag
        if (closestSpawnLoc != null) {
//            dir = rc.getLocation().directionTo(closestSpawnLoc);
//            if (rc.canMove(dir)) {
//                rc.setIndicatorString("moving flag");
//                rc.move(dir);
//            }
            rc.setIndicatorString("moving flag");
            if(rc.isMovementReady()) Pathfinding.bugNav2(rc, closestSpawnLoc);
        }

        //attack
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo[] enemyRobotsAttackRange = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo[] allyRobots = rc.senseNearbyRobots(GameConstants.VISION_RADIUS_SQUARED, rc.getTeam());
        RobotInfo[] allyRobotsHealRange = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());
        RobotInfo toHeal = bestHeal(rc, allyRobotsHealRange);
        //immediately try to heal if they have a flag
        if(toHeal != null && toHeal.hasFlag() && rc.canHeal(toHeal.getLocation())){
            rc.heal(toHeal.getLocation());
        }
        //no enemy in view
        if (enemyRobots.length == 0) {
            //Task System/ Broadcast locations
            MapLocation closestBroadcasted = findClosestBroadcastFlags(rc);
            if (closestBroadcasted != null) {
                if(rc.isMovementReady()) Pathfinding.bugNav2(rc, closestBroadcasted);
            }
            if(allyRobotsHealRange.length > 0)
            {
                //Heal Allies if possible
                if(toHeal != null && rc.canHeal(toHeal.getLocation())){
                    rc.heal(toHeal.getLocation());
                }
                if(rc.isMovementReady()) Pathfinding.bugNav2(rc, Utilities.newGetClosestEnemy(rc));
            }
        }
        //enemy in view
        else {
            //More Enemies
            if (enemyRobots.length < allyRobots.length) {
                //Can Attack
                MapLocation toAttack = lowestHealth(enemyRobotsAttackRange);
                if (toAttack != null && rc.canAttack(toAttack))
                {
                    rc.attack(toAttack);
                }
                //Can't Attack
                else {
                    //Move
                    //Task System/ Broadcast locations
                    MapLocation closestBroadcasted = findClosestBroadcastFlags(rc);
                    if (closestBroadcasted != null) {
                        if(rc.isMovementReady()) Pathfinding.bugNav2(rc, closestBroadcasted);
                    }
                    //Can Heal
                    if (allyRobotsHealRange.length > 0) {
                        if(toHeal != null && rc.canHeal(toHeal.getLocation())){
                            rc.heal(toHeal.getLocation());
                        }
                    }
                }
            }
            //Less Enemies
            else
            {
                MapLocation toAttack = lowestHealth(enemyRobotsAttackRange);
                if (toAttack != null && rc.canAttack(toAttack))
                    rc.attack(toAttack);
                if(rc.getLocation() != null && toAttack != null && rc.canMove(rc.getLocation().directionTo(toAttack)))
                    rc.move(rc.getLocation().directionTo(toAttack));
            }
        }


        }


        /*
        if (enemyRobots.length > allyRobots.length)
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
         */
}
