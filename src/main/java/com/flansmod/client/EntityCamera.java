package com.flansmod.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.ModEntities;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.vector.Vector3f;

public class EntityCamera extends Entity
{
	protected Level world;

	public EntityDriveable driveable;

	public EntityCamera(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityCamera(Level world)
	{
		this(ModEntities.CAMERA, world);
	}

	public EntityCamera(Level world, EntityDriveable d)
	{
		this(world);
		driveable = d;
		setPos(d.getX(), d.getY(), d.getZ());
	}

	@Override
	public void tick()
	{
		super.tick();

		xo = getX();
		yo = getY();
		zo = getZ();

		Vector3f cameraPosition = new Vector3f();
		cameraPosition = driveable.axes.findLocalVectorGlobally(cameraPosition);

		// Lerp it
		double dX = driveable.getX() + cameraPosition.x - getX();
		double dY = driveable.getY() + cameraPosition.y - getY();
		double dZ = driveable.getZ() + cameraPosition.z - getZ();

		float lerpAmount = 0.1F;

		setPos(getX() + dX * lerpAmount, getY() + dY * lerpAmount, getZ() + dZ * lerpAmount);

		setYRot(driveable.axes.getYaw() - 90);
		setXRot(driveable.axes.getPitch());

		while(getYRot() - yRotO >= 180F)
		{
			setYRot(getYRot() - 360F);
		}
		while(getYRot() - yRotO < -180F)
		{
			setYRot(getYRot() + 360F);
		}
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
	}

	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
	{
		return false;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
	{
	}
}
