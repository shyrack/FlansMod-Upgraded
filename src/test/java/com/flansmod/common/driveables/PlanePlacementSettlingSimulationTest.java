package com.flansmod.common.driveables;

import org.junit.jupiter.api.Test;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.TypeFile;
import com.flansmod.common.vector.Matrix4f;
import com.flansmod.common.vector.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless simulation of placing a Ye Olde pack Biplane on flat ground,
 * replicating EntityPlane.tick's wheel-spring loop (real RotatedAxes /
 * Vector3f / Matrix4f math) and the checkForCollisions sweep, with the
 * ground modelled as a hard clamp (wheels and the plane's AABB cannot sink
 * below the block top).
 *
 * This is the reproduction harness for the "plane jumps when placed" bug:
 *
 *  - EntityPlane.tick feeds gravity into the delta movement every tick and
 *    writes it back via setDeltaMovement(motion...), but the wheel springs
 *    support the plane by move()ing it without touching the velocity. A
 *    parked plane therefore converges to ~-1.9 m/tick "falling" velocity
 *    while resting on the ground.
 *  - checkForCollisions damages parts by speed * hardness^2, so the resting
 *    plane takes crash damage at that fake 1.9 m/tick speed. The
 *    tail-dragger stance (RestingPitch 5, tail wheel far behind) produces a
 *    permanent dAngle torque in the wheel-spring loop
 *    (axes.rotateGlobal(-dAngle * strength, ...)) that rocks the plane and
 *    sweeps the wing collision points through the ground.
 *  - When a swept part dies, checkForCollisions explodes the plane
 *    (world.explode at the part) - the visible jump.
 */
class PlanePlacementSettlingSimulationTest
{
	private static final double GROUND_TOP = 0.0;
	private static final double ENTITY_HALF_HEIGHT = 0.9; // default 0.6x1.8 AABB
	private static final float WHEEL_SPRING_STRENGTH = 0.125F;
	private static final float WHEEL_PROPORTION = 0.75F;
	private static final float GRASS_HARDNESS = 0.6F;
	private static final float WING_HEALTH = 25F;
	private static final float TAIL_HEALTH = 50F;

	/** Ye Olde pack Biplane wheel positions (16ths). */
	private static final Vector3f[] WHEELS = {
			new Vector3f(2F / 16F, -13F / 16F, -6F / 16F),
			new Vector3f(2F / 16F, -13F / 16F, 6F / 16F),
			new Vector3f(-46F / 16F, -8F / 16F, 0F)
	};

	/** Ye Olde pack Biplane collision points: wing tips + tail sweep points. */
	private static final Vector3f[] WING_COLLISION_POINTS = {
			new Vector3f(0F, -2F / 16F, 20F / 16F),
			new Vector3f(0F, -2F / 16F, 40F / 16F),
			new Vector3f(0F, -2F / 16F, -20F / 16F),
			new Vector3f(0F, -2F / 16F, -40F / 16F),
			new Vector3f(-20F / 16F, -2F / 16F, 0F),
			new Vector3f(-40F / 16F, -2F / 16F, 0F)
	};

	/** Simulated placement physics, mirroring EntityPlane.tick. */
	static final class PlacedPlane
	{
		/** true = write actual displacement back into the velocity (the fix);
		 *  false = write the gravity-fed motion back (the bug). */
		boolean velocityWriteback;

		PlaneType type;
		RotatedAxes axes = new RotatedAxes();
		RotatedAxes prevAxes = new RotatedAxes();
		Vector3f planePos = new Vector3f(0F, 2.5F, 0F);
		Vector3f planeStart = new Vector3f(planePos);
		Vector3f motion = new Vector3f();
		Vector3f[] wheels = new Vector3f[WHEELS.length];
		Vector3f[] wheelPrev = new Vector3f[WHEELS.length];

		float wingHealth = WING_HEALTH;
		float tailHealth = TAIL_HEALTH;
		double totalDamage = 0.0;
		boolean exploded = false;
		int explosionTick = -1;
		double maxSpeedAtRest = 0.0;

