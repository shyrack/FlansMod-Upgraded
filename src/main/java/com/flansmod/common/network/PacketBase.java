package com.flansmod.common.network;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base class for all packets in Flan's Mod.
 */
public abstract class PacketBase
{
	/**
	 * Encode the packet into a ByteBuf stream.
	 */
	public abstract void encodeInto(ByteBuf data);

	/**
	 * Decode the packet from a ByteBuf stream.
	 */
	public abstract void decodeInto(ByteBuf data);

	/**
	 * Handle the packet on server side, post-decoding
	 */
	public abstract void handleServerSide(ServerPlayer playerEntity);

	/**
	 * Handle the packet on client side, post-decoding
	 */
	public abstract void handleClientSide(Player clientPlayer);

	/**
	 * Util method for quickly writing strings
	 */
	public static void writeUTF(ByteBuf data, String s)
	{
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		data.writeShort(bytes.length);
		data.writeBytes(bytes);
	}

	/**
	 * Util method for quickly reading strings
	 */
	public static String readUTF(ByteBuf data)
	{
		short length = data.readShort();
		byte[] bytes = new byte[length];
		data.readBytes(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
