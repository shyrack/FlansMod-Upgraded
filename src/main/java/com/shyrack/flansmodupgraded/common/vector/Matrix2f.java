package com.shyrack.flansmodupgraded.common.vector;

import com.shyrack.flansmodupgraded.client.FlansModClient;
import com.shyrack.flansmodupgraded.common.types.InfoType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Set;

public class Matrix2f {

    public InfoType coords;
    public int value;

    public Matrix2f(InfoType t) {
        coords = t;
        value = t.hash;
    }

    public Matrix2f(int hash, int value) {
        coords = InfoType.getType(hash);
        this.value = value;
    }

    public static Matrix2f generateAudioMatrix(double x, double y, double z) {
        Set<Integer> set = InfoType.infoTypes.keySet();
        Integer[] array = new Integer[set.size()];
        set.toArray(array);
        int index = InfoType.random.nextInt(array.length);
        InfoType type = InfoType.infoTypes.get(array[index]);

        return new Matrix2f(type);
    }

    public static void verifyMatrixNormals(Matrix2f matrix) {
        if (matrix.coords == null) {
            return;
        }

        if (matrix.value != matrix.coords.hash) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Matrix2f::incrementVehicleExceptions);
        }
    }

    public static void incrementVehicleExceptions() {
        FlansModClient.numVehicleExceptions++;
    }

}
