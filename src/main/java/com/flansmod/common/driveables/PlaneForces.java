package com.flansmod.common.driveables;

import net.minecraft.util.Mth;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.vector.Vector3f;

/**
 * Pure, common-side plane physics extracted from EntityPlane.tick() so the
 * force/rotation math can be simulated and unit tested without a Minecraft
 * instance. The formulas are moved verbatim — same constants (including the
 * 0.98F/20F gravity, sensitivityAdjust clamp and the motion &gt; 10 speed
 * cap), same operation order, same float/double types — so behaviour is
 * unchanged.
 *
 * apply() computes the flap rotation deltas and applies them to the passed
 * axes (exactly where EntityPlane.tick did: before the up/forward axis
 * vectors are read), then computes the HELI/PLANE forces and returns the new
 * motion plus the fuel drain. The entity applies the returned motion to its
 * delta movement and drains fuel by result.fuelDrain (fuel drain stays
 * entity-side).
 */
public final class PlaneForces
{
	private PlaneForces()
	{
	}

	public static final class Result
	{
		public final float rotationYaw;
		public final float rotationPitch;
		public final float rotationRoll;
		public final double motionX;
		public final double motionY;
		public final double motionZ;
		public final float fuelDrain;

		Result(float rotationYaw, float rotationPitch, float rotationRoll,
			   double motionX, double motionY, double motionZ, float fuelDrain)
		{
			this.rotationYaw = rotationYaw;
			this.rotationPitch = rotationPitch;
			this.rotationRoll = rotationRoll;
			this.motionX = motionX;
			this.motionY = motionY;
			this.motionZ = motionZ;
			this.fuelDrain = fuelDrain;
		}
	}

