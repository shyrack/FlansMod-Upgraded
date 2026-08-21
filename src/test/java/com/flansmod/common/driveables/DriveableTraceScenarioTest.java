package com.flansmod.common.driveables;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.TypeFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end scripted scenario: plane takeoff -> climb -> level -> coast,
 * driven through PlaneForces with PhysicsDiagnostics and a
 * RecordingPhysicsTracer. On any assertion failure the full recorded trace
 * is included in the failure message, so the log alone shows the physics
 * picture without needing a debugger or the user.
 */
class DriveableTraceScenarioTest
{
	@Test
	void scriptedFlightHoldsAllInvariants()
	{
		PlaneType type = new PlaneType(new TypeFile("TestPack", EnumType.plane, "scenario_plane", false));
		type.maxThrottle = 1F;
		type.drag = 1F;

		RotatedAxes axes = new RotatedAxes();
		RecordingPhysicsTracer tracer = new RecordingPhysicsTracer();

		double motX = 0D, motY = 0D, motZ = 0D;
		double posX = 0D, posY = 0D, posZ = 0D;
		float throttle = 0F;
		float flapsPitchLeft = 0F, flapsPitchRight = 0F;

		// Phase boundaries: takeoff ramp -> cruise -> climb -> coast
		final int cruiseStart = 100;
		final int climbStart = 200;
		final int coastStart = 300;
		final int totalTicks = 400;

		double altitudeAtClimbStart = 0D;
		double altitudeAtClimbEnd = 0D;
		double maxSpeed = 0D;

		try
		{
			for(int tick = 0; tick < totalTicks; tick++)
			{
				if(tick < cruiseStart)
				{
					// Takeoff: throttle ramps 0 -> 1
					throttle = Math.min(1F, tick / 100F);
					flapsPitchLeft = flapsPitchRight = 0F;
				}
				else if(tick < climbStart)
				{
					// Cruise: full throttle, wings level
					throttle = 1F;
					flapsPitchLeft = flapsPitchRight = 0F;
				}
				else if(tick < coastStart)
				{
					// Climb: positive flap input pitches the nose up in this
					// physics convention (getPitch goes negative)
					throttle = 1F;
					flapsPitchLeft = flapsPitchRight = 1F;
				}
				else
				{
					// Coast: throttle to idle
					throttle = 0F;
					flapsPitchLeft = flapsPitchRight = 0F;
				}

				double lastTickSpeed = Math.sqrt(motX * motX + motY * motY + motZ * motZ);
				double speedXZ = Math.sqrt(motX * motX + motZ * motZ);
				PlaneForces.Result result = PlaneForces.apply(
						throttle, 0F, flapsPitchLeft, flapsPitchRight,
						EnumPlaneMode.PLANE, (float)lastTickSpeed, speedXZ,
						motX, motY, motZ,
						true, true, true, true,
						true, 2, 2, 2, 2,
						type, 1F, 1F, axes);
				motX = result.motionX;
				motY = result.motionY;
				motZ = result.motionZ;
				posX += motX;
				posY += motY;
				posZ += motZ;

				double speed = Math.sqrt(motX * motX + motY * motY + motZ * motZ);
				maxSpeed = Math.max(maxSpeed, speed);

				double accX = 0D, accY = 0D, accZ = 0D;
				if(!tracer.traces.isEmpty())
				{
					PhysicsTickTrace prev = tracer.traces.get(tracer.traces.size() - 1);
					accX = (motX - prev.velX()) * 20;
					accY = (motY - prev.velY()) * 20;
					accZ = (motZ - prev.velZ()) * 20;
				}

				PhysicsTickTrace trace = new PhysicsTickTrace(
						tick, "S", "Plane", 1,
						posX, posY, posZ, motX, motY, motZ, accX, accY, accZ,
						axes.getYaw(), axes.getPitch(), axes.getRoll(),
						throttle, false,
						0D, 0D, 0D, null, null, null, List.of());

				List<String> anomalies = tracer.traces.isEmpty()
						? PhysicsDiagnostics.analyze(null, trace)
						: PhysicsDiagnostics.analyze(tracer.traces.get(tracer.traces.size() - 1), trace);
				trace = trace.withAnomalies(anomalies);
				tracer.onTick(trace);

				if(tick == climbStart)
					altitudeAtClimbStart = posY;
				if(tick == coastStart)
					altitudeAtClimbEnd = posY;
			}
		}
		catch(AssertionError | RuntimeException e)
		{
			throw new AssertionError(e.getMessage() + "\nFull trace:\n" + tracer.formatTrace(), e);
		}

		try
		{
			assertEquals(totalTicks, tracer.traces.size());

			// Every tick must be finite and free of anomalies
			for(PhysicsTickTrace trace : tracer.traces)
			{
				assertTrue(Double.isFinite(trace.posX()) && Double.isFinite(trace.posY())
						&& Double.isFinite(trace.posZ()) && Double.isFinite(trace.velX())
						&& Double.isFinite(trace.velY()) && Double.isFinite(trace.velZ()),
						"non-finite trace at tick " + trace.tick());
				assertTrue(trace.anomalies().isEmpty(),
						"unexpected anomalies at tick " + trace.tick() + ": " + trace.anomalies());
			}

			// Speed cap of 10 must be respected throughout
			assertTrue(maxSpeed <= 10.0 + 1e-6, "speed cap violated: " + maxSpeed);

			// Takeoff + cruise must have produced meaningful speed: this plane
			// config converges to a ~0.456 m/tick cruise speed (the corrected
			// speed target only gains 0.04 per tick over the current speed)
			PhysicsTickTrace cruiseTrace = tracer.traces.get(climbStart);
			double cruiseSpeed = Math.sqrt(cruiseTrace.velX() * cruiseTrace.velX()
					+ cruiseTrace.velY() * cruiseTrace.velY()
					+ cruiseTrace.velZ() * cruiseTrace.velZ());
			assertTrue(cruiseSpeed > 0.3, "cruise speed too low: " + cruiseSpeed);

			// The climb phase must gain altitude
			assertTrue(altitudeAtClimbEnd > altitudeAtClimbStart + 1.0,
					"climb must gain altitude: " + altitudeAtClimbStart + " -> "
							+ altitudeAtClimbEnd);

			// Coasting must bleed horizontal speed (drag decays it; gravity
			// still pulls the plane down and does not count as drag)
			PhysicsTickTrace coastStartTrace = tracer.traces.get(coastStart);
			double coastStartXZ = Math.sqrt(coastStartTrace.velX() * coastStartTrace.velX()
					+ coastStartTrace.velZ() * coastStartTrace.velZ());
			PhysicsTickTrace last = tracer.traces.get(tracer.traces.size() - 1);
			double coastEndXZ = Math.sqrt(last.velX() * last.velX()
					+ last.velZ() * last.velZ());
			assertTrue(coastEndXZ < coastStartXZ / 2,
					"coast must bleed horizontal speed: " + coastStartXZ + " -> " + coastEndXZ);
		}
		catch(AssertionError e)
		{
			throw new AssertionError(e.getMessage() + "\nFull trace:\n" + tracer.formatTrace(), e);
		}
	}
}
