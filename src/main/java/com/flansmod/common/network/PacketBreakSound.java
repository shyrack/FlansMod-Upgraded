package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;

public class PacketBreakSound extends PacketBase
{
	public int x, y, z;
	public int blockID;
	
	public PacketBreakSound()
	{
	}
	
	public PacketBreakSound(int x, int y, int z, Block block)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		blockID = BuiltInRegistries.BLOCK.getId(block);
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeInt(x);
		data.writeInt(y);
		data.writeInt(z);
		data.writeInt(blockID);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		x = data.readInt();
		y = data.readInt();
		z = data.readInt();
		blockID = data.readInt();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received block break sound packet on server. Skipping.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		Level world = clientPlayer.level();
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = world.getBlockState(pos);
		
		SoundType sound = state.getSoundType();
		SoundEvent event = sound.getBreakSound();
		world.playSound(clientPlayer, x + 0.5F, y + 0.5F, z + 0.5F, event, SoundSource.BLOCKS,
				(sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
	}
}
