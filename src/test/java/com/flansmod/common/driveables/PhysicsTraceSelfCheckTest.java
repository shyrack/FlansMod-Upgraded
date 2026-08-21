package com.flansmod.common.driveables;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.flansmod.common.RotatedAxes;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.TypeFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Self-check for the physics telemetry pipeline used by the flansmod-physics
 * skill: scripts a PLANE takeoff through PlaneForces, feeds every tick into
 * the same trace flow as EntityDriveable.logPhysicsTick (acc delta, anomaly
 * analysis, RecordingPhysicsTracer) and verifies that the in-game log format
 * (LoggingPhysicsTracer.format) matches the documented layout.
 *
 * The forecast lines are printed to stdout so a failing CI run or a developer
 * can read the predicted future positions/velocities/accelerations directly.
 */
class PhysicsTraceSelfCheckTest
{
	@Test
	void forecastAndTracePipelineProducesReadablePhysicsLines()
	{
		PlaneType type = new PlaneType(new TypeFile("TestPack", EnumType.plane, "selfcheck_plane", false));
		type.maxThrottle = 1F;
		type.drag = 1F;

		RotatedAxes axes = new RotatedAxes();
		RecordingPhysicsTracer tracer = new RecordingPhysicsTracer();

		double motX = 0D, motY = 0D, motZ = 0D;
		double posX = 0D, posY = 0D, posZ = 0D;
		float throttle = 0F;

		final int ticks = 60;
		for(int tick = 0; tick < ticks; tick++)
		{
			throttle = Math.min(1F, tick / 30F);
			double lastTickSpeed = Math.sqrt(motX * motX + motY * motY + motZ * motZ);
			double speedXZ = Math.sqrt(motX * motX + motZ * motZ);

			PlaneForces.Result result = PlaneForces.apply(
					throttle, 0F, 0F, 0F, EnumPlaneMode.PLANE,
					(float)lastTickSpeed, speedXZ,
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
		}

		try
		{
			// 1. Every forecast tick was recorded
			assertEquals(ticks, tracer.traces.size(), "tracer must record one trace per tick");

			// 2. The scripted takeoff must be anomaly-free and finite
			for(PhysicsTickTrace trace : tracer.traces)
			{
				assertTrue(Double.isFinite(trace.posX()) && Double.isFinite(trace.velX())
						&& Double.isFinite(trace.accX()), "non-finite trace at tick " + trace.tick());
				assertTrue(trace.anomalies().isEmpty(),
						"unexpected anomalies at tick " + trace.tick() + ": " + trace.anomalies());
			}

			// 3. Speed helpers agree with the raw components
			PhysicsTickTrace last = tracer.traces.get(tracer.traces.size() - 1);
			assertEquals(Math.sqrt(last.velX() * last.velX() + last.velY() * last.velY()
					+ last.velZ() * last.velZ()), last.speed(), 1e-12);
			assertEquals(Math.sqrt(last.accX() * last.accX() + last.accY() * last.accY()
					+ last.accZ() * last.accZ()), last.accMag(), 1e-12);

			// 4. Inertia accumulated: final horizontal speed far beyond one
			//    tick of thrust (write-back regression stays fixed)
			double finalXZ = Math.sqrt(last.velX() * last.velX() + last.velZ() * last.velZ());
			assertTrue(finalXZ > 0.2, "velocity must accumulate across ticks, got " + finalXZ);

			// 5. The in-game log format contains every documented field
			String line = LoggingPhysicsTracer.format(last);
			for(String field : new String[] {"[DriveablePhysics][S] Plane #1", "pos=(",
					"vel=(", "speed=", "acc=(", "accMag=", "rot=(", "throttle=", "ground=",
					"seat0=("})
			{
				assertTrue(line.contains(field), "log line must contain '" + field + "': " + line);
			}
			assertFalse(line.contains("player=("),
					"player offset must be omitted when seat 0 has no passenger: " + line);

			// 6. Diagnostics threshold sanity: a teleported trace is flagged
			PhysicsTickTrace teleported = new PhysicsTickTrace(
					ticks, "S", "Plane", 1,
					last.posX() + 100, last.posY(), last.posZ(),
					last.velX(), last.velY(), last.velZ(), 0D, 0D, 0D,
					last.rotYaw(), last.rotPitch(), last.rotRoll(),
					last.throttle(), false, 0D, 0D, 0D, null, null, null, List.of());
			assertTrue(PhysicsDiagnostics.analyze(last, teleported).stream()
					.anyMatch(a -> a.contains("position jumped")),
					"a 100-block teleport must be flagged as a position jump");
		}
		catch(AssertionError e)
		{
			throw new AssertionError(e.getMessage() + "\nFull forecast:\n" + tracer.formatTrace(), e);
		}

		// 7. Print the forecast: this is the "seeing the future" output the
		//    skill relies on (same format as run/logs/debug.log in-game)
		System.out.println("[flansmod-physics self-check] forecast of " + ticks
				+ " ticks (throttle ramp 0 -> 1):");
		for(PhysicsTickTrace trace : tracer.traces)
		{
			System.out.println(LoggingPhysicsTracer.format(trace));
		}
	}
}
