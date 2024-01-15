package defense;

import battlecode.common.*;
import java.util.Random;

public class Utilities {
  //default direction refers to which direction (right or left) the robot defaults to
  //when doing stupid bugnav
  private static MapLocation currentDestination = null;
  private static boolean defaultDirection = true;
  private static int prevH = 9999; 
  private static int dMin = 9999; //shortest distance of any point found to the destination
  private static boolean stupidBugMode = false;
  private static MapLocation preventLooping = null;
  private static MapLocation lastWall = null;

  public static void tryMove(Direction d, RobotController rc) throws GameActionException{
    if (rc.canMove(d)) rc.move(d);
    if (rc.canMove(d.rotateLeft())) rc.move(d.rotateLeft());
    if (rc.canMove(d.rotateRight())) rc.move(d.rotateRight());
    if (rc.canMove(d.rotateLeft().rotateLeft())) rc.move(d.rotateLeft().rotateLeft());
    if (rc.canMove(d.rotateRight().rotateRight())) rc.move(d.rotateRight().rotateRight());
  }

  public static void tryMoveWithFill(Direction d, RobotController rc) throws GameActionException{
    MapLocation current = rc.getLocation();
    
    if(rc.canFill(current.add(d))) rc.fill(current.add(d));
    if (rc.canMove(d)) rc.move(d);

    if(rc.canFill(current.add(d.rotateRight()))) rc.fill(current.add(d.rotateRight()));
    if (rc.canMove(d.rotateRight())) rc.move(d.rotateRight());

    if(rc.canFill(current.add(d.rotateLeft()))) rc.fill(current.add(d.rotateLeft()));
    if (rc.canMove(d.rotateLeft())) rc.move(d.rotateLeft());

    if(rc.canFill(current.add(d.rotateRight().rotateRight()))) rc.fill(current.add(d.rotateRight().rotateRight()));
    if (rc.canMove(d.rotateRight().rotateRight())) rc.move(d.rotateRight().rotateRight());

    if(rc.canFill(current.add(d.rotateLeft().rotateLeft()))) rc.fill(current.add(d.rotateLeft().rotateLeft()));
    if (rc.canMove(d.rotateLeft().rotateLeft())) rc.move(d.rotateLeft().rotateLeft());
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
    rc.setIndicatorLine(current,wallEncountered,0,0,255);

    //set dMin (and possible stupidBugMode)
    if(wallEncountered.distanceSquaredTo(destination)<dMin){
      //rc.setIndicatorString("prev dMin: "+dMin+" new dMin: "+wallEncountered.distanceSquaredTo(destination));
      dMin = wallEncountered.distanceSquaredTo(destination);
      stupidBugMode=false;
      lastWall=null;
      preventLooping=null;
      //reset the default direction
      defaultDirection=rc.getID()%2==0;
    }

    if(!stupidBugMode){
      //not stupidBugMode
      if(sensePassabilityWithFilling(wallEncountered, rc)){
        //go in straight line to the destination
        rc.setIndicatorLine(current,destination,255,0,0);
        rc.setIndicatorString("straight shot to "+destination);
        return dirTo;
      }
      
      //if we get to this point in the code, path isn't clear
      MapLocation wallEndPoint=wallEndPoint(rc,destination,wallEncountered);
      
      int currentH = current.distanceSquaredTo(wallEndPoint)+wallEndPoint.distanceSquaredTo(destination);
      //H is the distance to the wall endpoint + distance from wall endpoint to the destination
      //as long as H keeps getting better, we move towards H
      //rc.setIndicatorString("prevH: "+prevH+" currentH; "+currentH);
      if(currentH<prevH){
        rc.setIndicatorLine(current,wallEndPoint,0,255,0);
        rc.setIndicatorString("wall in the way, routing to wallEndPoint: "+wallEndPoint);
        prevH = currentH;
        return current.directionTo(wallEndPoint);
      }
      //if the if statement fails, we go to stupidBugMode
      //if(current.distanceSquaredTo(wallEncountered)>2)  System.out.println(current+": Catestrophic error in transitioning to stupid bugnav (not adjacent to correct wall)");
      lastWall=wallEndPoint;
    }
    //boundary following behavior (stupid bug)
    stupidBugMode=true;
    rc.setIndicatorString("stupid bug mode");
    rc.setIndicatorDot(lastWall,0,255,0);
    if(rc.canSenseLocation(lastWall)&&rc.sensePassability(lastWall)){
      rc.setIndicatorString("catastrophic error: lastWall isn't a wall: "+lastWall);
      resetBugNav(destination);
      return Direction.CENTER;
    }
    
    //this could run into trouble if we get pinned against a wall by other ducks
    if(touchingOutOfBounds(rc)!=null) defaultDirection = !defaultDirection;

    //update the wall we are looking at
    lastWall = bugLook(rc,lastWall);

    if(preventLooping==null){
      if(current.distanceSquaredTo(lastWall)<=2)
        preventLooping=current;
    }else if(preventLooping.equals(current)){
      rc.setIndicatorString("fuck this shit, I'm done");
      return null;
    }
    if(preventLooping!=null)  rc.setIndicatorDot(preventLooping,255,0,255);
    
    return tryDirection(current.directionTo(lastWall), rc);
    //rc.setIndicatorString("lastWall: "+lastWall);
    //return Direction.CENTER;
  }

