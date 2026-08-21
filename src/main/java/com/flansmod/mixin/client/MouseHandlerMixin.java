package com.flansmod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.SmoothDouble;

import com.flansmod.client.handlers.MouseInputHandler;
import com.flansmod.common.driveables.EntitySeat;

/**
 * Captures the per-frame mouse deltas while riding an EntitySeat and cancels
 * the vanilla player turn. Vanilla applies rotation to the LocalPlayer from
 * the private accumulated deltas in turnPlayer(D) right before zeroing them;
 * EntitySeat.updatePosition() then overwrites the player rotation every tick
 * with the driveable-derived look, so the vanilla turn only causes the view
 * to jump toward the mouse and snap back. Cancelling makes the seat's
 * per-tick look the sole owner of the view (smoothed by vanilla render
 * interpolation), while the accumulated deltas are routed to
 * MouseInputHandler and applied to game state once per tick.
 *
 * Only EntitySeat rides are cancelled: EntityAAGun and mounted-gun aiming
 * follow the vanilla player look and must keep receiving it.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin
{
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;
	@Shadow
	private SmoothDouble smoothTurnX;
	@Shadow
	private SmoothDouble smoothTurnY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void flansmod$captureMouseBeforeVanillaTurn(double sensitivity, CallbackInfo ci)
	{
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if(player != null && player.getVehicle() instanceof EntitySeat)
		{
			MouseInputHandler.captureMouse(accumulatedDX, accumulatedDY, mc.screen != null);
			//Vanilla MouseHandler.onMove zeroes accumulatedDX/accumulatedDY
			//right after turnPlayer returns, so cancelling here cannot cause
			//the deltas to be counted twice.
			//Reset the smooth-turn accumulators too, so no residual vanilla
			//turn delta snaps the view right after dismounting.
			smoothTurnX.reset();
			smoothTurnY.reset();
			ci.cancel();
		}
	}
}