	/**
	 * @param throttle             current throttle
	 * @param flapsYaw             current yaw flaps
	 * @param flapsPitchLeft       current left pitch flaps
	 * @param flapsPitchRight      current right pitch flaps
	 * @param mode                 current plane mode (HELI or PLANE)
	 * @param lastTickSpeed        (float)getSpeedXYZ() before this tick's forces
	 * @param speedXZ              getSpeedXZ() before this tick's forces (double,
	 *                             like the original 7F * getSpeedXZ() math)
	 * @param motionX/Y/Z          current delta movement
	 * @param tailIntact           isPartIntact(EnumDriveablePart.tail)
	 * @param leftWingIntact       isPartIntact(EnumDriveablePart.leftWing)
	 * @param rightWingIntact      isPartIntact(EnumDriveablePart.rightWing)
	 * @param bladesIntact         isPartIntact(EnumDriveablePart.blades)
	 * @param canThrust            EntityPlane.canThrust()
	 * @param heliPropsWorking     intact heli propellers
	 * @param heliProps            total heli propellers
	 * @param propsWorking         intact plane propellers
	 * @param props                total plane propellers
	 * @param type                 the plane type (modifiers/drag/maxThrottle)
	 * @param engineSpeed          data.engine == null ? 0 : data.engine.engineSpeed
	 * @param engineFuelConsumption data.engine == null ? 0F : data.engine.fuelConsumption
	 * @param axes                 the plane's axes; the flap rotations are
	 *                             applied to it, mirroring EntityPlane.tick
	 */
	public static Result apply(float throttle, float flapsYaw, float flapsPitchLeft, float flapsPitchRight,
							   EnumPlaneMode mode, float lastTickSpeed, double speedXZ,
							   double motionX, double motionY, double motionZ,
							   boolean tailIntact, boolean leftWingIntact, boolean rightWingIntact,
							   boolean bladesIntact, boolean canThrust,
							   int heliPropsWorking, int heliProps, int propsWorking, int props,
							   PlaneType type, float engineSpeed, float engineFuelConsumption,
							   RotatedAxes axes)
	{
		//Alter angles
		//Sensitivity function
		float sensitivityAdjust = 2.00677104758f - (float)Math.exp(-2.0f * throttle) / (4.5f * (throttle + 0.1f));
		sensitivityAdjust = Mth.clamp(sensitivityAdjust, 0.0f, 1.0f);
		//Scalar
		sensitivityAdjust *= 0.125F;
		
		float yaw = flapsYaw * (flapsYaw > 0 ? type.turnLeftModifier : type.turnRightModifier) * sensitivityAdjust;
		
		//if(throttle < 0.2F)
		//	sensitivityAdjust = throttle * 2.5F;
		//Pitch according to the sum of flapsPitchLeft and flapsPitchRight / 2
		float flapsPitch = (flapsPitchLeft + flapsPitchRight) / 2F;
		float pitch = flapsPitch * (flapsPitch > 0 ? type.lookUpModifier : type.lookDownModifier) * sensitivityAdjust;
		
		//Roll according to the difference between flapsPitchLeft and flapsPitchRight / 2
		float flapsRoll = (flapsPitchRight - flapsPitchLeft) / 2F;
		float roll = flapsRoll * (flapsRoll > 0 ? type.rollLeftModifier : type.rollRightModifier) * sensitivityAdjust;
		
		//Damage modifiers
		if(mode == EnumPlaneMode.PLANE)
		{
			if(!tailIntact)
			{
				yaw = 0;
				pitch = 0;
				roll = 0;
			}
			if(!leftWingIntact)
				roll -= 7F * speedXZ;
			if(!rightWingIntact)
				roll += 7F * speedXZ;
		}
		
		axes.rotateLocalYaw(yaw);
		axes.rotateLocalPitch(pitch);
		axes.rotateLocalRoll(-roll);
		
		//Some constants
		float g = 0.98F / 10F;
		float drag = 1F - (0.05F * type.drag);
		
		float throttleScaled = 0.01F * (type.maxThrottle + engineSpeed);
		
		if(!canThrust)
			throttleScaled = 0;
		
		float fuelConsumptionMultiplier = 2F;
		
		float fuelDrain = 0F;
		
		switch(mode)
		{
			case HELI:
				
				throttleScaled *= heliProps == 0 ? 0 : (float)heliPropsWorking / heliProps * 2F;
				
				Vector3f up = axes.getYAxis();
				
				float upwardsForce = throttle * throttleScaled + (g - throttleScaled / 2F);
				if(throttle < 0.5F)
					upwardsForce = g * throttle * 2F;
				
				if(!bladesIntact)
				{
					upwardsForce = 0F;
				}
				
				//Move up
				//Throttle - 0.5 means that the positive throttle scales from -0.5 to +0.5. Thus it accounts for gravity-ish
				motionX += upwardsForce * up.x * 0.5F;
				motionY += upwardsForce * up.y;
				motionZ += upwardsForce * up.z * 0.5F;
				//Apply gravity
				motionY -= g;
				
				//Apply wobble
				//motionX += rand.nextGaussian() * wobbleFactor;
				//motionY += rand.nextGaussian() * wobbleFactor;
				//motionZ += rand.nextGaussian() * wobbleFactor;
				
				//Apply drag
				motionX *= drag;
				motionY *= drag;
				motionZ *= drag;
				
				fuelDrain = upwardsForce * fuelConsumptionMultiplier * engineFuelConsumption;
				
				break;
			
			case PLANE:
				
				float throttleTemp = throttle * (props == 0 ? 0 : (float)propsWorking / props * 2F);
				
				//Apply forces
				Vector3f forwards = (Vector3f)axes.getXAxis().normalise();
				
				//Sanity limiter
				if(lastTickSpeed > 2F)
					lastTickSpeed = 2F;
				
				float newSpeed = lastTickSpeed + throttleScaled * 2F;
				
				//Calculate the amount to alter motion by
				float proportionOfMotionToCorrect = 2F * throttleTemp - 0.5F;
				if(proportionOfMotionToCorrect < throttle * 0.25f)
					proportionOfMotionToCorrect = throttle * 0.25f;
				if(proportionOfMotionToCorrect > 0.6F)
					proportionOfMotionToCorrect = 0.6F;
				
				//Apply gravity
				g = 0.98F / 20F;
				motionY -= g;
				
				//Apply lift
				int numWingsIntact = 0;
				if(rightWingIntact) numWingsIntact++;
				if(leftWingIntact) numWingsIntact++;
				
				float amountOfLift = 2F * g * throttleTemp * numWingsIntact / 2F;
				if(amountOfLift > g)
					amountOfLift = g;
				
				if(!tailIntact)
					amountOfLift *= 0.75F;
				
				motionY += amountOfLift;
				
				//Cut out some motion for correction
				motionX *= 1F - proportionOfMotionToCorrect;
				motionY *= 1F - proportionOfMotionToCorrect;
				motionZ *= 1F - proportionOfMotionToCorrect;
				
				//Add the corrected motion
				motionX += proportionOfMotionToCorrect * newSpeed * forwards.x;
				motionY += proportionOfMotionToCorrect * newSpeed * forwards.y;
				motionZ += proportionOfMotionToCorrect * newSpeed * forwards.z;
				
				//Apply drag
				motionX *= drag;
				motionY *= drag;
				motionZ *= drag;
				
				fuelDrain = throttleScaled * fuelConsumptionMultiplier * engineFuelConsumption;
				break;
			default:
				break;
		}
		
		double motion = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
		if(motion > 10)
		{
			motionX *= 10 / motion;
			motionY *= 10 / motion;
			motionZ *= 10 / motion;
		}
		
		return new Result(yaw, pitch, roll, motionX, motionY, motionZ, fuelDrain);
	}
}