		PlacedPlane(boolean velocityWriteback)
		{
			this.velocityWriteback = velocityWriteback;
			type = new PlaneType(new TypeFile("TestPack", EnumType.plane, "biplane", false));
			type.maxThrottle = 0.2F;
			type.drag = 0.5F;
			type.wheelSpringStrength = WHEEL_SPRING_STRENGTH;
			type.wheelPositions = new DriveablePosition[WHEELS.length];
			for(int i = 0; i < WHEELS.length; i++)
			{
				type.wheelPositions[i] = new DriveablePosition(new Vector3f(WHEELS[i]), EnumDriveablePart.coreWheel);
			}
			// Spawn like the placer constructor: yaw then resting pitch
			axes.rotateLocalPitch(5F);
			for(int i = 0; i < wheels.length; i++)
			{
				Vector3f offset = axes.findLocalVectorGlobally(WHEELS[i]);
				wheels[i] = Vector3f.add(planePos, offset, null);
				wheelPrev[i] = new Vector3f(wheels[i]);
			}
			prevAxes = velocityWriteback ? axes.clone() : new RotatedAxes();
		}

		void tick(int tick)
		{
			// --- forces (throttle 0, parked): gravity + drag via PlaneForces ---
			double speed = velocityWriteback ? speedOf(planeStart, planePos) : motion.length();
			PlaneForces.Result forces = PlaneForces.apply(
					0F, 0F, 0F, 0F,
					EnumPlaneMode.PLANE, (float)speed, (float)speedOfXZ(),
					motion.x, motion.y, motion.z,
					true, true, true, true,
					false, 1, 1, 1, 1,
					type, 1F, 1F, axes);
			motion = new Vector3f((float)forces.motionX, (float)forces.motionY, (float)forces.motionZ);

			// --- wheels fall with the plane motion ---
			for(int i = 0; i < wheels.length; i++)
			{
				wheelPrev[i] = new Vector3f(wheels[i]);
				wheels[i].x += motion.x;
				wheels[i].y += motion.y;
				wheels[i].z += motion.z;
				clampWheelToGround(wheels[i]);
			}

			// --- wheel spring loop, two iterations, exactly like the tick ---
			planeStart = new Vector3f(planePos);
			for(int iteration = 0; iteration < 2; iteration++)
			{
				Vector3f amountToMoveCar = new Vector3f(motion.x / 2F, motion.y / 2F, motion.z / 2F);

				for(int i = 0; i < wheels.length; i++)
				{
					Vector3f targetWheelPos = axes.findLocalVectorGlobally(WHEELS[i]);
					Vector3f currentWheelPos = Vector3f.sub(wheels[i], planePos, null);

					float targetWheelLength = targetWheelPos.length();
					float currentWheelLength = currentWheelPos.length();

					float dLength = targetWheelLength - currentWheelLength;
					float dAngle = (targetWheelPos.lengthSquared() < 1E-6F || currentWheelPos.lengthSquared() < 1E-6F)
							? 0F : Vector3f.angle(targetWheelPos, currentWheelPos);

					float newLength = currentWheelLength + dLength * WHEEL_SPRING_STRENGTH;
					Vector3f rotateAround = Vector3f.cross(targetWheelPos, currentWheelPos, null);

					Vector3f newWheelPos;
					if(rotateAround.lengthSquared() > 1E-6F && newLength > 1E-6F)
					{
						rotateAround.normalise();

						Matrix4f mat = new Matrix4f();
						mat.m00 = currentWheelPos.x;
						mat.m10 = currentWheelPos.y;
						mat.m20 = currentWheelPos.z;
						mat.rotate(dAngle * WHEEL_SPRING_STRENGTH, rotateAround);

						axes.rotateGlobal(-dAngle * WHEEL_SPRING_STRENGTH, rotateAround);

						newWheelPos = new Vector3f(mat.m00, mat.m10, mat.m20);
						newWheelPos.normalise().scale(newLength);
					}
					else
					{
						newWheelPos = new Vector3f(targetWheelPos);
						newWheelPos.normalise().scale(newLength);
					}

					Vector3f amountToMoveWheel = new Vector3f();
					amountToMoveWheel.x = (newWheelPos.x - currentWheelPos.x) * (1F - WHEEL_PROPORTION);
					amountToMoveWheel.y = (newWheelPos.y - currentWheelPos.y) * (1F - WHEEL_PROPORTION);
					amountToMoveWheel.z = (newWheelPos.z - currentWheelPos.z) * (1F - WHEEL_PROPORTION);

					amountToMoveCar.x -= amountToMoveWheel.x;
					amountToMoveCar.y -= amountToMoveWheel.y;
					amountToMoveCar.z -= amountToMoveWheel.z;

					amountToMoveCar.y += ((wheels[i].y - wheelPrev[i].y) - motion.y) * 0.5F / wheels.length;

					wheels[i].x += amountToMoveWheel.x;
					wheels[i].y += amountToMoveWheel.y;
					wheels[i].z += amountToMoveWheel.z;
					clampWheelToGround(wheels[i]);
				}

				planePos.x += amountToMoveCar.x;
				planePos.y += amountToMoveCar.y;
				planePos.z += amountToMoveCar.z;
				if(planePos.y < ENTITY_HALF_HEIGHT)
					planePos.y = (float)ENTITY_HALF_HEIGHT;
			}

			// --- velocity write-back: the bug vs the fix ---
			if(velocityWriteback)
			{
				motion = new Vector3f(planePos.x - planeStart.x, planePos.y - planeStart.y, planePos.z - planeStart.z);
			}
			maxSpeedAtRest = Math.max(maxSpeedAtRest, Math.abs(motion.y));

			// --- checkForCollisions sweep, simplified to the vertical ground plane ---
			double crashSpeed = Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
			for(Vector3f point : WING_COLLISION_POINTS)
			{
				Vector3f lastRel = prevAxes.findLocalVectorGlobally(point);
				Vector3f currRel = axes.findLocalVectorGlobally(point);
				double lastY = planeStart.y + lastRel.y;
				double currY = planePos.y + currRel.y;

				boolean hit = false;
				if(lastY < GROUND_TOP || currY < GROUND_TOP)
					hit = true;
				else if((lastY - GROUND_TOP) * (currY - GROUND_TOP) < 0)
					hit = true;

				if(hit)
				{
					double damage = crashSpeed * GRASS_HARDNESS * GRASS_HARDNESS;
					if(damage > 0.1D) // the checkForCollisions fix: only real impacts count
					{
						boolean tailPoint = point.x <= -20F / 16F + 1E-3F;
						if(tailPoint)
							tailHealth -= (float)damage;
						else
							wingHealth -= (float)damage;
						totalDamage += damage;
					}
					if((tailHealth <= 0 || wingHealth <= 0) && !exploded)
					{
						exploded = true;
						explosionTick = tick;
					}
				}
			}
			prevAxes = axes.clone();
		}

