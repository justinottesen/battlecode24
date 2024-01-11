package defense;

import battlecode.common.*;
import java.util.Random;

public class Utilities {
  //default direction refers to which direction (right or left) the robot defaults to
  //when doing stupid bugnav
  private static MapLocation currentDestination = null;
  private static boolean defaultDirection = true;
  private static int prevH = 0;
  private static int dMin = 9999;
  private static boolean stupidBugMode = false;
  private static MapLocation preventLooping = null;
  private static MapLocation lastWallHugged = null;

  public static void tryMove(Direction d, RobotController rc) throws GameActionException{
    if (rc.canMove(d)) rc.move(d);
    if (rc.canMove(d.rotateLeft())) rc.move(d.rotateLeft());
    if (rc.canMove(d.rotateRight())) rc.move(d.rotateRight());
    if (rc.canMove(d.rotateLeft().rotateLeft())) rc.move(d.rotateLeft().rotateLeft());
    if (rc.canMove(d.rotateRight().rotateRight())) rc.move(d.rotateRight().rotateRight());
  }
  public static Direction tryDirection(Direction d, RobotController rc) throws GameActionException{
    Direction tryDirection=d;
    if (rc.canMove(d)) return d;

    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;
    tryDirection=rotateDefault(tryDirection);
    if (rc.canMove(tryDirection)) return tryDirection;

    return Direction.CENTER;
  }
  private static Direction rotateDefault(Direction d){
    if(defaultDirection) return d.rotateLeft();
    return d.rotateRight();
  }
  // https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
  public static void shuffleArray(Object[] array) {
    int index;
    Object temp;
    Random random = new Random();
    for (int i = array.length - 1; i > 0; i--)
    {
        index = random.nextInt(i + 1);
        if (index != i)
        {
          index = random.nextInt(i + 1);
          temp = array[index];
          array[index] = array[i];
          array[i] = temp;
        }
    }
  }

