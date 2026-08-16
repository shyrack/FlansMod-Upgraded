package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.ModEntities;

public class EntityNukeDrop extends Entity
{
	protected Level world;
	public static final int explosionLength = 500;
	public int timeSinceExplosion;
	
	public EntityNukeDrop(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityNukeDrop(Level world)
	{
		this(ModEntities.NUKE_DROP, world);
		this.world = level();

		noPhysics = false;
		// TODO APOCALYPSE: ignoreFrustumCheck no longer exists; nuke drop may cull when far away
	}
	
	public EntityNukeDrop(Level world, double x, double y, double z)
	{
		this(world);
		setPos(x, y, z);
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{

	}

	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
	{
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output)
	{

	}

	@Override
	public void tick()
	{
		super.tick();
		
		if(!onGround())
		{
			setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - 0.01D, getDeltaMovement().z);
			move(MoverType.SELF, getDeltaMovement());
		}
		else
		{
			timeSinceExplosion++;
			
			if(timeSinceExplosion > explosionLength)
				discard();
		}
		
		if(!world.isClientSide() && FlansModApocalypse.proxy.getApocalypseCountdown() <= 0)
			discard();
	}
}