  //resets the bugnav variables when navigating to a new destination
  public static void resetBugNav(MapLocation destination){
      currentDestination = destination;
      prevH = 9999;
      dMin = 9999;
      stupidBugMode = false;
      preventLooping=null;
      lastWall=null;
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
    if(current.equals(destination)||!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    //line2
    current=next;
    next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    //line3
    current=next;
    next = current.add(current.directionTo(destination));
    if(current.equals(destination)||!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    return next;
  }

  //returns null if the path to the destination is clear
  //returns the first wall encountered if path isn't clear
  private static MapLocation lineVision(RobotController rc, Direction d) throws GameActionException{
    //manually check all 4 squares
    MapLocation current = rc.getLocation();
    MapLocation next = current.add(d);
    if(!rc.canSenseLocation(next)) return current;

    //line1
    current=next;
    next = current.add(d);
    if(!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    //line2
    current=next;
    next = current.add(d);
    if(!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    //line3
    current=next;
    next = current.add(d);
    if(!rc.canSenseLocation(next)||!sensePassabilityWithFilling(current, rc)) return current;
    return next;
  }

  //returns the closest wall endpoint to the destination
  //"wall endpoint" is simply where a wall ends (ie: a corner, endpoint, or simply where the duck's vision of a wall ends)
  private static MapLocation wallEndPoint(RobotController rc, MapLocation destination, MapLocation firstWall) throws GameActionException{
    MapLocation current = rc.getLocation();
    Direction dirTo = current.directionTo(destination);
    Direction left = dirTo.rotateLeft().rotateLeft();
    Direction right = dirTo.rotateRight().rotateRight();
    if(!isCardinalDirection(dirTo)){
      left=left.rotateLeft();
      right=right.rotateLeft();
      dirTo=dirTo.rotateLeft();
    }
    MapLocation leftLoc = followWall(firstWall,left,dirTo,rc);
    MapLocation rightLoc = followWall(firstWall,right,dirTo,rc);
    
    rc.setIndicatorDot(leftLoc,255,0,255);
    rc.setIndicatorDot(rightLoc,0,255,255);
    //now decide which wallEndPoint is better
    //(leftLoc and rightLoc are the wallEndPoints)
    if((current.distanceSquaredTo(leftLoc)+leftLoc.distanceSquaredTo(destination))<(current.distanceSquaredTo(rightLoc)+rightLoc.distanceSquaredTo(destination)) || !inBounds(rightLoc,rc)){
      return leftLoc;
    }
    return rightLoc;

  }

  //returns adjacent wall MapLocation (the wall the bug is hugging)
  //if not adjacent to any walls, returns lastWall
  private static MapLocation bugLook(RobotController rc, MapLocation lastWall) throws GameActionException{
    MapLocation current = rc.getLocation();
    Direction bugLookDirection = current.directionTo(lastWall);
    MapLocation bugLookLocation = current.add(bugLookDirection);
    MapInfo bugLookMapInfo = null;

    //1
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection); 

    //2
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection); 

    //3
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection);
    
    //4
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection);
    
    //5
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection);
    
