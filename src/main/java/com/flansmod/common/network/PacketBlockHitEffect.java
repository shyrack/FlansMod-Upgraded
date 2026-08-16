package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.vector.Vector3f;

public class PacketBlockHitEffect extends PacketBase
{
	private Float x;
	private Float y;
	private Float z;
	
	private Float directionX;
	private Float directionY;
	private Float directionZ;
	
	private Integer blockX;
	private Integer blockY;
	private Integer blockZ;
	
	private Direction facing;
	
	public PacketBlockHitEffect() {
		//default constructor
	}
	
	public PacketBlockHitEffect(Vector3f hit, Vector3f direction, BlockPos position, Direction facing)
	{
		this(hit.x, hit.y, hit.z, direction.x, direction.y, direction.z, position.getX(), position.getY(), position.getZ(), facing);
	}
	
	public PacketBlockHitEffect(Float x, Float y, Float z, Float directionX, Float directionY, Float directionZ, Integer blockX, Integer blockY, Integer blockZ, Direction facing)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.directionX = directionX;
		this.directionY = directionY;
		this.directionZ = directionZ;
		
		this.blockX = blockX;
		this.blockY = blockY;
		this.blockZ = blockZ;
		
		this.facing = facing;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeFloat(x);
		data.writeFloat(y);
		data.writeFloat(z);
		
		data.writeFloat(directionX);
		data.writeFloat(directionY);
		data.writeFloat(directionZ);
		
		data.writeInt(blockX);
		data.writeInt(blockY);
		data.writeInt(blockZ);
		
		data.writeInt(facing.get3DDataValue());
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		x = data.readFloat();
		y = data.readFloat();
		z = data.readFloat();
		
		directionX = data.readFloat();
		directionY = data.readFloat();
		directionZ = data.readFloat();
		
		blockX = data.readInt();
		blockY = data.readInt();
		blockZ = data.readInt();
		
		facing = Direction.from3DDataValue(data.readInt());
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		FlansMod.log.warn("Received Packet packet on client. Skipping.");
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		Level world = clientPlayer.level();
		BlockPos pos = new BlockPos(blockX, blockY, blockZ);
		BlockState state = world.getBlockState(pos);
		Vec3i facingDir = facing.getUnitVec3i();
		
		for(int i = 0; i < 2; i++)
		{
			// TODO: [1.12] Check why this isn't moving right
			float scale = (float)world.getRandom().nextGaussian() * 0.1f + 0.5f;
			
			double motionX = (double)facingDir.getX() * scale + world.getRandom().nextGaussian() * 0.025d;
			double motionY = (double)facingDir.getY() * scale + world.getRandom().nextGaussian() * 0.025d;
			double motionZ = (double)facingDir.getZ() * scale + world.getRandom().nextGaussian() * 0.025d;
			
			motionX += directionX;
			motionY += directionY;
			motionZ += directionZ;
			
			world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
					x, y, z, motionX, motionY, motionZ);
		}
		
		double scale = world.getRandom().nextGaussian() * 0.05d + 0.05d;
		double motionX = (double)facingDir.getX() * scale + world.getRandom().nextGaussian() * 0.025d;
		double motionY = (double)facingDir.getY() * scale + world.getRandom().nextGaussian() * 0.025d;
		double motionZ = (double)facingDir.getZ() * scale + world.getRandom().nextGaussian() * 0.025d;

		world.addParticle(ParticleTypes.CLOUD, x, y, z, motionX, motionY, motionZ);
	}
}
