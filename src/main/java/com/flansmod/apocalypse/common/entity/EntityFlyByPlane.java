package com.flansmod.apocalypse.common.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import com.flansmod.apocalypse.common.world.BiomeApocalypse;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.ModEntities;

public class EntityFlyByPlane extends EntityPlane
{
	public EntityFlyByPlane(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityFlyByPlane(Level world)
	{
		this(ModEntities.FLY_BY_PLANE, world);
		this.world = level();
	}
	
	public EntityFlyByPlane(Level world, double x, double y, double z, PlaneType type, DriveableData data)
	{
		super(world, x, y, z, type, data);
		this.world = level();
	}
	
	@Override
	public void tick()
	{
		throttle = 1F;
		
		super.tick();
	}
	
	private float getBiomeHeight(Holder<Biome> biome)
	{
		if(biome.is(BiomeApocalypse.DESERT_KEY))
			return 80F;
		else if(biome.is(BiomeApocalypse.DEEP_CANYON_KEY) || biome.is(BiomeApocalypse.SULPHUR_PITS_KEY))
			return 80F;
		else if(biome.is(BiomeApocalypse.HIGH_PLATEAU_KEY))
			return 120F;
		return 128F;
	}

	@Override
	public boolean canThrust()
	{
		return true;
	}
	
	@Override
	public boolean hasFuel()
	{
		return true;
	}

	@Override
	public boolean hasEnoughFuel()
	{
		return true;
	}
}