    //6
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
      else return lastWall;
    }
    bugLookDirection = rotateDefault(bugLookDirection);
    bugLookLocation = current.add(bugLookDirection); 

    //7
    if(rc.canSenseLocation(bugLookLocation)){
      bugLookMapInfo = rc.senseMapInfo(bugLookLocation);
      if(bugLookMapInfo.isWall()||bugLookMapInfo.isDam()||bugLookMapInfo.isWater()) lastWall = bugLookLocation;
    }

    return lastWall;
  }
  //followWall finds the "endpoint" of a wall in a given direction (either where the wall ends or where vision of the wall ends)
  //Wall must be a wall/impassible terrain that is bordering a square that is passible
  //rightOrLeft and Forwards must form a 90 degree angle and they must be cardinal directions
  public static MapLocation followWall(MapLocation wall, Direction rightOrLeft, Direction forwards, RobotController rc) throws GameActionException{
    if(!rc.canSenseLocation(wall)||sensePassabilityWithFilling(wall, rc)) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT,"invalid wall: "+wall);
    if(!isCardinalDirection(rightOrLeft)) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "rightOrLeft isn't a cardinal direction: "+rightOrLeft);
    if(!isCardinalDirection(forwards)) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "forwards isn't a cardinal direction: "+rightOrLeft);
    if(rightOrLeft.dx==forwards.dx||rightOrLeft.dy==forwards.dy) throw new GameActionException(GameActionExceptionType.CANT_DO_THAT, "rightOrLeft: "+rightOrLeft+" and forwards: "+forwards+" aren't 90 degrees apart");
    rc.setIndicatorDot(wall,255,255,0);
    Direction prevDirection = rightOrLeft;
    while(rc.canSenseLocation(wall)){
      //the first two directions (left and backwards) take immediate precedent
      //and the moment you can't see left or backwards, we've reached the end of the wall
      //(backwards is prioritized over left)

      //backwards
      if(rc.canSenseLocation(wall.add(forwards.opposite()))){
        if(prevDirection!=forwards &&!sensePassabilityWithFilling(wall.add(forwards.opposite()),rc)){
          rc.setIndicatorLine(wall,wall.add(forwards.opposite()),255,255,0);
          wall = wall.add(forwards.opposite());
          prevDirection = forwards.opposite();
          continue;
        }
      }else{
        break;
      }

      //left
      if(rc.canSenseLocation(wall.add(rightOrLeft))){
        if(prevDirection!= rightOrLeft.opposite() && !sensePassabilityWithFilling(wall.add(rightOrLeft),rc)){
          rc.setIndicatorLine(wall,wall.add(rightOrLeft),255,255,0);
          wall = wall.add(rightOrLeft);
          prevDirection = rightOrLeft;
          continue;
        }
      }else{
        break;
      }
      //the second two directions (forwards and right) have less precedent
      //we need to make sure that they don't undo a previous left or backwards
      //(forwards is prioritized over backwards)

      //forwards
      if(prevDirection!=forwards.opposite() && rc.canSenseLocation(wall.add(forwards))){
        if(!sensePassabilityWithFilling(wall.add(forwards),rc)){
          rc.setIndicatorLine(wall,wall.add(forwards),255,255,0);
          wall = wall.add(forwards);
          prevDirection = forwards;
          continue;
        }
      }else{
        break;
      }

      //right
      /*
      if(prevDirection!=rightOrLeft && rc.canSenseLocation(wall.add(rightOrLeft.opposite()))){
        if(!sensePassabilityWithFilling(wall.add(rightOrLeft.opposite()),rc)){
          rc.setIndicatorLine(wall,wall.add(rightOrLeft.opposite()),255,255,0);
          wall = wall.add(rightOrLeft.opposite());
          prevDirection = rightOrLeft.opposite();
          continue;
        }
      }
      */
      break;
    }
    return wall;
  }

  public static boolean isCardinalDirection(Direction d){
    return d.dx*d.dy==0;
  }
  
  public static boolean sensePassabilityWithFilling(MapLocation m, RobotController rc) throws GameActionException{
    if(!inBounds(m,rc)) return false;
    MapInfo mapInfo = rc.senseMapInfo(m);
    return !(mapInfo.isWall()||mapInfo.isDam());
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

  public static Direction touchingOutOfBounds(RobotController rc) throws GameActionException{
    MapLocation current = rc.getLocation();
    if(!rc.canSenseLocation(current.add(Direction.EAST))) return Direction.EAST;
    if(!rc.canSenseLocation(current.add(Direction.NORTH))) return Direction.NORTH;
    if(!rc.canSenseLocation(current.add(Direction.NORTHEAST))) return Direction.NORTHEAST;
    if(!rc.canSenseLocation(current.add(Direction.NORTHWEST))) return Direction.NORTHWEST;
    if(!rc.canSenseLocation(current.add(Direction.SOUTH))) return Direction.SOUTH;
    if(!rc.canSenseLocation(current.add(Direction.SOUTHEAST))) return Direction.SOUTHEAST;
    if(!rc.canSenseLocation(current.add(Direction.WEST))) return Direction.WEST;
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

  public static void heal(RobotController rc) throws GameActionException{
    MapLocation current = rc.getLocation();
    RobotInfo[] allies = rc.senseNearbyRobots(6, rc.getTeam());
    if(allies.length==0) return;

    //find weakest ally
    RobotInfo weakestAlly = allies[0];
    for(int i=1;i<allies.length;++i){
      if(allies[i].getHealth()<weakestAlly.getHealth()){
        weakestAlly=allies[i];
      }
      if(allies[i].getHealth()<=weakestAlly.getHealth() && current.distanceSquaredTo(allies[i].getLocation())>current.distanceSquaredTo(weakestAlly.getLocation())){
        weakestAlly=allies[i];
      }
    }

    //weakest ally is at full health
    if(weakestAlly.getHealth()==1000) return;

    //attempt to heal weakest ally
    if(rc.canHeal(weakestAlly.getLocation())){
      rc.heal(weakestAlly.getLocation());
    }else{
      tryMove(current.directionTo(weakestAlly.getLocation()),rc);
      if(rc.canHeal(weakestAlly.getLocation())){
        rc.heal(weakestAlly.getLocation());
      }
    }

  }
}
