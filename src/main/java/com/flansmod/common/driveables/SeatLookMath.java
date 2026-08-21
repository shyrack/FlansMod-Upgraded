package com.flansmod.common.driveables;

/**
 * Pure, common-side seat look math extracted from EntitySeat so the
 * yaw/pitch wraparound, seat limiters and aiming-speed chase can be unit
 * tested without a Minecraft instance.
 *
 * The formulas are moved verbatim from EntitySeat.updateSeatRotation() and
 * EntitySeat.onMouseMoved() with identical constants, ordering and
 * float/double types, so behaviour is unchanged.
 */
public final class SeatLookMath
{
	private SeatLookMath()
	{
	}

	/**
	 * Result of one tick of the aiming-speed chase in
	 * EntitySeat.updateSeatRotation(). The sound flags are derived from the
	 * sign deltas and the yaw-to-move comparison exactly as before.
	 */
	public static final class LookUpdate
	{
		public final float newYaw;
		public final float newPitch;
		public final int signDeltaX;
		public final int signDeltaY;
		public final float currentYawToMove;
		public final float minYawToMove;

		LookUpdate(float newYaw, float newPitch, int signDeltaX, int signDeltaY,
				   float currentYawToMove, float minYawToMove)
		{
			this.newYaw = newYaw;
			this.newPitch = newPitch;
			this.signDeltaX = signDeltaX;
			this.signDeltaY = signDeltaY;
			this.currentYawToMove = currentYawToMove;
			this.minYawToMove = minYawToMove;
		}
	}

	/**
	 * target - current, wrapped to (-180, 180]. 350 to 10 yields +20, not -340.
	 */
	public static float wrappedAngleDelta(float target, float current)
	{
		float delta = target - current;
		while(delta > 180F)
		{
			delta -= 360F;
		}
		while(delta <= -180F)
		{
			delta += 360F;
		}
		return delta;
	}

	/**
	 * Sign of the aiming-speed step: 1, -1 or 0 (within half an aiming speed
	 * step of the target, or legacy aiming).
	 */
	public static int signDelta(float toMove, float halfAimingSpeed, boolean legacyAiming)
	{
		if(toMove > halfAimingSpeed && !legacyAiming)
		{
			return 1;
		}
		else if(toMove < -halfAimingSpeed && !legacyAiming)
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Clamps yaw into [minYaw, maxYaw] while preserving continuous wrapped
	 * representation: since the yaw limiters go from -360 to 360, the pair of
	 * yaw values (newYaw, newYaw +/- 360) are both checked and the
	 * representation closer to the range is moved in. Used by both
	 * updateSeatRotation() and the onMouseMoved() look-around.
	 */
	public static float clampYawToLimits(float newYaw, float minYaw, float maxYaw)
	{
		float otherNewYaw = newYaw - 360F;
		if(newYaw < 0)
			otherNewYaw = newYaw + 360F;
		if((newYaw >= minYaw && newYaw <= maxYaw) ||
				(otherNewYaw >= minYaw && otherNewYaw <= maxYaw))
		{
			//All is well
			return newYaw;
		}

		float newYawDistFromRange =
				Math.min(Math.abs(newYaw - minYaw), Math.abs(newYaw - maxYaw));
		float otherNewYawDistFromRange = Math.min(Math.abs(otherNewYaw - minYaw),
				Math.abs(otherNewYaw - maxYaw));
		// If the newYaw is closer to the range than the otherNewYaw, move newYaw into the range
		if(newYawDistFromRange <= otherNewYawDistFromRange)
		{
			if(newYaw > maxYaw)
				newYaw = maxYaw;
			if(newYaw < minYaw)
				newYaw = minYaw;
		}
		// Else, the otherNewYaw is closer, so move it in
		else
		{
			if(otherNewYaw > maxYaw)
				otherNewYaw = maxYaw;
			if(otherNewYaw < minYaw)
				otherNewYaw = minYaw;
			//Then match up the newYaw with the otherNewYaw
			if(newYaw < 0)
				newYaw = otherNewYaw - 360F;
			else newYaw = otherNewYaw + 360F;
		}
		return newYaw;
	}

	/**
	 * Seat pitch limiter from onMouseMoved()/updateSeatRotation().
	 */
	public static float clampPitchToLimits(float newPitch, float minPitch, float maxPitch)
	{
		if(newPitch > -minPitch)
			newPitch = -minPitch;
		if(newPitch < -maxPitch)
			newPitch = -maxPitch;
		return newPitch;
	}

	/**
	 * One tick of the aiming chase from EntitySeat.updateSeatRotation().
	 * Preserves legacyAiming, latePitch, yawBeforePitch and aimingSpeed
	 * semantics exactly.
	 */
	public static LookUpdate updateLook(float targetYaw, float targetPitch,
										float lookingYaw, float lookingPitch,
										float minYaw, float maxYaw, float minPitch, float maxPitch,
										float aimingSpeedX, float aimingSpeedY,
										boolean legacyAiming, boolean yawBeforePitch, boolean latePitch)
	{
		// Move the seat accordingly
		// Consider new Yaw and Yaw limiters

		float yawToMove = wrappedAngleDelta(targetYaw, lookingYaw);

		int signDeltaX = signDelta(yawToMove, aimingSpeedX / 2, legacyAiming);

		// Calculate new yaw and consider yaw limiters
		float newYaw;

		if(legacyAiming || (signDeltaX == 0))
		{
			newYaw = targetYaw;
		}
		else
		{
			newYaw = lookingYaw + signDeltaX * aimingSpeedX;
		}
		newYaw = clampYawToLimits(newYaw, minYaw, maxYaw);

		// Calculate the new pitch and consider pitch limiters
		float pitchToMove = wrappedAngleDelta(targetPitch, lookingPitch);

		int signDeltaY = signDelta(pitchToMove, aimingSpeedY / 2, legacyAiming);

		// Pitches the gun at the last possible moment in order to reach target pitch at the same time as target yaw.
		float minYawToMove;

		if(latePitch)
		{
			minYawToMove = ((float)Math
					.sqrt((pitchToMove / aimingSpeedY) * (pitchToMove / aimingSpeedY))) *
					aimingSpeedX;
		}
		else
		{
			minYawToMove = 360f;
		}

		float currentYawToMove = (float)Math.sqrt((yawToMove) * (yawToMove));

		float newPitch;

		if(legacyAiming || (signDeltaY == 0))
		{
			newPitch = targetPitch;
		}
		else if(!yawBeforePitch && currentYawToMove < minYawToMove)
		{
			newPitch = lookingPitch + signDeltaY * aimingSpeedY;
		}
		else if(yawBeforePitch && signDeltaX == 0)
		{
			newPitch = lookingPitch + signDeltaY * aimingSpeedY;
		}
		else if(yawBeforePitch)
		{
			newPitch = lookingPitch;
		}
		else
		{
			newPitch = lookingPitch;
		}

		newPitch = clampPitchToLimits(newPitch, minPitch, maxPitch);

		return new LookUpdate(newYaw, newPitch, signDeltaX, signDeltaY, currentYawToMove, minYawToMove);
	}
}
