package com.flansmod.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simulation tests for the per-wheel integration math extracted from
 * EntityVehicle.tick (damping 0.9, gravity 0.98F/20F, tank vs car thrust,
 * speed-proportional steering, water buoyancy).
 */
class VehicleMotionSimulationTest
{
	private static final double EPSILON = 1e-6;

	private static DriveableMotion.Result wheel(
			double motX, double motY, double motZ,
			float throttle, float wheelsYaw, int wheelID,
			float wheelYRotDegrees, double wheelSpeedXZ,
			boolean tank, boolean canThrust)
	{
		return DriveableMotion.applyWheelMotion(
				motX, motY, motZ,
				throttle, wheelsYaw, wheelID,
				wheelYRotDegrees, wheelSpeedXZ,
				tank, 1F, 0F, 1F, 1F,
				1F, 1F,
				canThrust, false,
				false, 0F);
	}

	@Test
	void carThrottleRampBuildsSpeedThroughInertia()
	{
		double motX = 0D;
		double motZ = 0D;
		double previousSpeed = 0D;
		for(int tick = 0; tick < 100; tick++)
		{
			DriveableMotion.Result result = wheel(motX, 0D, motZ, 1F, 0F, 2, 0F, 0D, false, true);
			motX = result.motX;
			motZ = result.motZ;
			double speed = Math.sqrt(motX * motX + motZ * motZ);
			assertTrue(speed >= previousSpeed - 1e-9,
					"speed must not decrease while thrusting: " + previousSpeed + " -> " + speed);
			previousSpeed = speed;
			assertTrue(Double.isFinite(speed));
		}

		// Steady state of 0.1 thrust with 0.9 damping is 0.1 / (1 - 0.9) = 1.0,
		// far above a single tick of thrust (inertia accumulates)
		assertEquals(1.0, previousSpeed, 0.01);
		assertTrue(previousSpeed > 0.2,
				"velocity must accumulate across ticks (write-back regression), got " + previousSpeed);
	}

	@Test
	void releasingThrottleDecaysByTheZeroNineDamping()
	{
		DriveableMotion.Result result = wheel(1.0, 0D, 0.5, 1F, 0F, 2, 0F, 0D, false, false);
		assertEquals(0.9, result.motX, EPSILON, "motion must be damped by 0.9 per tick");
		assertEquals(0.45, result.motZ, EPSILON, "motion must be damped by 0.9 per tick");
	}

	@Test
	void tankSteeringDrivesLeftAndRightWheelsAsymmetrically()
	{
		// Turning left (wheelsYaw > 0): the left wheels get extra thrust,
		// the right wheels get less
		DriveableMotion.Result left = wheel(0D, 0D, 0D, 1F, 10F, 0, 0F, 0D, true, true);
		DriveableMotion.Result right = wheel(0D, 0D, 0D, 1F, 10F, 1, 0F, 0D, true, true);
		assertTrue(left.motX > right.motX,
				"left wheel must push harder than the right wheel in a left turn: "
						+ left.motX + " vs " + right.motX);
		assertEquals(0.08, left.motX, EPSILON,
				"left wheel: 0.9 * 0.8 * 0 + (1 + 10 * 0.1) * 0.04 = 0.08");
		assertEquals(0.0, right.motX, EPSILON,
				"right wheel: (1 - 10 * 0.1) * 0.04 = 0");
	}

	@Test
	void carSteeringTermScalesWithWheelSpeed()
	{
		// Front wheel at 90 degrees: steering correction
		// -= wheelSpeedXZ * sin(90) * 0.01 * wheelsYaw
		DriveableMotion.Result slow = wheel(0D, 0D, 0D, 1F, 5F, 2, 90F, 10D, false, true);
		DriveableMotion.Result fast = wheel(0D, 0D, 0D, 1F, 5F, 2, 90F, 20D, false, true);
		assertEquals(-0.5, slow.motX, EPSILON);
		assertEquals(-1.0, fast.motX, EPSILON);
		assertTrue(Math.abs(fast.motX) > Math.abs(slow.motX),
				"steering correction must scale with wheel speed");
	}

	@Test
	void nonFrontWheelsGetExtraDamping()
	{
		DriveableMotion.Result front = wheel(1D, 0D, 1D, 1F, 0F, 2, 0F, 0D, false, true);
		DriveableMotion.Result rear = wheel(1D, 0D, 1D, 1F, 0F, 0, 0F, 0D, false, true);
		// Front: 0.9 * 1 + 0.1 = 1.0 ; Rear: (0.9 * 1 + 0.1) * 0.9 = 0.9
		assertEquals(1.0, front.motX, EPSILON);
		assertEquals(0.9, rear.motX, EPSILON);
	}

	@Test
	void waterBuoyancyAddsLift()
	{
		DriveableMotion.Result result = DriveableMotion.applyWheelMotion(
				0D, 0D, 0D, 0F, 0F, 0, 0F, 0D,
				false, 1F, 0F, 1F, 1F, 1F, 1F,
				false, false, true, 0.0165F);
		// 0.9 * 0 - 0.98/20 + 0.0165 = -0.0325
		assertEquals(-0.0325, result.motY, 1e-5);
	}

	@Test
	void gravityAppliesEveryTickWithoutNaN()
	{
		double motY = 0D;
		for(int tick = 0; tick < 50; tick++)
		{
			DriveableMotion.Result result = wheel(0D, motY, 0D, 0F, 0F, 0, 0F, 0D, false, false);
			motY = result.motY;
			assertTrue(Double.isFinite(motY));
		}
		// Gravity accumulates: strictly negative after damping equilibrium
		assertTrue(motY < -0.4);
	}

	@Test
	void fuelDrainsOnlyWhenThrustingAndNotCreative()
	{
		DriveableMotion.Result thrust = wheel(0D, 0D, 0D, 1F, 0F, 2, 0F, 0D, false, true);
		assertEquals(1F, thrust.fuelDrain, EPSILON);

		DriveableMotion.Result idle = wheel(0D, 0D, 0D, 1F, 0F, 2, 0F, 0D, false, false);
		assertEquals(0F, idle.fuelDrain, EPSILON);

		DriveableMotion.Result creative = DriveableMotion.applyWheelMotion(
				0D, 0D, 0D, 1F, 0F, 2, 0F, 0D,
				false, 1F, 0F, 1F, 1F, 1F, 1F,
				true, true, false, 0F);
		assertEquals(0F, creative.fuelDrain, EPSILON);
	}
}
