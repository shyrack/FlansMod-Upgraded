package com.flansmod.common.network;

import java.util.Random;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;

public class PacketFlak extends PacketBase
{
	public static Random rand = new Random();
	
	/**
	 * Position of this flak
	 */
	public double x, y, z;
	/**
	 * Num particles
	 */
	public int numParticles;
	/**
	 * Particle type
	 */
	public String particleType;
	
	public PacketFlak()
	{
	}
	
	public PacketFlak(double x1, double y1, double z1, int n, String s)
	{
		x = x1;
		y = y1;
		z = z1;
		numParticles = n;
		particleType = s;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeDouble(x);
		data.writeDouble(y);
		data.writeDouble(z);
		data.writeInt(numParticles);
		writeUTF(data, particleType);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		x = data.readDouble();
		y = data.readDouble();
		z = data.readDouble();
		numParticles = data.readInt();
		particleType = readUTF(data);
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received flak packet on server. Disregarding.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		Level world = clientPlayer.level();
		ParticleOptions options = FlansMod.getParticleType(particleType);
		for(int i = 0; i < numParticles; i++)
		{
			world.addParticle(options, x + rand.nextGaussian(), y + rand.nextGaussian(), z + rand.nextGaussian(),
					rand.nextGaussian() / 20.0F, rand.nextGaussian() / 20.0F, rand.nextGaussian() / 20.0F);
		}
	}
}
