package com.flansmod.client.handlers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import com.flansmod.client.ClientRenderHooks;
import com.flansmod.client.FlansModClient;
import com.flansmod.client.model.InstantBulletRenderer;
import com.flansmod.client.model.RenderFlag;
import com.flansmod.client.model.RenderGun;

/**
 * All handled events for the client should go through here and be passed on, this makes it easier to see which events
 * are being handled by the mod
 */
public class ClientEventHandler
{
	private KeyInputHandler keyInputHandler = new KeyInputHandler();
	private MouseInputHandler mouseInputHandler = new MouseInputHandler();
	private ClientRenderHooks renderHooks = new ClientRenderHooks();

	public ClientEventHandler()
	{
		ClientTickEvents.END_CLIENT_TICK.register(minecraft ->
		{
			clientTick(minecraft);
		});
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, chatMessage, sender, bound, instant) ->
		{
			return chatMessage(message);
		});
	}

	private void clientTick(Minecraft mc)
	{
		float renderTickTime = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		RenderGun.smoothing = renderTickTime;
		FlansModClient.updateCameraZoom(renderTickTime);
		renderHooks.setPartialTick(renderTickTime);
		renderHooks.updatePlayerView();

		//Handle all packets received since last tick
		FlansModClient.updateFlashlights(mc);

		InstantBulletRenderer.UpdateAllTrails();
		renderHooks.update();
		RenderFlag.angle += 2F;
		FlansModClient.tick();
		keyInputHandler.checkTickKeys();
		keyInputHandler.checkEventKeys();
	}

	private boolean chatMessage(Component message)
	{
		return !message.getString().equals("#flansmod");
	}
}
