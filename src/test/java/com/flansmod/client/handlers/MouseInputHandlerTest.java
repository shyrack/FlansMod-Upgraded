package com.flansmod.client.handlers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.flansmod.api.IControllable;
import com.flansmod.common.driveables.EntitySeat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the static, Minecraft-free mouse accumulator. Per-frame
 * deltas are captured by MouseHandlerMixin and flushed once per client tick
 * into the controllable being ridden.
 */
class MouseInputHandlerTest
{
	/** Minimal fake IControllable that records the routed deltas. */
	private static final class RecordingControllable implements IControllable
	{
		double totalDX;
		double totalDY;
		int calls;

		@Override
		public void onMouseMoved(double deltaX, double deltaY)
		{
			totalDX += deltaX;
			totalDY += deltaY;
			calls++;
		}

		@Override
		public boolean pressKey(int key, Player player, boolean isOnEvent)
		{
			return false;
		}

		@Override
		public boolean serverHandleKeyPress(int key, Player player)
		{
			return false;
		}

		@Override
		public void updateKeyHeldState(int key, boolean held)
		{
		}

		@Override
		public Entity getControllingEntity()
		{
			return null;
		}

		@Override
		public boolean isDead()
		{
			return false;
		}

		@Override
		public float getPlayerRoll()
		{
			return 0F;
		}

		@Override
		public float getPrevPlayerRoll()
		{
			return 0F;
		}

		@Override
		public float getCameraDistance()
		{
			return 0F;
		}

		@Override
		public Entity getCamera()
		{
			return null;
		}

		@Override
		public EntitySeat getSeat(LivingEntity living)
		{
			return null;
		}
	}

	@AfterEach
	void reset()
	{
		MouseInputHandler.resetAccumulators();
	}

	@Test
	void flushRoutesExactlyTheSumOnceThenZeroesAccumulators()
	{
		MouseInputHandler.captureMouse(1.5, -2.25, false);
		MouseInputHandler.captureMouse(0.5, 0.75, false);
		MouseInputHandler.captureMouse(2.0, 1.0, false);

		RecordingControllable controllable = new RecordingControllable();
		MouseInputHandler.flushMouse(controllable);

		assertEquals(1, controllable.calls, "flush must route exactly once per tick");
		assertEquals(4.0, controllable.totalDX, 1e-9, "deltaX must be the exact sum");
		assertEquals(-0.5, controllable.totalDY, 1e-9, "deltaY must be the exact sum");

		// Accumulators must be zeroed by the flush
		assertEquals(0D, MouseInputHandler.getAccumulatedDX(), 1e-12);
		assertEquals(0D, MouseInputHandler.getAccumulatedDY(), 1e-12);

		// A second flush must route nothing
		RecordingControllable second = new RecordingControllable();
		MouseInputHandler.flushMouse(second);
		assertEquals(0.0, second.totalDX, 1e-12);
		assertEquals(0.0, second.totalDY, 1e-12);
	}

	@Test
	void captureWhileScreenIsOpenDropsAndZeroesDeltas()
	{
		MouseInputHandler.captureMouse(5.0, 5.0, false);
		MouseInputHandler.captureMouse(3.0, 3.0, true);

		assertEquals(0D, MouseInputHandler.getAccumulatedDX(), 1e-12);
		assertEquals(0D, MouseInputHandler.getAccumulatedDY(), 1e-12);

		RecordingControllable controllable = new RecordingControllable();
		MouseInputHandler.flushMouse(controllable);
		assertEquals(0.0, controllable.totalDX, 1e-12,
				"deltas captured before a screen opened must be dropped");
		assertEquals(0.0, controllable.totalDY, 1e-12);
	}

	@Test
	void flushWithNullRiddenIsANoOp()
	{
		MouseInputHandler.captureMouse(2.0, 1.0, false);
		MouseInputHandler.flushMouse(null);
		assertEquals(0D, MouseInputHandler.getAccumulatedDX(), 1e-12,
				"flush with null ridden must still reset the accumulators");
		assertEquals(0D, MouseInputHandler.getAccumulatedDY(), 1e-12);
	}

	@Test
	void fractionalDeltasAreNotTruncatedToInts()
	{
		// Sub-pixel movement (high-DPI slow motion) must survive: the old
		// int-cast API truncated these to zero
		MouseInputHandler.captureMouse(0.4, -0.6, false);
		RecordingControllable controllable = new RecordingControllable();
		MouseInputHandler.flushMouse(controllable);
		assertEquals(0.4, controllable.totalDX, 1e-9);
		assertEquals(-0.6, controllable.totalDY, 1e-9);
	}
}
