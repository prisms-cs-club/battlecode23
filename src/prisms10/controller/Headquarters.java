package prisms10.controller;

import battlecode.common.*;
import prisms10.memory.MemoryAddress;
import prisms10.memory.MemorySection;
import prisms10.util.Map;

public class Headquarters extends Robot {

    // the first few robots the headquarters will build
    static final RobotType[] initialRobots = {
            RobotType.AMPLIFIER, RobotType.CARRIER, RobotType.LAUNCHER, RobotType.LAUNCHER
    };
    // number of rounds producing items randomly before producing an anchor is required
    static final int nextAnchorRound = 30;

    public Headquarters(RobotController rc) {
        super(rc);
        robotType = RobotType.HEADQUARTERS;
    }


    @Override
    public void run() throws GameActionException {
        // initialize
        if ((rc.readSharedArray(63) & 0x8000) == 0) {
            rc.writeSharedArray(63, 0x8000);
            // initialize shared memory
            for (int i = 0; i < MemorySection.IDX_ENEMY_HQ_END; i++) {
                rc.writeSharedArray(i, MemoryAddress.MASK_COORDS);
            }
            // initialize nearby well info
            super.run();
        }
        // record the current headquarters' position into shared memory
        int currentLocation = MemoryAddress.fromLocation(rc.getLocation());
        for (int i = 8; i < 11; i++) {
            int data = rc.readSharedArray(i);
            if (data == currentLocation) {
                // repeated information found in shared memory
                break;
            }
            if (rc.readSharedArray(i) == MemoryAddress.MASK_COORDS) {
                rc.writeSharedArray(i, currentLocation);
                break;
            }
        }
        // produce first few items as scheduled in array `initialRobots`
        if (state < initialRobots.length) {
            // randomly select one location on the rim of the HQ to build a robot
            MapLocation[] rimLocs = Map.getCircleRimLocs(rc.getLocation(), robotType.actionRadiusSquared);
            // randomly select the first robot
            do {
                RobotType curType = initialRobots[state];
                MapLocation curLoc = rimLocs[Math.abs(random.nextInt()) % 16];
                if (rc.canBuildRobot(curType, curLoc)) {
                    rc.buildRobot(curType, curLoc);
                    state++;
                } else {
                    break;
                }
            } while (state < initialRobots.length);

        }
        while (true) {
            // repeat until can't build more
            if (state == initialRobots.length + nextAnchorRound) {
                // produce an anchor on specific state
                if (rc.canBuildAnchor(Anchor.STANDARD)) {
                    rc.buildAnchor(Anchor.STANDARD);
                    state = initialRobots.length;
                } else break;
            } else {
                // Pick a direction to build in.
                Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
                MapLocation newLoc = rc.getLocation().add(dir);
                float randNum = random.nextFloat();
                if (randNum < 0.42) {
                    // probability for carrier: 42%
                    rc.setIndicatorString("Trying to build a carrier");
                    if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                        rc.buildRobot(RobotType.CARRIER, newLoc);
                        state++;
                    } else break;
                } else if (randNum < 0.96) {
                    // probability for launcher: 54%
                    rc.setIndicatorString("Trying to build a launcher");
                    if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                        rc.buildRobot(RobotType.LAUNCHER, newLoc);
                        state++;
                    } else break;
                } else {
                    // probability for amplifier: 4%
                    rc.setIndicatorString("Trying to build an amplifier");
                    if (rc.canBuildRobot(RobotType.AMPLIFIER, newLoc)) {
                        rc.buildRobot(RobotType.AMPLIFIER, newLoc);
                        state++;
                    } else break;
                }
            }
        }

    }

}
