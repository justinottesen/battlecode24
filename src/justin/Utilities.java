package justin;

import java.util.Arrays;
import java.util.Random;

import battlecode.common.*;

public class Utilities {

  static final Random rng = new Random(420);

  static final Direction[] allDirs = {
    Direction.NORTH,
    Direction.NORTHEAST,
    Direction.EAST,
    Direction.SOUTHEAST,
    Direction.SOUTH,
    Direction.SOUTHWEST,
    Direction.WEST,
    Direction.NORTHWEST
  };

  static final Direction[] cardDirs = {
    Direction.NORTH,
    Direction.EAST,
    Direction.SOUTH,
    Direction.WEST
  };

  static void sortByDistanceInPlace(MapLocation loc, RobotInfo[] list) {
    Arrays.sort(list, (r1, r2) -> loc.distanceSquaredTo(r1.getLocation()) - loc.distanceSquaredTo(r2.getLocation()));
  }

  static MapLocation getClosest(MapLocation loc, MapLocation[] list) {
    MapLocation closest = null;
    int dist = -1;
    int d;
    for (MapLocation l : list) {
      if ((d = loc.distanceSquaredTo(l)) < dist || closest == null) {
        closest = l;
        dist = d;
      }
    }
    return closest;
  }
  static MapLocation getClosest(MapLocation loc, RobotInfo[] list) {
    MapLocation closest = null;
    int dist = -1;
    int d;
    for (RobotInfo r : list) {
      if ((d = loc.distanceSquaredTo(r.getLocation())) < dist || closest == null) {
        closest = r.getLocation();
        dist = d;
      }
    }
    return closest;
  }

  // CHECKS IF list CONTAINS x (or ALL of xs)
  static boolean contains(Object x, Object[] list) {
    for (Object l : list) {
      if (x.equals(l)) { return true; }
    }
    return false;
  }
  static boolean containsAll(Object[] xs, Object[] list) {
    for (Object x : xs) {
      if (!contains(x, list)) { return false; }
    }
    return true;
  }
  static boolean containsAny(Object[] xs, Object[] list) {
    for (Object x : xs) {
      if (contains(x, list)) { return true; }
    }
    return false;
  }
  // THESE DO NOT CHECK MAP BOUNDS
  static MapLocation[] getAdjacents(MapLocation center) {
    return getAdjacents(center, allDirs);
  }
  static MapLocation[] getAdjacents(MapLocation start, Direction[] dirs) {
    MapLocation[] adjacents = new MapLocation[dirs.length];
    for (int i = 0; i < dirs.length; ++i) {
      adjacents[i] = start.add(dirs[i]);
    }
    return adjacents;
  }

  // Shuffle in place
  public static void shuffleInPlace(Object[] array) {
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

}