package com.flansmod.common.network;

import java.util.LinkedList;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;

import com.flansmod.common.FlansMod;

/**
 * Flan's Mod packet handler class. Directs packet data to packet classes.
 * Packets are transported as a single {@link CustomPacketPayload} whose body
 * carries a packet discriminator followed by the packet's encoded data.
 */
public class PacketHandler
{
	public static final CustomPacketPayload.Type<FlansModPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("flansmod", "main"));

	public static class FlansModPayload implements CustomPacketPayload
	{
		public final PacketBase packet;

		public FlansModPayload(PacketBase packet)
		{
			this.packet = packet;
		}

		@Override
		public Type<? extends CustomPacketPayload> type()
		{
			return TYPE;
		}
	}

	//The list of registered packets. Should contain no more than 256 packets.
	private final LinkedList<Class<? extends PacketBase>> packets = new LinkedList<>();
	private boolean modInitialised = false;

	/**
	 * Registers a packet with the handler
	 */
	public boolean registerPacket(Class<? extends PacketBase> cl)
	{
		if(packets.size() > 256)
		{
			FlansMod.log.warn("Packet limit exceeded in Flan's Mod packet handler by packet " + cl.getCanonicalName() + ".");
			return false;
		}
		if(packets.contains(cl))
		{
			FlansMod.log.warn("Tried to register " + cl.getCanonicalName() + " packet class twice.");
			return false;
		}
		if(modInitialised)
		{
			FlansMod.log.warn("Tried to register packet " + cl.getCanonicalName() + " after mod initialisation.");
			return false;
		}

		packets.add(cl);
		return true;
	}

	public static final StreamCodec<ByteBuf, FlansModPayload> CODEC = StreamCodec.of(
			PacketHandler::encode,
			PacketHandler::decode);

	private static void encode(ByteBuf buf, FlansModPayload payload)
	{
		PacketHandler handler = FlansMod.getPacketHandler();
		int id = handler.packets.indexOf(payload.packet.getClass());
		if(id < 0)
			throw new NullPointerException("Packet not registered : " + payload.packet.getClass().getCanonicalName());
		buf.writeShort(id);
		payload.packet.encodeInto(buf);
	}

	private static FlansModPayload decode(ByteBuf buf)
	{
		PacketHandler handler = FlansMod.getPacketHandler();
		int id = buf.readShort();
		Class<? extends PacketBase> clazz = id >= 0 && id < handler.packets.size() ? handler.packets.get(id) : null;
		if(clazz == null)
			throw new NullPointerException("Packet not registered for discriminator : " + id);
		try
		{
			PacketBase packet = clazz.getDeclaredConstructor().newInstance();
			packet.decodeInto(buf.readSlice(buf.readableBytes()));
			return new FlansModPayload(packet);
		}
		catch(Exception e)
		{
			FlansMod.log.error("ERROR decoding packet");
			FlansMod.log.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialisation method called from FlansMod
	 */
	public void initialise()
	{
		registerPacket(PacketAAGunAngles.class);
		registerPacket(PacketBaseEdit.class);
		registerPacket(PacketBreakSound.class);
		registerPacket(PacketBuyArmour.class);
		registerPacket(PacketBuyWeapon.class);
		registerPacket(PacketCraftDriveable.class);
		registerPacket(PacketDriveableControl.class);
		registerPacket(PacketDriveableDamage.class);
		registerPacket(PacketDriveableGUI.class);
		registerPacket(PacketDriveableKey.class);
		registerPacket(PacketDriveableKeyHeld.class);
		registerPacket(PacketFlak.class);
		registerPacket(PacketGunFire.class);
		registerPacket(PacketGunPaint.class);
		registerPacket(PacketKillMessage.class);
		registerPacket(PacketMechaControl.class);
		registerPacket(PacketMGFire.class);
		registerPacket(PacketMGMount.class);
		registerPacket(PacketPlaneControl.class);
		registerPacket(PacketPlaySound.class);
		registerPacket(PacketReload.class);
		registerPacket(PacketRepairDriveable.class);
		registerPacket(PacketRoundFinished.class);
		registerPacket(PacketSeatUpdates.class);
		registerPacket(PacketTeamInfo.class);
		registerPacket(PacketTeamSelect.class);
		registerPacket(PacketVehicleControl.class);
		registerPacket(PacketVoteCast.class);
		registerPacket(PacketVoting.class);
		registerPacket(PacketRequestDebug.class);
		registerPacket(PacketLoadoutData.class);
		registerPacket(PacketOpenRewardBox.class);
		registerPacket(PacketAddSingleRewardBoxInstance.class);
		registerPacket(PacketGunAnimation.class);
		registerPacket(PacketBulletTrail.class);
		registerPacket(PacketHitMarker.class);
		registerPacket(PacketBlockHitEffect.class);

		modInitialised = true;
		//Sort the packets to ensure a matching ordering on client and server
		packets.sort((c1, c2) ->
		{
			int com = String.CASE_INSENSITIVE_ORDER.compare(c1.getCanonicalName(), c2.getCanonicalName());
			if(com == 0)
				com = c1.getCanonicalName().compareTo(c2.getCanonicalName());
			return com;
		});

		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) ->
				context.server().execute(() -> payload.packet.handleServerSide(context.player())));
	}

	/**
	 * Client side registration lives in com.flansmod.client.network.
	 * ClientPacketRegistration (called from the client initialiser), so the
	 * common handler never references client-only classes.
	 */

	/**
	 * Send a packet to all players
	 */
	public void sendToAll(PacketBase packet)
	{
		for(ServerPlayer player : FlansMod.serverInstance.getPlayerList().getPlayers())
		{
			ServerPlayNetworking.send(player, new FlansModPayload(packet));
		}
	}

	/**
	 * Send a packet to a player
	 */
	public void sendTo(PacketBase packet, ServerPlayer player)
	{
		ServerPlayNetworking.send(player, new FlansModPayload(packet));
	}

	private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> getDimensionKey(int dimensionID)
	{
		switch(dimensionID)
		{
			case -1: return net.minecraft.world.level.Level.NETHER;
			case 1: return net.minecraft.world.level.Level.END;
			default: return net.minecraft.world.level.Level.OVERWORLD;
		}
	}

	/**
	 * Send a packet to all around a point
	 */
	public void sendToAllAround(PacketBase packet, double x, double y, double z, float range, int dimension)
	{
		double rangeSq = range * range;
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = getDimensionKey(dimension);
		for(ServerPlayer player : FlansMod.serverInstance.getPlayerList().getPlayers())
		{
			if(player.level().dimension().equals(dim))
			{
				double dx = player.getX() - x;
				double dy = player.getY() - y;
				double dz = player.getZ() - z;
				if(dx * dx + dy * dy + dz * dz < rangeSq)
					ServerPlayNetworking.send(player, new FlansModPayload(packet));
			}
		}
	}

	/**
	 * Send a packet to all in a dimension
	 */
	public void sendToDimension(PacketBase packet, int dimensionID)
	{
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = getDimensionKey(dimensionID);
		for(ServerPlayer player : FlansMod.serverInstance.getPlayerList().getPlayers())
		{
			if(player.level().dimension().equals(dim))
				ServerPlayNetworking.send(player, new FlansModPayload(packet));
		}
	}

	/**
	 * Send a packet to the server
	 */
	public void sendToServer(PacketBase packet)
	{
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new FlansModPayload(packet));
	}

	/**
	 * Send a packet to all around a point without having to create one's own TargetPoint
	 */
	public void sendToAllAround(PacketBase packet, double x, double y, double z, float range)
	{
		sendToAllAround(packet, x, y, z, range, 0);
	}
}
