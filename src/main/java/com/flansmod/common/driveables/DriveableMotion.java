package com.flansmod.common.driveables;

/**
 * Pure, common-side per-wheel integration math extracted from
 * EntityVehicle.tick() so the wheel physics can be simulated and unit tested
 * without a Minecraft instance. Formulas moved verbatim — same constants
 * (0.9 damping, 0.98F/20F gravity, 0.04F/0.1F tank thrust/steering scales,
 * 0.1F/0.01F car thrust/steering scales), same operation order, same
 * float/double types — so behaviour is unchanged.
 *
 * The entity computes the world-dependent inputs (canThrust,
 * driverIsCreative, wheel rotation, in-water check) and applies the returned
 * motion to the wheel's delta movement; the fuel drain is applied
 * entity-side via result.fuelDrain.
 */
public final class DriveableMotion
{
	private DriveableMotion()
	{
	}

	public static final class Result
	{
		public final double motX;
		public final double motY;
		public final double motZ;
		public final float fuelDrain;

		Result(double motX, double motY, double motZ, float fuelDrain)
		{
			this.motX = motX;
			this.motY = motY;
			this.motZ = motZ;
			this.fuelDrain = fuelDrain;
		}
	}

	/**
	 * @param motX/Y/Z              the wheel's current delta movement
	 * @param throttle              vehicle throttle
	 * @param wheelsYaw             steering input
	 * @param wheelID               wheel.getExpectedWheelID()
	 * @param wheelYRotDegrees      wheel.getYRot() (float, like the original float math)
	 * @param wheelSpeedXZ          wheel.getSpeedXZ() (pre-tick wheel speed)
	 * @param tank                  vehicleType.tank
	 * @param maxThrottle           vehicleType.maxThrottle
	 * @param maxNegativeThrottle   vehicleType.maxNegativeThrottle
	 * @param turnLeftModifier      vehicleType.turnLeftModifier
	 * @param turnRightModifier     vehicleType.turnRightModifier
	 * @param engineSpeed           data.engine == null ? 0 : data.engine.engineSpeed
	 * @param engineFuelConsumption data.engine == null ? 0F : data.engine.fuelConsumption
	 * @param canThrust             EntityVehicle.canThrust(data, driver)
	 * @param driverIsCreative      EntityDriveable.driverIsCreative()
	 * @param inWater               type.floatOnWater && world.containsAnyLiquid(wheel.getBoundingBox())
	 * @param buoyancy              vehicleType.buoyancy
	 */
	public static Result applyWheelMotion(double motX, double motY, double motZ,
										  float throttle, float wheelsYaw, int wheelID,
										  float wheelYRotDegrees, double wheelSpeedXZ,
										  boolean tank, float maxThrottle, float maxNegativeThrottle,
										  float turnLeftModifier, float turnRightModifier,
										  float engineSpeed, float engineFuelConsumption,
										  boolean canThrust, boolean driverIsCreative,
										  boolean inWater, float buoyancy)
	{
		motX *= 0.9F;
		motY *= 0.9F;
		motZ *= 0.9F;
		
		//Apply gravity
		motY -= 0.98F / 20F;
		
		//Apply velocity
		float fuelDrain = 0F;
		if(canThrust)
		{
			if(!driverIsCreative)
			{
				fuelDrain = engineFuelConsumption * throttle;
			}
			
			if(tank)
			{
				boolean left = wheelID == 0 || wheelID == 3;
				
				float turningDrag = 0.02F;
				motX *= 1F - (Math.abs(wheelsYaw) * turningDrag);
				motZ *= 1F - (Math.abs(wheelsYaw) * turningDrag);
				
				float velocityScale = 0.04F * (throttle > 0 ? maxThrottle : maxNegativeThrottle) *
					engineSpeed;
				float steeringScale = 0.1F * (wheelsYaw > 0 ? turnLeftModifier : turnRightModifier);
				float effectiveWheelSpeed =
					(throttle + (wheelsYaw * (left ? 1 : -1) * steeringScale)) * velocityScale;
				motX += effectiveWheelSpeed * Math.cos(wheelYRotDegrees * 3.14159265F / 180F);
				motZ += effectiveWheelSpeed * Math.sin(wheelYRotDegrees * 3.14159265F / 180F);
				
				
			}
			else
			{
				//if(getVehicleType().fourWheelDrive || wheel.ID == 0 || wheel.ID == 1)
				{
					float velocityScale =
						0.1F * throttle * (throttle > 0 ? maxThrottle : maxNegativeThrottle) *
							engineSpeed;
					motX += Math.cos(wheelYRotDegrees * 3.14159265F / 180F) * velocityScale;
					motZ += Math.sin(wheelYRotDegrees * 3.14159265F / 180F) * velocityScale;
				}
				
				//Apply steering
				if(wheelID == 2 || wheelID == 3)
				{
					float velocityScale = 0.01F * (wheelsYaw > 0 ? turnLeftModifier : turnRightModifier) *
						(throttle > 0 ? 1 : -1);
					
					motX -=
						wheelSpeedXZ * Math.sin(wheelYRotDegrees * 3.14159265F / 180F) * velocityScale *
							wheelsYaw;
					motZ +=
						wheelSpeedXZ * Math.cos(wheelYRotDegrees * 3.14159265F / 180F) * velocityScale *
							wheelsYaw;
				}
				else
				{
					motX *= 0.9F;
					motZ *= 0.9F;
				}
			}
		}
		
		if(inWater)
		{
			motY += buoyancy;
		}
		
		return new Result(motX, motY, motZ, fuelDrain);
	}
}
