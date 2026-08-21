package com.flansmod.common.driveables;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.TypeFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simulation tests for the plane force math extracted from EntityPlane.tick.
 *
 * These tests drive PlaneForces.apply directly (plus the same axes rotations
 * the entity performs) and never touch a Minecraft instance, so they can
 * characterise the physics without bootstrapping the game.
 */
class PlaneForcesSimulationTest
{
	private static final double EPSILON = 1e-4;

	/** A minimal plane type: sensible defaults for a generic prop plane. */
	private static PlaneType planeType()
	{
		PlaneType type = new PlaneType(new TypeFile("TestPack", EnumType.plane, "test_plane", false));
		type.mode = EnumPlaneMode.PLANE;
		type.maxThrottle = 1F;
		type.drag = 1F;
		return type;
	}

	/** Minimal simulation state, mirroring the entity-tick usage. */
	private static final class PlaneSim
	{
		final RotatedAxes axes = new RotatedAxes();
		double motX, motY, motZ;
		double posX, posY, posZ;
		float throttle;
		float flapsYaw, flapsPitchLeft, flapsPitchRight;
		float fuel = 1000F;
		final List<Double> speeds = new ArrayList<>();
		final List<Double> xzSpeeds = new ArrayList<>();

		void step(PlaneType type, EnumPlaneMode mode, boolean canThrust)
		{
			double lastTickSpeed = Math.sqrt(motX * motX + motY * motY + motZ * motZ);
			double speedXZ = Math.sqrt(motX * motX + motZ * motZ);
			PlaneForces.Result result = PlaneForces.apply(
					throttle, flapsYaw, flapsPitchLeft, flapsPitchRight,
					mode, (float)lastTickSpeed, speedXZ,
					motX, motY, motZ,
					true, true, true, true, // tail, leftWing, rightWing, blades intact
					canThrust,
					2, 2, 2, 2,             // 2/2 heli props, 2/2 plane props working
					type, 1F, 1F,            // engineSpeed, engineFuelConsumption
					axes);
			motX = result.motionX;
			motY = result.motionY;
			motZ = result.motionZ;
			posX += motX;
			posY += motY;
			posZ += motZ;
			fuel -= result.fuelDrain;
			speeds.add(Math.sqrt(motX * motX + motY * motY + motZ * motZ));
			xzSpeeds.add(Math.sqrt(motX * motX + motZ * motZ));
		}
	}

	@Test
	void planeTakeoffBuildsSpeedMonotonicallyAndStaysFinite()
	{
		PlaneSim sim = new PlaneSim();
		PlaneType type = planeType();

		// Throttle ramps 0 -> 1 over 100 ticks, then cruises at full throttle
		for(int tick = 0; tick < 200; tick++)
		{
			sim.throttle = Math.min(1F, tick / 100F);
			sim.step(type, EnumPlaneMode.PLANE, true);
		}

		for(double speed : sim.speeds)
		{
			assertTrue(Double.isFinite(speed), "speed must stay finite, full history: " + sim.speeds);
			assertTrue(speed <= 10.0 + 1e-6, "speed cap of 10 must be respected, got " + speed);
		}

		// Horizontal speed must never decrease while thrusting
		for(int tick = 1; tick < sim.xzSpeeds.size(); tick++)
		{
			assertTrue(sim.xzSpeeds.get(tick) >= sim.xzSpeeds.get(tick - 1) - 1e-9,
					"horizontal speed must not decrease while thrusting: tick " + tick + " "
							+ sim.xzSpeeds.get(tick - 1) + " -> " + sim.xzSpeeds.get(tick)
							+ "\n" + sim.xzSpeeds);
		}

		// Speed must have grown far beyond one tick of thrust. One tick of
		// full-throttle thrust raises the corrected speed target by 0.04
		// (throttleScaled * 2); the accumulated speed of ~0.456 is ~20 times
		// that, proving velocity persists across ticks (the write-back
		// regression fix).
		assertTrue(sim.xzSpeeds.get(sim.xzSpeeds.size() - 1) > 0.2,
				"velocity must accumulate across ticks (write-back regression): final XZ speed "
						+ sim.xzSpeeds.get(sim.xzSpeeds.size() - 1));

		// Acceleration magnitude must stay bounded
		for(int tick = 1; tick < sim.speeds.size(); tick++)
		{
			double acc = (sim.speeds.get(tick) - sim.speeds.get(tick - 1)) * 20;
			assertTrue(Math.abs(acc) < 100, "acceleration magnitude out of bounds at tick " + tick
					+ ": " + acc);
		}
	}

