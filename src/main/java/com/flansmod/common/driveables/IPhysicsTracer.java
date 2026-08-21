package com.flansmod.common.driveables;

/**
 * Pluggable sink for physics telemetry. The default sink is
 * LoggingPhysicsTracer; tests install a RecordingPhysicsTracer so simulated
 * flights can be asserted on and printed on failure.
 */
public interface IPhysicsTracer
{
	void onTick(PhysicsTickTrace trace);
}
