package com.flansmod.common.driveables;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhysicsDiagnostics: each anomaly class produces its warning,
 * and a clean scripted flight produces none.
 */
class PhysicsDiagnosticsTest
{
	private static PhysicsTickTrace trace(double px, double py, double pz,
										  double vx, double vy, double vz,
										  double ax, double ay, double az)
	{
		return new PhysicsTickTrace(1, "S", "Plane", 1,
				px, py, pz, vx, vy, vz, ax, ay, az,
				0F, 0F, 0F, 0.5F, false,
				0D, 0D, 0D, null, null, null, List.of());
	}

	@Test
	void teleportProducesPositionJumpWarning()
	{
		PhysicsTickTrace prev = trace(0D, 0D, 0D, 1D, 0D, 0D, 0D, 0D, 0D);
		PhysicsTickTrace curr = trace(100D, 0D, 0D, 1D, 0D, 0D, 0D, 0D, 0D);
		List<String> anomalies = PhysicsDiagnostics.analyze(prev, curr);
		assertFalse(anomalies.isEmpty());
		assertTrue(anomalies.stream().anyMatch(a -> a.contains("position jumped")),
				"teleport must produce a position jump warning: " + anomalies);
	}

	@Test
	void overspeedProducesSpeedCapWarning()
	{
		PhysicsTickTrace prev = trace(0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D);
		PhysicsTickTrace curr = trace(0.1D, 0D, 0D, 11D, 0D, 0D, 0D, 0D, 0D);
		List<String> anomalies = PhysicsDiagnostics.analyze(prev, curr);
		assertTrue(anomalies.stream().anyMatch(a -> a.contains("hit speed cap")),
				"speed above 10 must produce a cap warning: " + anomalies);
	}

	@Test
	void accelerationSpikeProducesWarningWithoutOverspeed()
	{
		// acc = (1 - (-30)) * 20 = 620 > 600, but speed is only 1
		PhysicsTickTrace prev = trace(0D, 0D, 0D, -30D, 0D, 0D, 0D, 0D, 0D);
		PhysicsTickTrace curr = trace(0.1D, 0D, 0D, 1D, 0D, 0D, 620D, 0D, 0D);
		List<String> anomalies = PhysicsDiagnostics.analyze(prev, curr);
		assertTrue(anomalies.stream().anyMatch(a -> a.contains("acceleration spike")),
				"acceleration above 600 must produce a spike warning: " + anomalies);
		assertFalse(anomalies.stream().anyMatch(a -> a.contains("hit speed cap")),
				"speed 1 must not trigger the cap warning: " + anomalies);
	}

	@Test
	void nonFiniteMotionProducesNaNWarning()
	{
		PhysicsTickTrace prev = trace(0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D);
		PhysicsTickTrace curr = trace(Double.NaN, 0D, 0D, 0.5D, 0D, 0D, 0D, 0D, 0D);
		List<String> anomalies = PhysicsDiagnostics.analyze(prev, curr);
		assertTrue(anomalies.stream().anyMatch(a -> a.contains("NaN/Inf")),
				"non-finite position must produce a NaN warning: " + anomalies);
	}

	@Test
	void cleanFlightProducesNoAnomalies()
	{
		// A scripted takeoff: small deltas, bounded speed and acceleration
		PhysicsTickTrace prev = trace(0D, 0D, 0D, 0.5D, 0D, 0D, 0D, 0D, 0D);
		for(int tick = 1; tick <= 100; tick++)
		{
			PhysicsTickTrace curr = trace(0.5D * tick, 0D, 0D,
					0.5D + 0.01D * tick, 0D, 0D,
					0.2D, 0D, 0D);
			List<String> anomalies = PhysicsDiagnostics.analyze(prev, curr);
			assertTrue(anomalies.isEmpty(),
					"clean flight must produce no anomalies at tick " + tick + ": " + anomalies);
			prev = curr;
		}
	}

	@Test
	void nullPreviousTickOnlyChecksUnconditionalInvariants()
	{
		PhysicsTickTrace curr = trace(0D, 0D, 0D, 0.5D, 0D, 0D, 0D, 0D, 0D);
		assertTrue(PhysicsDiagnostics.analyze(null, curr).isEmpty());
	}
}