  public static boolean inBounds(MapLocation m, RobotController rc){
    //todo: check if map height and width correspond with the max value (ie: no signpost error)
    return m.x>=0&&m.y>=0&&m.x<=rc.getMapWidth()&&m.y<=rc.getMapHeight();
  }
  public static MapLocation randMapLocation(Random rng, RobotController rc){
    return new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapHeight()));
  }

  //bugNav is a basic pathfinding algorithm
  //use it to figure out the best direction to go
  //there's a small bug where if the duck gets knocked off the wall by another duck and onto another wall, it will follow the wrong wall forever
  public static Direction bugNav(RobotController rc, MapLocation destination) throws GameActionException{
    //reset bugnav when given a new destination
    if(currentDestination==null || !(destination.equals(currentDestination))){
      rc.setIndicatorString("current destination: "+currentDestination+" new destination: "+destination);
      resetBugNav(destination);
    } 
    MapLocation current = rc.getLocation();
    Direction dirTo = current.directionTo(destination);

    MapLocation wallEncountered = lineVision(rc,destination);

    //set dMin (and possible stupidBugMode)
    if(wallEncountered.distanceSquaredTo(destination)<dMin){
      //rc.setIndicatorString("prev dMin: "+dMin+" new dMin: "+wallEncountered.distanceSquaredTo(destination));
      rc.setIndicatorDot(wallEncountered,0,0,255);
      dMin = wallEncountered.distanceSquaredTo(destination);
      stupidBugMode=false;
      lastWallHugged = null;
      //reset the default direction
      defaultDirection=rc.getID()%2==0;
    }
    
    if(!stupidBugMode){
      //not stupidBugMode
      if(rc.sensePassability(wallEncountered)){
        //go in straight line to the destination
        rc.setIndicatorLine(current,destination,255,0,0);
        return dirTo;
      }
      
      //if we get to this point in the code, path isn't clear
      MapLocation wallEndPoint=wallEndPoint(rc,destination,wallEncountered);
      
      //wallEncountered (the wall straight ahead) conveniently serves as the closest currently "sensed" point
      if(current.distanceSquaredTo(wallEndPoint)+wallEndPoint.distanceSquaredTo(destination)<=prevH){
        rc.setIndicatorLine(current,wallEndPoint,0,0,255);
        prevH = current.distanceSquaredTo(wallEndPoint)+wallEndPoint.distanceSquaredTo(destination);
        return current.directionTo(wallEndPoint);
      }
      //if the if statement fails, we go to stupidBugMode
      preventLooping = wallEncountered;
    }
    //boundary following behavior (stupid bug)
    stupidBugMode=true;

    rc.setIndicatorDot(preventLooping,0,255,0);
    //are we touching a wall?
    Direction bugDirection;
    if(touchingWall(rc)==null){
      //find wall
      if(lastWallHugged == null){
        bugDirection = dirTo;
      }else{
        bugDirection = current.directionTo(lastWallHugged);
      }
    }else{
      lastWallHugged=current.add(touchingWall(rc));
      bugDirection=tryDirection(touchingWall(rc),rc);
    }
    rc.setIndicatorString("stupid bug wants to go "+bugDirection);

    //check that we haven't done a complete loop
    if(current.distanceSquaredTo(preventLooping)<=2){
      //this also runs if the duck hasn't moved since it touched the wall (ie: it got blocked by a bunch of other ducks)
      rc.setIndicatorString("stupid bug wants to go "+bugDirection+" full loop, fuck this shit");
      //we can't use DIRECTION.CENTER bc sometimes tryDirection won't be able to move cuz its surrounded by ducks
      return null;
    }
    return bugDirection;
  }

  //resets the bugnav variables when navigating to a new destination
  private static void resetBugNav(MapLocation destination){
      currentDestination = destination;
      prevH = 0;
      dMin = 9999;
      stupidBugMode = false;
      preventLooping = null;
      lastWallHugged = null;
  }

  //returns null if the path to the destination is clear
  //returns the first wall encountered if path isn't clear
  private static MapLocation lineVision(RobotController rc, MapLocation destination) throws GameActionException{
    //manually check all 4 squares
    MapLocation current = rc.getLocation();
    MapLocation next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)) return current;

    //line1
    current=next;
    next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)||!rc.sensePassability(current)) return current;
    //line2
    current=next;
    next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)||!rc.sensePassability(current)) return current;
    //line3
    current=next;
    next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)||!rc.sensePassability(current)) return current;
    return next;
  }

  //returns the closest wall endpoint to the destination
  //"wall endpoint" is simply where a wall ends (ie: a corner, endpoint, or simply where the duck's vision of a wall ends)
  private static MapLocation wallEndPoint(RobotController rc, MapLocation destination, MapLocation firstWall) throws GameActionException{
    MapLocation current = rc.getLocation();
    Direction dirTo = current.directionTo(destination);
    Direction left = dirTo.rotateLeft().rotateLeft();
    Direction right = dirTo.rotateRight().rotateRight();

    //starting with firstWall, check adjacent squares to see if the wall extends
    MapLocation leftLoc = firstWall;
    MapLocation rightLoc = firstWall;

    while(rc.canSenseLocation(leftLoc)){
      MapLocation diagLeft = leftLoc.add(left.rotateLeft());
      MapLocation straightLeft = leftLoc.add(left);
      MapLocation diagRight = leftLoc.add(left.rotateRight());
      if(rc.canSenseLocation(diagLeft)&&!rc.sensePassability(diagLeft)){
        leftLoc = diagLeft;
        continue;
      }
      if(rc.canSenseLocation(straightLeft)&&!rc.sensePassability(straightLeft)){
        leftLoc = straightLeft;
        continue;
      }
      if(rc.canSenseLocation(diagRight)&&!rc.sensePassability(diagRight)){
        leftLoc = diagRight;
        continue;
      }
      break;
    }
    while(rc.canSenseLocation(rightLoc)){
      MapLocation diagRight = rightLoc.add(right.rotateRight());
      MapLocation straightRight = rightLoc.add(right);
      MapLocation diagLeft = rightLoc.add(right.rotateLeft());
      if(rc.canSenseLocation(diagRight)&&!rc.sensePassability(diagRight)){
        rightLoc = diagRight;
        continue;
      }
      if(rc.canSenseLocation(straightRight)&&!rc.sensePassability(straightRight)){
        rightLoc = straightRight;
        continue;
      }
      if(rc.canSenseLocation(diagLeft)&&!rc.sensePassability(diagLeft)){
        rightLoc = diagRight;
        continue;
      }
      break;
    }

    //now decide which wallEndPoint is better
    //(leftLoc and rightLoc are the wallEndPoints)
    if((current.distanceSquaredTo(leftLoc)+leftLoc.distanceSquaredTo(destination))<(current.distanceSquaredTo(rightLoc)+rightLoc.distanceSquaredTo(destination)) || !inBounds(rightLoc,rc)){
      return leftLoc;
    }
    return rightLoc;

  }

  public static Direction touchingWall(RobotController rc) throws GameActionException{
    MapLocation current = rc.getLocation();
    if (!rc.canSenseLocation(current.add(Direction.EAST)) || !rc.sensePassability(current.add(Direction.EAST)) && !rc.canSenseRobotAtLocation(current.add(Direction.EAST))) return Direction.EAST;
    if (!rc.canSenseLocation(current.add(Direction.SOUTH)) || !rc.sensePassability(current.add(Direction.SOUTH)) && !rc.canSenseRobotAtLocation(current.add(Direction.SOUTH))) return Direction.SOUTH;
    if (!rc.canSenseLocation(current.add(Direction.WEST)) || !rc.sensePassability(current.add(Direction.WEST)) && !rc.canSenseRobotAtLocation(current.add(Direction.WEST))) return Direction.WEST;
    if (!rc.canSenseLocation(current.add(Direction.NORTH)) || !rc.sensePassability(current.add(Direction.NORTH)) && !rc.canSenseRobotAtLocation(current.add(Direction.NORTH))) return Direction.NORTH;
    if (!rc.canSenseLocation(current.add(Direction.SOUTHEAST)) || !rc.sensePassability(current.add(Direction.SOUTHEAST)) && !rc.canSenseRobotAtLocation(current.add(Direction.SOUTHEAST))) return Direction.SOUTHEAST;
    if (!rc.canSenseLocation(current.add(Direction.SOUTHWEST)) || !rc.sensePassability(current.add(Direction.SOUTHWEST)) && !rc.canSenseRobotAtLocation(current.add(Direction.SOUTHWEST))) return Direction.SOUTHWEST;
    if (!rc.canSenseLocation(current.add(Direction.NORTHWEST)) || !rc.sensePassability(current.add(Direction.NORTHWEST)) && !rc.canSenseRobotAtLocation(current.add(Direction.NORTHWEST))) return Direction.NORTHWEST;
    if (!rc.canSenseLocation(current.add(Direction.NORTHEAST)) || !rc.sensePassability(current.add(Direction.NORTHEAST)) && !rc.canSenseRobotAtLocation(current.add(Direction.NORTHEAST))) return Direction.NORTHEAST;
    return null;
  }

  //returns true if m is the centerwall
  //returns false otherwise
  public static boolean isCenterWall(MapLocation m, RobotController rc) throws GameActionException{
    if(!rc.canSenseLocation(m)) return false;
    if(rc.sensePassability(m)) return false;
    MapInfo mapInfo = rc.senseMapInfo(m);
    if(mapInfo.isWater()) return false;
    if(mapInfo.isWall()) return false;
    return true;
  }

  public static void fight(RobotController rc) throws GameActionException{
    MapLocation current = rc.getLocation();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    if(enemies.length==0) return;
    RobotInfo[] enemiesWithinAttack = rc.senseNearbyRobots(4, rc.getTeam().opponent());

    //retreat logic

    if(enemiesWithinAttack.length>0&&!(rc.getActionCooldownTurns()>=10)){
      //retreat because you can't attack
      tryMove(enemiesWithinAttack[0].getLocation().directionTo(current),rc);
    }

    if(rc.getHealth()<225){
      //retreat cuz low health
      tryMove(enemies[0].getLocation().directionTo(current),rc);
    }
    //end retreat logic

    //TODO: put enemy targets in shared array

    //the next stuff has to do with attacking, so return if we can't attack
    if(rc.getActionCooldownTurns()>=10) return;

    if(rc.getMovementCooldownTurns()<10){
      //can move, increase our attack range by one movement
      enemiesWithinAttack = rc.senseNearbyRobots(6, rc.getTeam().opponent());
    }

    //this shouldn't ever happen but just in case
    if(enemiesWithinAttack.length==0) return;

    //find the weakest enemy to attack
    RobotInfo weakestEnemy = enemiesWithinAttack[0];
    for(int i=1;i<enemiesWithinAttack.length;++i){
      if(enemiesWithinAttack[i].getHealth()<weakestEnemy.getHealth()){
        weakestEnemy = enemiesWithinAttack[i];
      }
      //break ties by picking enemies that are further away to attack
      if(enemiesWithinAttack[i].getHealth()<=weakestEnemy.getHealth() && enemiesWithinAttack[i].getLocation().distanceSquaredTo(current)>weakestEnemy.getLocation().distanceSquaredTo(current)){
        weakestEnemy = enemiesWithinAttack[i];
      }
    }

    //attack
    if(rc.canAttack(weakestEnemy.getLocation())){
      rc.attack(weakestEnemy.getLocation());
    }else{
      //move and attack
      if(rc.canMove(current.directionTo(weakestEnemy.getLocation()))){
        rc.move(current.directionTo(weakestEnemy.getLocation()));
      }
      if(rc.canAttack(weakestEnemy.getLocation())){
        rc.attack(weakestEnemy.getLocation());
      }
    }
  }
}
