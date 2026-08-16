package com.flansmod.common.driveables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.vector.Vector3f;

public class EntityWheel extends Entity
{
	/**
	 * The vehicle this wheel is part of
	 */
	public EntityDriveable vehicle;
	/**
	 * The ID of this wheel within the vehicle
	 */
	private int ID;
	
	/**
	 * The ID of the vehicle this wheel is part of, for client-server syncing
	 */
	private int vehicleID;
	
	/** The level this entity is in, mirrors the 1.12.2 world field */
	protected Level world;
	
	private static final EntityDataAccessor<Integer> VEHICLE =
		SynchedEntityData.defineId(EntityWheel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> WHEEL =
		SynchedEntityData.defineId(EntityWheel.class, EntityDataSerializers.INT);
	
	public EntityWheel(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityWheel(Level world)
	{
		this(ModEntities.WHEEL, world);
		this.world = level();
	}
	
	public EntityWheel(Level world, EntityDriveable entity, int i)
	{
		this(world);
		vehicle = entity;
		vehicleID = entity.getId();
		entityData.set(VEHICLE, vehicleID);
		ID = i;
		entityData.set(WHEEL, ID);
		
		initPosition();
	}
	
	public void initPosition()
	{
		Vector3f wheelVector =
				vehicle.axes.findLocalVectorGlobally(vehicle.getDriveableType().wheelPositions[ID].position);
		setPos(vehicle.getX() + wheelVector.x, vehicle.getY() + wheelVector.y, vehicle.getZ() + wheelVector.z);
		
		xo = getX();
		yo = getY();
		zo = getZ();
	}
	
	@Override
	public float maxUpStep()
	{
		return vehicle != null ? vehicle.getDriveableType().wheelStepHeight : 1.0F;
	}
	
	@Override
	public boolean causeFallDamage(double k, float l, DamageSource source)
	{
		if(vehicle == null || k <= 0)
			return false;
		int i = Mth.ceil(k - 3F);
		if(i > 0)
			vehicle.attackPart(vehicle.getDriveableType().wheelPositions[ID].part, level().damageSources().fall(), i);
		return true;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(VEHICLE, -1);
		builder.define(WHEEL, -1);
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
		CompoundTag tags = input.read("FlanData", net.minecraft.nbt.CompoundTag.CODEC).orElse(new CompoundTag());
		DriveableType type = DriveableType.getDriveable(tags.getStringOr("DriveableType", ""));
		ID = tags.getIntOr("Index", 0);
		entityData.set(WHEEL, ID);
		
		if(type == null)
		{
			FlansMod.log.warn("Killing wheel due to invalid type tag");
			reallySetDead();
			return;
		}
		
		if(getVehicle() instanceof EntityDriveable)
		{
			vehicle = (EntityDriveable)getVehicle();
			vehicle.registerWheel(this);
			entityData.set(VEHICLE, vehicle.getId());
		}
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		CompoundTag tags = new CompoundTag();
		if(vehicle != null)
		{
			tags.putString("DriveableType", vehicle.getDriveableType().shortName);
			tags.putInt("Index", ID);
		}
		output.store("FlanData", net.minecraft.nbt.CompoundTag.CODEC, tags);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		if(vehicle == null || isRemoved())
		{
			vehicleID = entityData.get(VEHICLE);
			if(vehicleID >= 0 && level().getEntity(vehicleID) instanceof EntityDriveable)
			{
				vehicle = (EntityDriveable)level().getEntity(vehicleID);
				vehicle.registerWheel(this);
			}
			if(vehicle == null)
			{
				return;
			}
		}
		
		ID = entityData.get(WHEEL);
	}
	
	@Override
	public boolean canBeCollidedWith(Entity other)
	{
		return !isRemoved();
	}
	
	public void reallySetDead()
	{
		discard();
	}
	
	public double getSpeedXZ()
	{
		return Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z);
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount)
	{
		return false;
	}
	
	public int getExpectedWheelID()
	{
		return ID;
	}
	
}
