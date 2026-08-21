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
		//26.1.2 caches the dimensions from the EntityType in the constructor,
		//so the 0.5x0.5 override must be applied explicitly
		refreshDimensions();
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
	
	/**
	 * Wheels are 0.5x0.5 cubes like in 1.12.2 (the default 0.6x1.8 entity box
	 * made the resting plane sit ~0.9 blocks too low: model wheels buried in
	 * the ground and the tail-dragger stance collapsed).
	 */
	@Override
	public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose)
	{
		return net.minecraft.world.entity.EntityDimensions.scalable(0.5F, 0.5F);
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
		ID = tags.getIntOr("Index", 0);
		entityData.set(WHEEL, ID);
		
		//The driveable may not be attached yet when the wheel loads (the
		//passenger chain is restored after load), so resolve it lazily in tick()
		//rather than killing the wheel here.
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		CompoundTag tags = new CompoundTag();
		EntityDriveable saveVehicle = vehicle;
		if(saveVehicle == null && getVehicle() instanceof EntityDriveable)
			saveVehicle = (EntityDriveable)getVehicle();
		if(saveVehicle != null && saveVehicle.getDriveableType() != null)
		{
			tags.putString("DriveableType", saveVehicle.getDriveableType().shortName);
			tags.putInt("Index", ID);
		}
		output.store("FlanData", net.minecraft.nbt.CompoundTag.CODEC, tags);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		if(vehicle == null && getVehicle() instanceof EntityDriveable)
		{
			vehicle = (EntityDriveable)getVehicle();
			vehicle.registerWheel(this);
			entityData.set(VEHICLE, vehicle.getId());
		}
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
	public void rideTick()
	{
		// Wheels are positioned manually by the driveable's spring physics
		// (EntityPlane.tick / EntityVehicle.tick). Vanilla rideTick would
		// additionally call getVehicle().positionRider(this), teleporting the
		// wheel to the driveable's passenger attachment point (0, 1.8, 0)
		// every tick and destroying ground contact. See EntitySeat.rideTick().
		if(vehicle != null || getVehicle() != null)
			tick();
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
