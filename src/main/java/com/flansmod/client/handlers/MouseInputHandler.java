package com.flansmod.client.handlers;

import com.flansmod.api.IControllable;

/**
 * Static, Minecraft-free accumulator for per-frame mouse deltas.
 *
 * The deltas are captured per frame (see MouseHandlerMixin) and applied to
 * game state once per client tick via {@link #flushMouse(IControllable)}, so
 * plane flap sensitivity and seat look-around are frame-rate independent and
 * match the 20Hz physics tick.
 *
 * This class deliberately takes all world/player state as method arguments
 * so it can be unit tested without a Minecraft instance.
 */
public class MouseInputHandler
{
	private static double accumulatedDX = 0D;
	private static double accumulatedDY = 0D;

	public static void init()
	{
	}

	/**
	 * Called per frame with the raw deltas accumulated by vanilla.
	 * While a screen is open the deltas are dropped and the accumulators are
	 * zeroed, so a burst of pre-screen movement is not applied when the
	 * screen closes.
	 */
	public static void captureMouse(double dx, double dy, boolean screenOpen)
	{
		if(screenOpen)
		{
			accumulatedDX = 0D;
			accumulatedDY = 0D;
			return;
		}

		accumulatedDX += dx;
		accumulatedDY += dy;
	}

	/**
	 * Called once per client tick. Routes the accumulated deltas to the
	 * controllable entity being ridden, then resets the accumulators.
	 */
	public static void flushMouse(IControllable ridden)
	{
		if(ridden != null)
		{
			ridden.onMouseMoved(accumulatedDX, accumulatedDY);
		}
		accumulatedDX = 0D;
		accumulatedDY = 0D;
	}

	/**
	 * Test access for the current accumulator values.
	 */
	static double getAccumulatedDX()
	{
		return accumulatedDX;
	}

	static double getAccumulatedDY()
	{
		return accumulatedDY;
	}

	static void resetAccumulators()
	{
		accumulatedDX = 0D;
		accumulatedDY = 0D;
	}
}
