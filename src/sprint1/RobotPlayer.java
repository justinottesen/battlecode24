package sprint1;

import battlecode.common.*;

import java.util.Random;
import sprint1.Utilities;

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
              Utilities.resetBugNav(null);
              break; 
            }
          }
          if(!rc.isSpawned())
            Clock.yield();
          
        }

        //buy Action if available
        if(rc.canBuyGlobal(GlobalUpgrade.ACTION)){
          rc.buyGlobal(GlobalUpgrade.ACTION);
        }

        Utilities.fight(rc);
        Utilities.heal(rc);
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
        //Utilities.tryMove(dir, rc);
        Utilities.tryMoveWithFill(dir,rc);

      } catch (GameActionException e) {
        // Oh no! It looks like we did something illegal in the Battlecode world. You should
        // handle GameActionExceptions judiciously, in case unexpected events occur in the game
        // world. Remember, uncaught exceptions cause your robot to explode!
        System.out.println("GameActionException");
        e.printStackTrace();

      } catch (Exception e) {
        // Oh no! It looks like our code tried to do something bad. This isn't a
        // GameActionException, so it's more likely to be a bug in our code.
        System.out.println("Exception");
        e.printStackTrace();

      } finally {
        // Signify we've done everything we want to do, thereby ending our turn.
        // This will make our code wait until the next turn, and then perform this loop again.
        Clock.yield();
      }
      // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
    }
    // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
  }
}
