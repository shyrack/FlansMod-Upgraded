package com.flansmod.common.driveables;

import java.util.List;

/**
 * One structured telemetry event per driveable physics tick. Emitted on both
 * sides ("C"/"S") for every ticking plane/vehicle/mecha when the
 * LogDriveablePhysics config flag is enabled.
 *
 * acc = (vel - prevVel) * 20, computed by EntityDriveable.logPhysicsTick from
 * the previous tick's velocity. playerOffset is null when seat 0 has no
 * passenger; offsets are relative to the driveable centre (seat0) / the seat
 * (player).
 */
public record PhysicsTickTrace(
		int tick,
		String side,
		String typeName,
		int entityId,
		double posX, double posY, double posZ,
		double velX, double velY, double velZ,
		double accX, double accY, double accZ,
		float rotYaw, float rotPitch, float rotRoll,
		float throttle,
		boolean onGround,
		double seatOffsetX, double seatOffsetY, double seatOffsetZ,
		Double playerOffsetX, Double playerOffsetY, Double playerOffsetZ,
		List<String> anomalies)
{
	public PhysicsTickTrace
	{
		anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
	}

	public double speed()
	{
		return Math.sqrt(velX * velX + velY * velY + velZ * velZ);
	}

	public double accMag()
	{
		return Math.sqrt(accX * accX + accY * accY + accZ * accZ);
	}

	/**
	 * Builds a trace with zero acceleration and no anomalies; used for the
	 * previous-tick snapshot that PhysicsDiagnostics compares against.
	 */
	public static PhysicsTickTrace snapshot(int tick, String side, String typeName, int entityId,
											double posX, double posY, double posZ,
											double velX, double velY, double velZ,
											float rotYaw, float rotPitch, float rotRoll,
											float throttle, boolean onGround,
											double seatOffsetX, double seatOffsetY, double seatOffsetZ,
											Double playerOffsetX, Double playerOffsetY, Double playerOffsetZ)
	{
		return new PhysicsTickTrace(tick, side, typeName, entityId,
				posX, posY, posZ, velX, velY, velZ, 0D, 0D, 0D,
				rotYaw, rotPitch, rotRoll, throttle, onGround,
				seatOffsetX, seatOffsetY, seatOffsetZ,
				playerOffsetX, playerOffsetY, playerOffsetZ, List.of());
	}

	/**
	 * Copy with replaced acceleration components (used after the delta has
	 * been computed against the previous tick).
	 */
	public PhysicsTickTrace withAcceleration(double newAccX, double newAccY, double newAccZ)
	{
		return new PhysicsTickTrace(tick, side, typeName, entityId,
				posX, posY, posZ, velX, velY, velZ, newAccX, newAccY, newAccZ,
				rotYaw, rotPitch, rotRoll, throttle, onGround,
				seatOffsetX, seatOffsetY, seatOffsetZ,
				playerOffsetX, playerOffsetY, playerOffsetZ, anomalies);
	}

	/**
	 * Copy with replaced anomaly list.
	 */
	public PhysicsTickTrace withAnomalies(List<String> newAnomalies)
	{
		return new PhysicsTickTrace(tick, side, typeName, entityId,
				posX, posY, posZ, velX, velY, velZ, accX, accY, accZ,
				rotYaw, rotPitch, rotRoll, throttle, onGround,
				seatOffsetX, seatOffsetY, seatOffsetZ,
				playerOffsetX, playerOffsetY, playerOffsetZ, newAnomalies);
	}

	public boolean hasAnomalies()
	{
		return !anomalies.isEmpty();
	}
}
