package com.flansmod.common.driveables;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-side tracer: records every PhysicsTickTrace so simulations can be
 * asserted on and printed in full when an assertion fails.
 */
public class RecordingPhysicsTracer implements IPhysicsTracer
{
	public final List<PhysicsTickTrace> traces = new ArrayList<>();

	@Override
	public void onTick(PhysicsTickTrace trace)
	{
		traces.add(trace);
	}

	/**
	 * Full formatted dump, using the same line format as LoggingPhysicsTracer
	 * so failure output reads exactly like the in-game log.
	 */
	public String formatTrace()
	{
		StringBuilder builder = new StringBuilder();
		for(PhysicsTickTrace trace : traces)
		{
			builder.append(LoggingPhysicsTracer.format(trace));
			for(String anomaly : trace.anomalies())
			{
				builder.append("  [anomaly] ").append(anomaly);
			}
			builder.append('\n');
		}
		return builder.toString();
	}
}
