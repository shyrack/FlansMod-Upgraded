package com.flansmod.common.vector;

import org.joml.Vector3f;

public class VectorUtils {
    public static void add(Vector3f a, Vector3f b, Vector3f result) {
        result.set(a.x + b.x, a.y + b.y, a.z + b.z);
    }
}