		private void clampWheelToGround(Vector3f wheel)
		{
			if(wheel.y < ENTITY_HALF_HEIGHT)
				wheel.y = (float)ENTITY_HALF_HEIGHT;
		}

		private static double speedOf(Vector3f from, Vector3f to)
		{
			double dx = to.x - from.x;
			double dy = to.y - from.y;
			double dz = to.z - from.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		private double speedOfXZ()
		{
			return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		}
	}

	@Test
	void buggyWritebackLeavesParkedPlaneFallingAtTerminalVelocity()
	{
		PlacedPlane plane = new PlacedPlane(false);
		for(int tick = 0; tick < 300; tick++)
			plane.tick(tick);

		// The bug signature: a resting plane reports a large downward velocity
		assertTrue(plane.maxSpeedAtRest > 0.5,
				"buggy velocity write-back: parked plane keeps a gravity-fed "
						+ "falling velocity, got |vy| max " + plane.maxSpeedAtRest);
	}

	@Test
	void fixedWritebackLetsParkedPlaneSettle()
	{
		PlacedPlane plane = new PlacedPlane(true);
		for(int tick = 0; tick < 300; tick++)
			plane.tick(tick);

		// The placement drop itself reaches ~0.3 m/tick for a few ticks; after
		// settling, the resting velocity must be ~zero
		double lateMaxSpeed = 0.0;
		for(int tick = 300; tick < 600; tick++)
		{
			plane.tick(tick);
			lateMaxSpeed = Math.max(lateMaxSpeed, Math.abs(plane.motion.y));
		}
		assertTrue(lateMaxSpeed < 0.05,
				"with the fix, a settled plane's velocity must be near zero, got |vy| max "
						+ lateMaxSpeed);
		assertFalse(plane.exploded,
				"a parked plane must never take crash damage and explode"
						+ (plane.exploded ? " (exploded at tick " + plane.explosionTick + ")" : ""));
		assertEquals(0.0, plane.totalDamage, 1e-6,
				"a parked plane must not accumulate wall damage from resting contact");
		assertTrue(Double.isFinite(plane.planePos.y) && plane.planePos.y >= 0.5 && plane.planePos.y <= 4.0,
				"the plane must settle near the ground, got y=" + plane.planePos.y);
	}
}
