package com.flansmod.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.flansmod.common.network.PacketHandler;

/**
 * Client-side packet registration, kept out of the common PacketHandler so a
 * dedicated server never resolves client-only classes (the lambda here
 * references LocalPlayer via ClientPlayNetworking.Context#player, which
 * fabric-loader refuses to load in a SERVER environment).
 */
public class ClientPacketRegistration
{
	private ClientPacketRegistration()
	{
	}

	public static void register()
	{
		ClientPlayNetworking.registerGlobalReceiver(PacketHandler.TYPE,
				(payload, context) -> context.client().execute(() ->
						payload.packet.handleClientSide(context.player())));
	}
}