	@Test
	void planeCoastsDownWhenThrottleIsCut()
	{
		PlaneSim sim = new PlaneSim();
		PlaneType type = planeType();

		// Reach cruise speed
		sim.throttle = 1F;
		for(int tick = 0; tick < 150; tick++)
			sim.step(type, EnumPlaneMode.PLANE, true);

		// Cut the throttle; inertia plus drag must decay speed monotonically
		sim.throttle = 0F;
		for(int tick = 0; tick < 50; tick++)
			sim.step(type, EnumPlaneMode.PLANE, true);

		int coastStart = 150;
		for(int tick = coastStart + 1; tick < sim.xzSpeeds.size(); tick++)
		{
			assertTrue(sim.xzSpeeds.get(tick) < sim.xzSpeeds.get(tick - 1) + 1e-12,
					"horizontal speed must decay monotonically during coast: tick " + tick);
		}
		assertTrue(sim.xzSpeeds.get(sim.xzSpeeds.size() - 1) < sim.xzSpeeds.get(coastStart) / 2,
				"drag must halve the cruise speed within 50 ticks of coasting");
	}

	@Test
	void heliHoversWithNetZeroUpwardAcceleration()
	{
		PlaneSim sim = new PlaneSim();
		PlaneType type = planeType();
		type.mode = EnumPlaneMode.HELI;

		sim.throttle = 0.5F;
		for(int tick = 0; tick < 20; tick++)
		{
			sim.step(type, EnumPlaneMode.HELI, true);
			assertTrue(Double.isFinite(sim.motY), "motionY must stay finite");
			assertTrue(Math.abs(sim.motY) < 1e-2,
					"heli at throttle 0.5 with 2/2 props must hover: |motionY| " + Math.abs(sim.motY));
		}
	}

	@Test
	void flapSteeringRotatesWithSensitivityScaledByThrottle()
	{
		PlaneType type = planeType();
		RotatedAxes axes = new RotatedAxes();

		// Yaw: flapsYaw 10 at full throttle -> 10 * 1 * 0.125 = 1.25 degrees
		PlaneForces.Result fullThrottle = PlaneForces.apply(
				1F, 10F, 0F, 0F, EnumPlaneMode.PLANE, 0F, 0D, 0D, 0D, 0D,
				true, true, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(1.25F, fullThrottle.rotationYaw, EPSILON);

		// At zero throttle the sensitivity collapses and there is no steering
		PlaneForces.Result noThrottle = PlaneForces.apply(
				0F, 10F, 0F, 0F, EnumPlaneMode.PLANE, 0F, 0D, 0D, 0D, 0D,
				true, true, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(0F, noThrottle.rotationYaw, EPSILON);

		// Yaw delta magnitude grows with throttle (which drives speed)
		assertTrue(Math.abs(fullThrottle.rotationYaw) > Math.abs(noThrottle.rotationYaw),
				"yaw delta must grow with throttle/speed");

		// Pitch: symmetric flaps pitch up
		PlaneForces.Result pitch = PlaneForces.apply(
				1F, 0F, 10F, 10F, EnumPlaneMode.PLANE, 0F, 0D, 0D, 0D, 0D,
				true, true, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(1.25F, pitch.rotationPitch, EPSILON);
		assertEquals(0F, pitch.rotationRoll, EPSILON);

		// Roll: asymmetric flaps roll
		PlaneForces.Result roll = PlaneForces.apply(
				1F, 0F, 10F, -10F, EnumPlaneMode.PLANE, 0F, 0D, 0D, 0D, 0D,
				true, true, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(-1.25F, roll.rotationRoll, EPSILON);
	}

	@Test
	void brokenWingsInduceRollInPlaneMode()
	{
		PlaneType type = planeType();
		RotatedAxes axes = new RotatedAxes();

		// Left wing missing: roll -= 7F * speedXZ
		PlaneForces.Result result = PlaneForces.apply(
				1F, 0F, 0F, 0F, EnumPlaneMode.PLANE, 0F, 2D, 0D, 0D, 0D,
				true, false, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(-14F, result.rotationRoll, EPSILON);

		// Broken tail: all rotation deltas forced to zero
		PlaneForces.Result noTail = PlaneForces.apply(
				1F, 10F, 10F, 10F, EnumPlaneMode.PLANE, 0F, 2D, 0D, 0D, 0D,
				false, true, true, true, true, 2, 2, 2, 2, type, 1F, 1F, axes);
		assertEquals(0F, noTail.rotationYaw, EPSILON);
		assertEquals(0F, noTail.rotationPitch, EPSILON);
		assertEquals(0F, noTail.rotationRoll, EPSILON);
	}
}
