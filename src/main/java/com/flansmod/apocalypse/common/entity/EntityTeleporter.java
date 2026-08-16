package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.world.TeleporterApocalypse;
import com.flansmod.common.ModEntities;

public class EntityTeleporter extends Entity
{
	protected Level world;
	/**
	 * Points to the lower left power cube in the portal frame
	 */
	private BlockPos lowerLeftCornerPowerCube;
	/**
	 * Points to the lower left power cube in the target teleporter
	 */
	private BlockPos targetTeleporter;
	
	public EntityTeleporter(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityTeleporter(Level world)
	{
		this(ModEntities.TELEPORTER, world);
		this.world = level();

	}
	
	public EntityTeleporter(Level world, BlockPos pos)
	{
		this(world);
		this.lowerLeftCornerPowerCube = pos;
		this.setPos(pos.getX() + 2D, pos.getY(), pos.getZ() + 2D);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(lowerLeftCornerPowerCube == null)
		{
			lowerLeftCornerPowerCube = new BlockPos(Mth.floor(getX() - 1.5D), Mth.floor(getY() + 0.5D), Mth.floor(getZ() - 1.5D));
		}

		if(!world.isClientSide())
			for(int i = 0; i < 2; i++)
				for(int j = 0; j < 2; j++)
					if(world.getBlockState(lowerLeftCornerPowerCube.offset(3 * i, 0, 3 * j)).getBlock() != FlansModApocalypse.blockPowerCube)
					{
						discard();
					}
		
		for(int i = 0; i < 10; i++)
		{
			double dX = random.nextGaussian();
			double dY = random.nextGaussian();
			double dZ = random.nextGaussian();
			if(world.isClientSide())
				world.addParticle(ParticleTypes.PORTAL, getX() + dX, getY() + 1 + dY, getZ() + dZ, dX, dY, dZ);
			else
				((ServerLevel)world).sendParticles(ParticleTypes.PORTAL, getX() + dX, getY() + 1 + dY, getZ() + dZ, 1, 0D, 0D, 0D, 0D);
		}
	}
	
	@Override
	public void playerTouch(Player player)
	{
		if(!world.isClientSide())
		{
			if(world.getBlockState(lowerLeftCornerPowerCube).getBlock() != FlansModApocalypse.blockPowerCube
			|| world.getBlockState(lowerLeftCornerPowerCube.offset(3,0,0)).getBlock() != FlansModApocalypse.blockPowerCube
			|| world.getBlockState(lowerLeftCornerPowerCube.offset(3,0,3)).getBlock() != FlansModApocalypse.blockPowerCube
			|| world.getBlockState(lowerLeftCornerPowerCube.offset(0,0,3)).getBlock() != FlansModApocalypse.blockPowerCube)
			{
				discard();
				return;
			}
			
			if(targetTeleporter == null)
				findPortal(player);
			
			if(targetTeleporter != null && !player.isOnPortalCooldown())
			{
				player.setPortalCooldown(200);
				ServerLevel serverLevel = (ServerLevel)world;
				//Switch between overworld and apocalypse
				if(world.dimension() == Level.OVERWORLD)
				{
					ServerLevel apocLevel = serverLevel.getServer().getLevel(FlansModApocalypse.APOCALYPSE_DIMENSION_KEY);
					if(apocLevel != null)
						((ServerPlayer)player).teleport(new TeleportTransition(apocLevel, new Vec3(targetTeleporter.getX() + 2D, targetTeleporter.getY() + 1.5D, targetTeleporter.getZ() + 2D), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
				}
				else
				{
					ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
					if(overworld != null)
						((ServerPlayer)player).teleport(new TeleportTransition(overworld, new Vec3(targetTeleporter.getX() + 2D, targetTeleporter.getY() + 1.5D, targetTeleporter.getZ() + 2D), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
				}
			}
		}
		
	}
	
	private void findPortal(Player player)
	{
		if(world.dimension() == FlansModApocalypse.APOCALYPSE_DIMENSION_KEY)
		{
			BlockPos entryPoint = FlansModApocalypse.proxy.data.entryPoints.get(player.getUUID());
	
			//Find a valid place to enter the world
			ServerLevel overworld = ((ServerLevel)world).getServer().getLevel(Level.OVERWORLD);
			if(overworld == null)
				return;
			
			// Map their apoc pos to overworld 1:1
			if(entryPoint == null)
				entryPoint = player.blockPosition();

			for(int j = 0; j < 300; j++)
			{
				double i = overworld.getRandom().nextDouble() * Math.PI * 2d;
				double dX = Math.cos(i) * FlansModApocalypse.RETURN_RADIUS;
				double dZ = Math.sin(i) * FlansModApocalypse.RETURN_RADIUS;
				
				BlockPos pos = new BlockPos(Mth.floor(entryPoint.getX() + dX), 256, Mth.floor(entryPoint.getZ() + dZ));
				for(; pos.getY() >= 0; pos = pos.below())
				{
					if(overworld.isEmptyBlock(pos) && overworld.getBlockState(pos.below()).isSolid())
					{
						if(overworld.getWorldBorder().isWithinBounds(pos) && overworld.getWorldBorder().isWithinBounds(pos.offset(3, 0, 3)))
						{
							//We have found a valid position
							if(createPortal(overworld, pos))
							{
								targetTeleporter = pos;
								return;
							}
						}
					}
				}
			}
		}
		else
		{
			ServerLevel apocWorld = ((ServerLevel)world).getServer().getLevel(FlansModApocalypse.APOCALYPSE_DIMENSION_KEY);
			if(apocWorld == null)
				return;
			for(int j = 0; j < 300; j++)
			{
				double i = apocWorld.getRandom().nextDouble() * Math.PI * 2d;
				double dX = Math.cos(i) * FlansModApocalypse.RETURN_RADIUS;
				double dZ = Math.sin(i) * FlansModApocalypse.RETURN_RADIUS;
				
				BlockPos pos = new BlockPos(Mth.floor(getX() + dX), 256, Mth.floor(getZ() + dZ));
				for(; pos.getY() >= 0; pos = pos.below())
				{
					if(apocWorld.isEmptyBlock(pos) && apocWorld.getBlockState(pos.below()).isSolid())
					{
						if(apocWorld.getWorldBorder().isWithinBounds(pos) && apocWorld.getWorldBorder().isWithinBounds(pos.offset(3, 0, 3)))
						{
							//We have found a valid position
							if(createPortal(apocWorld, pos))
							{
								targetTeleporter = pos;
								return;
							}
						}
					}
				}
			}
		}
		
	}
	
	private boolean createPortal(ServerLevel otherWorld, BlockPos pos)
	{
		//If there isn't enough space, reject this spot
		for(int i = 0; i < 4; i++)
		{
			for(int j = 0; j < 3; j++)
			{
				for(int k = 0; k < 4; k++)
				{
					if(otherWorld.getBlockState(pos.offset(i, j, k)).getBlock() != Blocks.AIR)
						return false;
				}
			}
		}
		//Create portal
		for(int i = 0; i < 2; i++)
		{
			for(int j = 0; j < 2; j++)
			{
				otherWorld.setBlockAndUpdate(pos.offset(i * 3, -1, j * 3), Blocks.OBSIDIAN.defaultBlockState());
				otherWorld.setBlockAndUpdate(pos.offset(i * 3, 0, j * 3), FlansModApocalypse.blockPowerCube.defaultBlockState());
				otherWorld.setBlockAndUpdate(pos.offset(1 + i, -1, 1 + j), Blocks.OBSIDIAN.defaultBlockState());
			}
		}
		//Create obsidian base pillar to avoid floating portals
		for(int i = 0; i < 4; i++)
		{
			for(int k = 0; k < 4; k++)
			{
				for(int j = -1; j >= 1 && otherWorld.getBlockState(pos.offset(i, j, k)).getBlock() == Blocks.AIR; j--)
				{
					otherWorld.setBlockAndUpdate(pos.offset(i, j, k), Blocks.OBSIDIAN.defaultBlockState());
				}
			}
		}
		EntityTeleporter teleporter = new EntityTeleporter(otherWorld, pos);
		teleporter.targetTeleporter = new BlockPos(lowerLeftCornerPowerCube);
		otherWorld.addFreshEntity(teleporter);
		
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{

	}

	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
	{
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
		lowerLeftCornerPowerCube = new BlockPos(input.getIntOr("X", 0), input.getIntOr("Y", 0), input.getIntOr("Z", 0));
		this.setPos(lowerLeftCornerPowerCube.getX() + 2D, lowerLeftCornerPowerCube.getY() + 1D, lowerLeftCornerPowerCube.getZ() + 2D);
		int targetX = input.getIntOr("targetX", Integer.MIN_VALUE);
		if(targetX != Integer.MIN_VALUE)
			targetTeleporter = new BlockPos(targetX, input.getIntOr("targetY", 0), input.getIntOr("targetZ", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output)
	{
		output.putInt("X", lowerLeftCornerPowerCube.getX());
		output.putInt("Y", lowerLeftCornerPowerCube.getY());
		output.putInt("Z", lowerLeftCornerPowerCube.getZ());
		if(targetTeleporter != null)
		{
			output.putInt("targetX", targetTeleporter.getX());
			output.putInt("targetY", targetTeleporter.getY());
			output.putInt("targetZ", targetTeleporter.getZ());
		}
	}

}
