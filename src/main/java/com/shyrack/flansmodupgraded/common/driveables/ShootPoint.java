package com.shyrack.flansmodupgraded.common.driveables;

import com.shyrack.flansmodupgraded.common.vector.Vector3f;

public class ShootPoint {

    public DriveablePosition rootPos;
    public Vector3f offPos;


    public ShootPoint(DriveablePosition driverPos, Vector3f offsetPos) {
        rootPos = driverPos;
        offPos = offsetPos;
    }

}
