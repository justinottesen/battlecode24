package defense;

import battlecode.common.*;

import java.util.Random;
import defense.Utilities;

public strictfp class RobotPlayer {

  /**
   * We will use this variable to count the number of turns this robot has been alive.
   * You can use static variables like this to save any information you want. Keep in mind that even though
   * these variables are static, in Battlecode they aren't actually shared between your robots.
   */
  static int turnCount = 0;

  /**
   * A random number generator.
   * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
   * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
   * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
   */
  static Random rng;

  /** Array containing all the possible movement directions. */
  static final Direction[] directions = {
    Direction.NORTH,
    Direction.NORTHEAST,
    Direction.EAST,
    Direction.SOUTHEAST,
    Direction.SOUTH,
    Direction.SOUTHWEST,
    Direction.WEST,
    Direction.NORTHWEST,
  };

  static MapLocation destination=null;
  static boolean isExplorer = false;
  static MapLocation waterTrapBuilt = null;
  /**
   * run() is the method that is called when a robot is instantiated in the Battlecode world.
   * It is like the main function for your robot. If this method returns, the robot dies!
   *
   * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
   *            information on its current status. Essentially your portal to interacting with the world.
   **/
  public static void run(RobotController rc) throws GameActionException {
    rng = new Random(rc.getID());
    MapLocation[] spawnLocs = rc.getAllySpawnLocations();

    while (true) {
      // This code runs during the entire lifespan of the robot, which is why it is in an infinite
      // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
      // loop, we call Clock.yield(), signifying that we've done everything we want to do.

      turnCount += 1;  // We have now been alive for one more turn!

      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
      try {
        // Make sure you spawn your robot in before you attempt to take any actions!
        // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
        while (!rc.isSpawned()) {
          Utilities.shuffleArray(spawnLocs);
          for ( MapLocation loc : spawnLocs) {
            if (rc.canSpawn(loc)) { 
              rc.spawn(loc);
              if(rc.readSharedArray(0)<5){
                isExplorer=true;
                rc.writeSharedArray(0,rc.readSharedArray(0)+1);
              } 
              break; 
            }
          }
          //System.out.println("Unable to spawn");
          Clock.yield();
        }

        //buy Action if available
        if(rc.canBuyGlobal(GlobalUpgrade.ACTION)){
          rc.buyGlobal(GlobalUpgrade.ACTION);
        }

        //if we're touching the centerwall (and we can move and build)
        if(rc.getMovementCooldownTurns()<10 && rc.getActionCooldownTurns()<10){
          Direction toWall = Utilities.touchingWall(rc);
          if(toWall!=null && (Utilities.isCenterWall(rc.getLocation().add(toWall),rc)||rc.getRoundNum()>200)&&rc.getCrumbs()>=200){
            MapLocation toBuild = rc.getLocation();
            if (rc.canMove(toWall.opposite())){
              rc.move(toWall.opposite());
              if(rc.canBuild(TrapType.WATER,toBuild)){
                rc.build(TrapType.WATER, toBuild);
                waterTrapBuilt = toBuild;
              }
            }
          }
        }
        if(waterTrapBuilt!=null){
          //if you just built a water trap, build two explosion traps 2 blocks away in the opposite direction
          Direction awayFromDivider = waterTrapBuilt.directionTo(rc.getLocation());
          if(rc.canBuild(TrapType.STUN,rc.getLocation().add(awayFromDivider))){
            rc.build(TrapType.STUN,rc.getLocation().add(awayFromDivider));
          }
          if(rc.canBuild(TrapType.STUN,rc.getLocation().add(awayFromDivider.rotateLeft()))){
            rc.build(TrapType.STUN,rc.getLocation().add(awayFromDivider.rotateLeft()));
          }
          if(rc.canBuild(TrapType.STUN,rc.getLocation().add(awayFromDivider.rotateRight()))){
            rc.build(TrapType.STUN,rc.getLocation().add(awayFromDivider.rotateRight()));
          }
          waterTrapBuilt = null;
        }else{
          Utilities.fight(rc);
          if (isExplorer && rc.getRoundNum() <= GameConstants.SETUP_ROUNDS&&rc.senseNearbyCrumbs(20).length>0){
            //look for crumbs
            MapLocation[] crumbLocations = rc.senseNearbyCrumbs(20);
            destination = crumbLocations[rng.nextInt(crumbLocations.length)];
          }

          //move to a random location on the map
          if(destination==null||destination.equals(rc.getLocation())) destination=Utilities.randMapLocation(rng, rc);
          rc.setIndicatorDot(destination,255,0,0);
          Direction dir = Utilities.bugNav(rc,destination);
          //if the random location is impossible to get to, pick a new one
          if(dir==null){
            destination=Utilities.randMapLocation(rng, rc);
            dir = Utilities.bugNav(rc,destination);
            if(dir == null) dir = Direction.CENTER;
          }
          Utilities.tryMove(dir, rc);
        }
      } catch (GameActionException e) {
        System.out.println("GameActionException");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println("Exception");
        e.printStackTrace();
      } finally {
        Clock.yield();
      }
    }
    // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
  }
}
