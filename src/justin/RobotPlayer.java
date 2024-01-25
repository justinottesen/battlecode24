package justin;

import java.util.Random;

import battlecode.common.*;

public strictfp class RobotPlayer {

  private static final Random rng = new Random(69);
  private static int turnCount = 1;

  // Initialized in Turn 1
  private static MapLocation[] spawnLocs = null;
  private static MapLocation[] spawnCenters = null;
  private static MapLocation[] spawnEdges = null;
  private static int mapWidth = 0;
  private static int mapHeight = 0;
  private static MapLocation mapCenter = null;

  // Initialized in Turn 2

  private static boolean attackUpgrade = false;
  private static boolean healingUpgrade = false;
  private static boolean capturingUpgrade = false;

  public static void run(RobotController rc) throws GameActionException {
    // First few turns "hardcoded" in
    
    turn1(rc); // ~23.5k bytecode
    turn2(rc); //

    while (true) {

      try {
        // Spawn in if necessary
        if (!rc.isSpawned()) { spawn(rc); }

        if (turnCount % 600 == 0) {
          System.out.println(turnCount);
          if (!attackUpgrade && rc.canBuyGlobal(GlobalUpgrade.ATTACK)) {
            System.out.println("UPGRADING ATTACK");
            rc.buyGlobal(GlobalUpgrade.ATTACK);
            attackUpgrade = true;
          } else if (!healingUpgrade && rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
            System.out.println("UPGRADING HEALING");
            rc.buyGlobal(GlobalUpgrade.HEALING);
            healingUpgrade = true;
          } else if (!capturingUpgrade && rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) {
            System.out.println("UPGRADING CAPTURING");
            rc.buyGlobal(GlobalUpgrade.CAPTURING);
            capturingUpgrade = true;
          }
        }

        // Move towards food
        crumbSearch(rc);

        // Sense nearby robots
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getLocation(), -1, rc.getTeam());
        Utilities.sortByDistanceInPlace(rc.getLocation(), friends);

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), -1, rc.getTeam().opponent());
        Utilities.sortByDistanceInPlace(rc.getLocation(), enemies);

        if (friends.length == 0 && enemies.length == 0) {
          // Move towards middle of map
          fillWater(rc);
          Direction dir = rc.getLocation().directionTo(mapCenter);
          fuzzyMove(rc, dir, 1);
        } else if (enemies.length == 0) {
          MapLocation closest = friends[0].getLocation();
          if (turnCount <= GameConstants.SETUP_ROUNDS - Math.max(mapHeight, mapWidth)/2) {
            // Move away from friends
            Direction dir = closest.directionTo(rc.getLocation());
            fuzzyMove(rc, dir, 1);
          } else {
            // Heal anyone around
            for (RobotInfo friend : friends) {
              if (!rc.isActionReady()) { break; }
              if (rc.canHeal(friend.getLocation())) {
                rc.heal(friend.getLocation());
              }
            }
            // Move towards middle
            Direction dir = rc.getLocation().directionTo(mapCenter);
            fuzzyMove(rc, dir, 1);
          }
        } else if (friends.length < enemies.length) {
          // Attack enemy if possible
          MapLocation closest = enemies[0].getLocation();
          if (rc.canAttack(closest)) {
            rc.attack(closest);
          }
          // Try to heal friends
          for (RobotInfo friend : friends) {
            if (!rc.isActionReady()) { break; }
            if (rc.canHeal(friend.getLocation())) {
              rc.heal(friend.getLocation());
            }
          }
          Direction dir = closest.directionTo(rc.getLocation());
          fuzzyMove(rc, dir, 1);
        } else if (enemies.length <= friends.length) {
          // Attack enemy if possible & chase
          MapLocation closestEnemy = enemies[0].getLocation();
          MapLocation closestFriend = friends[0].getLocation();
          if (rc.getLocation().distanceSquaredTo(closestFriend) < rc.getLocation().distanceSquaredTo(closestEnemy)) {
            fillWater(rc);
          }
          if (rc.canAttack(closestEnemy)) {
            rc.attack(closestEnemy);
          }
          Direction dir = rc.getLocation().directionTo(closestEnemy);
          fuzzyMove(rc, dir, 1);
        } else {
          System.out.println("I DON'T KNOW WHAT TO DO!!!");
          System.out.println(friends.length);
          System.out.println(enemies.length);
        }
        
      } catch (GameActionException e) {
        System.out.println("GameActionException");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println("Exception");
        e.printStackTrace();
      } finally {
        turnCount++;
        Clock.yield();
      }

    }

  }

  private static void turn1(RobotController rc) {
    try {
      // Get spawn information
      spawnLocs = rc.getAllySpawnLocations();
      spawnCenters = getSpawnCenters();
      spawnEdges = getSpawnEdges();

      // Get map information
      mapWidth = rc.getMapWidth();
      mapHeight = rc.getMapHeight();
      mapCenter = new MapLocation(mapHeight >> 1, mapWidth >> 1);

      spawn(rc);
      getOffStart(rc);

    } catch (GameActionException e) {
      System.out.println("GameActionException");
      e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Exception");
      e.printStackTrace();
    } finally {
      turnCount++;
      Clock.yield();
    }
  }
  private static void turn2(RobotController rc) {
    try {
      // Calculate Symmetric Locations
      MapLocation[] vCenters = new MapLocation[spawnCenters.length];
      MapLocation[] hCenters = new MapLocation[spawnCenters.length];
      MapLocation[] rCenters = new MapLocation[spawnCenters.length];
      for (int i = 0; i < spawnCenters.length; ++i) {
        vCenters[i] = new MapLocation(mapWidth - spawnCenters[i].x - 1, spawnCenters[i].y);
        hCenters[i] = new MapLocation(spawnCenters[i].x, mapHeight - spawnCenters[i].y - 1);
        rCenters[i] = new MapLocation(mapWidth - spawnCenters[i].x - 1, mapHeight - spawnCenters[i].y - 1);
      }

      // Check which are possible
      boolean vPossible = !Utilities.containsAny(vCenters, spawnLocs);
      boolean hPossible = !Utilities.containsAny(hCenters, spawnLocs);
      boolean rPossible = !Utilities.containsAny(rCenters, spawnLocs);

      if (vPossible) {
        System.out.println("Vertical Possible");
        for (MapLocation loc : vCenters) {
          rc.setIndicatorDot(loc, 255, 0, 0);
        }
      }
      if (hPossible) {
        System.out.println("Horizontal Possible");
        for (MapLocation loc : hCenters) {
          rc.setIndicatorDot(loc, 0, 255, 0);
        }
      }
      if (rPossible) {
        System.out.println("Rotational Possible");
        for (MapLocation loc : rCenters) {
          rc.setIndicatorDot(loc, 0, 0, 255);
        }
      }

    // } catch (GameActionException e) {
    //   System.out.println("GameActionException");
    //   e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Exception");
      e.printStackTrace();
    } finally {
      turnCount++;
      Clock.yield();
    }
  }

  private static void spawn(RobotController rc) throws GameActionException {
    while (!tryRespawn(rc)) {
      turnCount++;
      Clock.yield();
    }
  }

  private static void getOffStart(RobotController rc) throws GameActionException {
    MapLocation myCenter = Utilities.getClosest(rc.getLocation(), spawnCenters);
    if (Utilities.contains(rc.getLocation(), spawnLocs)) {
      if (rc.getLocation().equals(myCenter)) {
        moveRandom(rc);
      } else {
        fuzzyMove(rc, myCenter.directionTo(rc.getLocation()), 1);
      }
    }
  }

  private static boolean tryRespawn(RobotController rc) throws GameActionException {
    // Try to spawn on edges first
    Utilities.shuffleInPlace(spawnEdges);
    for (MapLocation loc : spawnEdges) {
      if (rc.canSpawn(loc)) {
        rc.spawn(loc);
        return true;
      }
    }

    // Centers if you have to
    Utilities.shuffleInPlace(spawnCenters);
    for (MapLocation loc : spawnCenters) {
      if (rc.canSpawn(loc)) {
        rc.spawn(loc);
        return true;
      }
    }
    return false;
  }
  
  private static MapLocation[] getSpawnCenters() {
    MapLocation[] centers = new MapLocation[GameConstants.NUMBER_FLAGS];
    int i = 0;
    for (MapLocation loc : spawnLocs) {
      if (Utilities.containsAll(Utilities.getAdjacents(loc, Utilities.cardDirs), spawnLocs)) {
        centers[i++] = loc;
      }
    }
    return centers;
  }
  private static MapLocation[] getSpawnEdges() {
    MapLocation[] edges = new MapLocation[spawnLocs.length - spawnCenters.length];
    int i = 0;
    for (MapLocation loc : spawnLocs) {
      if (!Utilities.contains(loc, spawnCenters)) {
        edges[i++] = loc;
      }
    }
    return edges;
  }

  // Attempts to move in a direction with some leeway
  private static boolean fuzzyMove(RobotController rc, Direction dir, int fuzz) throws GameActionException {
    if (rc.canMove(dir)) {
      rc.move(dir);
      return true;
    }
    Direction left = dir;
    Direction right = dir;
    for (int i = 1; i <= fuzz; ++i) {
      left = left.rotateLeft();
      right = right.rotateRight();
      if (rc.canMove(left)) {
        rc.move(left);
        return true;
      }
      if (rc.canMove(right)) {
        rc.move(right);
        return true;
      }
    }
    return false;
  }
  private static boolean moveRandom(RobotController rc) throws GameActionException {
    Direction dir = Utilities.allDirs[rng.nextInt(8)];
    return fuzzyMove(rc, dir, 4);
  }

  private static void crumbSearch(RobotController rc) throws GameActionException {
    MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
    if (crumbs.length > 0) {
      // Fill water on the way to crumbs
      fillWater(rc);
      MapLocation closest = Utilities.getClosest(rc.getLocation(), crumbs);
      Direction dir = rc.getLocation().directionTo(closest);
      fuzzyMove(rc, dir, 1);
    }
  }
  
  private static void fillWater(RobotController rc) throws GameActionException {
    if (!rc.isActionReady()) { return; }
    MapInfo[] actionable = rc.senseNearbyMapInfos(GameConstants.INTERACT_RADIUS_SQUARED);
    for (MapInfo loc : actionable) {
      if (rc.canFill(loc.getMapLocation())) {
        rc.fill(loc.getMapLocation());
      }
    }
  }
}
