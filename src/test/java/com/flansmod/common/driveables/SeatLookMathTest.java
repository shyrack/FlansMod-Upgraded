package com.flansmod.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the seat look math extracted from EntitySeat:
 * yaw/pitch wraparound, limiters, aiming-speed chase and the onMouseMoved
 * look-around limiter.
 */
class SeatLookMathTest
{
	private static final float EPSILON = 1e-4F;

	@Test
	void wraparoundFrom350To10YieldsPlus20()
	{
		assertEquals(20F, SeatLookMath.wrappedAngleDelta(10F, 350F), EPSILON);
		assertEquals(-20F, SeatLookMath.wrappedAngleDelta(350F, 10F), EPSILON);
		assertEquals(180F, SeatLookMath.wrappedAngleDelta(180F, 0F), EPSILON);
		// The original loops use <=, so exactly -180 wraps to +180
		assertEquals(180F, SeatLookMath.wrappedAngleDelta(-180F, 0F), EPSILON);
		assertEquals(0F, SeatLookMath.wrappedAngleDelta(0F, 0F), EPSILON);
	}

	@Test
	void pitchLimiterClampsToMinAndMax()
	{
		assertEquals(89F, SeatLookMath.clampPitchToLimits(100F, -89F, 89F), EPSILON);
		assertEquals(-89F, SeatLookMath.clampPitchToLimits(-100F, -89F, 89F), EPSILON);
		assertEquals(10F, SeatLookMath.clampPitchToLimits(10F, -89F, 89F), EPSILON);
	}

	@Test
	void yawLimiterKeepsEquivalentAnglesAndClampsTheRest()
	{
		// 400 is equivalent to 40, which is inside [-360, 360]: unchanged,
		// the wrapped representation is preserved for continuous rotation
		assertEquals(400F, SeatLookMath.clampYawToLimits(400F, -360F, 360F), EPSILON);
		assertEquals(500F, SeatLookMath.clampYawToLimits(500F, -360F, 360F), EPSILON);
		// 800: neither representation (800/440) is in range; the equivalent
		// (440) is moved to the nearest boundary and re-wrapped
		assertEquals(720F, SeatLookMath.clampYawToLimits(800F, -360F, 360F), EPSILON);
		assertEquals(-720F, SeatLookMath.clampYawToLimits(-800F, -360F, 360F), EPSILON);
		// Within a normal [-90, 90] seat range, out-of-range yaw clamps directly
		assertEquals(90F, SeatLookMath.clampYawToLimits(100F, -90F, 90F), EPSILON);
		assertEquals(-90F, SeatLookMath.clampYawToLimits(-100F, -90F, 90F), EPSILON);
		assertEquals(10F, SeatLookMath.clampYawToLimits(10F, -90F, 90F), EPSILON);
	}

	@Test
	void aimingChaseConvergesWithPerTickDeltaCappedAtAimingSpeed()
	{
		float lookingYaw = 0F;
		float lookingPitch = 0F;
		int steps = 0;
		while(steps < 30)
		{
			SeatLookMath.LookUpdate update = SeatLookMath.updateLook(
					100F, 0F, lookingYaw, lookingPitch,
					-360F, 360F, -89F, 89F,
					10F, 10F,
					false, false, true);
			assertTrue(Math.abs(update.newYaw - lookingYaw) <= 10F + EPSILON,
					"per-tick yaw delta must not exceed aimingSpeed.x");
			lookingYaw = update.newYaw;
			steps++;
			if(lookingYaw == 100F)
				break;
		}
		assertEquals(100F, lookingYaw, EPSILON);
		assertTrue(steps <= 11, "chase must converge within aimingSpeed steps, took " + steps);
	}

	@Test
	void legacyAimingSnapsDirectlyToTarget()
	{
		SeatLookMath.LookUpdate update = SeatLookMath.updateLook(
				100F, 50F, 0F, 0F,
				-360F, 360F, -89F, 89F,
				10F, 10F,
				true, false, true);
		assertEquals(100F, update.newYaw, EPSILON);
		assertEquals(50F, update.newPitch, EPSILON);
		assertEquals(0, update.signDeltaX);
		assertEquals(0, update.signDeltaY);
	}

	@Test
	void pitchIsHeldUntilYawIsCloseEnoughWithLatePitch()
	{
		// pitchToMove = 20, aimingSpeedY = 10 -> minYawToMove = |20/10| * 10 = 20
		// yawToMove = 30 > 20: pitch must hold
		SeatLookMath.LookUpdate held = SeatLookMath.updateLook(
				30F, 20F, 0F, 0F,
				-360F, 360F, -89F, 89F,
				10F, 10F,
				false, false, true);
		assertEquals(0F, held.newPitch, EPSILON);
		assertEquals(20F, held.minYawToMove, EPSILON);

		// yawToMove = 10 < 20: pitch now steps by aimingSpeed.y
		SeatLookMath.LookUpdate moving = SeatLookMath.updateLook(
				10F, 20F, 0F, 0F,
				-360F, 360F, -89F, 89F,
				10F, 10F,
				false, false, true);
		assertEquals(10F, moving.newPitch, EPSILON);
	}

	@Test
	void yawBeforePitchStepsPitchWhileYawIsAlreadyAligned()
	{
		// signDeltaX == 0 (yaw within half a step) and yawBeforePitch:
		// pitch steps immediately
		SeatLookMath.LookUpdate update = SeatLookMath.updateLook(
				4F, 20F, 0F, 0F,
				-360F, 360F, -89F, 89F,
				10F, 10F,
				false, true, true);
		assertEquals(4F, update.newYaw, EPSILON);
		assertEquals(10F, update.newPitch, EPSILON);
	}

	@Test
	void lookAroundClampRespectsSeatMinAndMax()
	{
		// The onMouseMoved look-around uses the same yaw/pitch limiters
		assertEquals(89F, SeatLookMath.clampPitchToLimits(200F, -89F, 89F), EPSILON);
		assertEquals(-89F, SeatLookMath.clampPitchToLimits(-200F, -89F, 89F), EPSILON);
		assertEquals(180F, SeatLookMath.clampYawToLimits(180F, -180F, 180F), EPSILON);
		assertEquals(-180F, SeatLookMath.clampYawToLimits(-180F, -180F, 180F), EPSILON);
		assertEquals(360F, SeatLookMath.clampYawToLimits(360F, -360F, 360F), EPSILON);
	}

	@Test
	void noLegacyAimingSnapsWithinHalfAStep()
	{
		// |yawToMove| <= aimingSpeed.x / 2 -> snap
		SeatLookMath.LookUpdate update = SeatLookMath.updateLook(
				4F, 0F, 0F, 0F,
				-360F, 360F, -89F, 89F,
				10F, 10F,
				false, false, true);
		assertEquals(4F, update.newYaw, EPSILON);
		assertEquals(0, update.signDeltaX);
	}
}
