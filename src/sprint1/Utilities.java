package sprint1;

import battlecode.common.*;
import java.util.Random;

public class Utilities {

  //variable that determines whether robot defaults to going left or right around an obstacle
  private static boolean left=true;
  //circumNavStart is how we determine if we've gone a full circle around an obstacle
  private static MapLocation circumNavStart=null;

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

  //bugNav is a basic pathfinding algorithm
  //use it to figure out the best direction to go
  public static Direction bugNav(RobotController rc, MapLocation destination) throws GameActionException{
    MapLocation current = rc.getLocation();
    Direction dirTo = current.directionTo(destination);

    MapLocation wallEncountered = lineVision(rc,destination);
    if(wallEncountered==null){
      //go in straight line to the destination
      rc.setIndicatorLine(current,destination,255,0,0);
      //this line resets the default for going left or right around an obstacle
      left=(rc.getID()%2==0);
      //reset the circumnav variable (since we're not going around an obstacle)
      circumNavStart=null;
      return dirTo;
    }

    //if we get to this point in the code, path isn't clear

    //alternate to finding the "endpoint of the wall"
    //boundary following behavior


    return dirTo;
  }

  //returns null if the path to the destination is clear
  //returns the first wall encountered if path isn't clear
  private static MapLocation lineVision(RobotController rc, MapLocation destination) throws GameActionException{
    //line is the straight line from the robot to its destination
    MapLocation[] line = new MapLocation[5];
    line[0]=rc.getLocation();
    if(line[0].equals(destination)) return null;
    line[1]=line[0].add(rc.getLocation().directionTo(destination));
    if(line[1].equals(destination)) return null;
    line[2]=line[1].add(rc.getLocation().directionTo(destination));
    line[3]=line[2].add(rc.getLocation().directionTo(destination));
    line[4]=line[3].add(rc.getLocation().directionTo(destination));

    //manually check all 4 squares
    //note: might have to reconfigure the rc.canSenseLocations
    if(rc.canSenseLocation(line[1])&&!rc.sensePassability(line[1])) return line[1];
    if(line[2].equals(destination)) return null;
    if(rc.canSenseLocation(line[2])&&!rc.sensePassability(line[2])) return line[2];
    if(line[3].equals(destination)) return null;
    if(rc.canSenseLocation(line[3])&&!rc.sensePassability(line[3])) return line[3];
    if(line[4].equals(destination)) return null;
    if(rc.canSenseLocation(line[4])&&!rc.sensePassability(line[4])) return line[4];
    return null;
  }

  //returns the closest wall endpoint to the destination
  //"wall endpoint" is simply where a wall ends (ie: a corner, endpoint, or simply where the duck's vision of a wall ends)
  private static MapLocation wallEndPoint(RobotController rc, MapLocation destination, MapLocation firstWall) throws GameActionException{
    MapLocation current = rc.getLocation();
    Direction dirTo = current.directionTo(destination);

    //starting with firstWall, check adjacent squares to see if the wall extends

    return null;

  }
}
