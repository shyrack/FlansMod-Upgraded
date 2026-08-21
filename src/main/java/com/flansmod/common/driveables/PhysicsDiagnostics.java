package com.flansmod.common.driveables;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure static anomaly detection for driveable physics traces.
 *
 * Thresholds:
 * <ul>
 *   <li>position jump &gt; 32 blocks between ticks ("position jumped N blocks")</li>
 *   <li>speed &gt; 10 ("hit speed cap")</li>
 *   <li>acceleration magnitude &gt; 600 ("acceleration spike")</li>
 *   <li>any non-finite value in the trace ("NaN/Inf in motion")</li>
 * </ul>
 */
public final class PhysicsDiagnostics
{
	private PhysicsDiagnostics()
	{
	}

	public static List<String> analyze(PhysicsTickTrace prev, PhysicsTickTrace curr)
	{
		List<String> anomalies = new ArrayList<>();

		if(prev != null)
		{
			double dX = curr.posX() - prev.posX();
			double dY = curr.posY() - prev.posY();
			double dZ = curr.posZ() - prev.posZ();
			double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
			if(distance > 32D)
			{
				anomalies.add(String.format("position jumped %.2f blocks", distance));
			}
		}

		if(curr.speed() > 10D)
		{
			anomalies.add(String.format("hit speed cap (speed %.2f)", curr.speed()));
		}

		if(curr.accMag() > 600D)
		{
			anomalies.add(String.format("acceleration spike (accMag %.2f)", curr.accMag()));
		}

		if(!isFinite(curr))
		{
			anomalies.add("NaN/Inf in motion");
		}

		return anomalies;
	}

	private static boolean isFinite(PhysicsTickTrace trace)
	{
		return isFinite(trace.posX()) && isFinite(trace.posY()) && isFinite(trace.posZ())
				&& isFinite(trace.velX()) && isFinite(trace.velY()) && isFinite(trace.velZ())
				&& isFinite(trace.accX()) && isFinite(trace.accY()) && isFinite(trace.accZ())
				&& isFinite(trace.rotYaw()) && isFinite(trace.rotPitch()) && isFinite(trace.rotRoll())
				&& isFinite(trace.throttle())
				&& isFinite(trace.seatOffsetX()) && isFinite(trace.seatOffsetY())
				&& isFinite(trace.seatOffsetZ());
	}

	private static boolean isFinite(double value)
	{
		return !Double.isNaN(value) && !Double.isInfinite(value);
	}

	private static boolean isFinite(float value)
	{
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}
}
