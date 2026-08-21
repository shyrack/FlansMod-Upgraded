package com.flansmod.common.driveables;

import com.flansmod.common.FlansMod;

/**
 * Default in-game sink: emits one formatted line per tick to FlansMod.log and
 * logs any anomalies at WARN.
 */
public class LoggingPhysicsTracer implements IPhysicsTracer
{
	@Override
	public void onTick(PhysicsTickTrace trace)
	{
		FlansMod.log.info(format(trace));
		for(String anomaly : trace.anomalies())
		{
			FlansMod.log.warn("[DriveablePhysics][{}] {} #{}: {}", trace.side(), trace.typeName(),
					trace.entityId(), anomaly);
		}
	}

	/**
	 * Shared single-line formatting, also used by the test-side
	 * RecordingPhysicsTracer so failure dumps look identical to game logs.
	 */
	public static String format(PhysicsTickTrace trace)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[DriveablePhysics][").append(trace.side()).append("] ")
				.append(trace.typeName()).append(" #").append(trace.entityId())
				.append(" pos=(").append(format3(trace.posX(), trace.posY(), trace.posZ())).append(')')
				.append(" vel=(").append(format3(trace.velX(), trace.velY(), trace.velZ())).append(')')
				.append(String.format(" speed=%.2f", trace.speed()))
				.append(" acc=(").append(format3(trace.accX(), trace.accY(), trace.accZ())).append(')')
				.append(String.format(" accMag=%.2f", trace.accMag()))
				.append(String.format(" rot=(%.2f, %.2f, %.2f)", trace.rotYaw(), trace.rotPitch(),
						trace.rotRoll()))
				.append(String.format(" throttle=%.2f", trace.throttle()))
				.append(" ground=").append(trace.onGround())
				.append(" seat0=(").append(format3(trace.seatOffsetX(), trace.seatOffsetY(),
						trace.seatOffsetZ())).append(')');
		if(trace.playerOffsetX() != null)
		{
			builder.append(" player=(").append(format3(trace.playerOffsetX(), trace.playerOffsetY(),
					trace.playerOffsetZ())).append(')');
		}
		return builder.toString();
	}

	private static String format3(double x, double y, double z)
	{
		return String.format("%.2f, %.2f, %.2f", x, y, z);
	}
}
