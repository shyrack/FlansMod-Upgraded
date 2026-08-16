package com.flansmod.client.debug;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.flansmod.common.ModEntities;
import com.flansmod.common.vector.Vector3f;

/**
 * Entity for debugging purposes.
 * On the client side a line (Vector) between the position of the entity and its pointing location is rendered
 */
public class EntityDebugVector extends EntityDebugColor
{
	
	private static final EntityDataAccessor<Float> POINTING_X = SynchedEntityData.defineId(EntityDebugVector.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> POINTING_Y = SynchedEntityData.defineId(EntityDebugVector.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> POINTING_Z = SynchedEntityData.defineId(EntityDebugVector.class, EntityDataSerializers.FLOAT);
	
	public int life = 1000;
	
	/**
	 * @param w Level for Entity Constructor
	 */
		public EntityDebugVector(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityDebugVector(Level w)
	{
		this(ModEntities.DEBUG_VECTOR, w);

	}
	
	/**
	 * Spawns an EntityDebug Vector
	 *
	 * @param w Level for Entity Constructor
	 * @param u Position where the Vector starts
	 * @param v Position where the Vector ends
	 * @param i Lifetime given in ticks
	 * @param r Red Color Value
	 * @param g Green Color Value
	 * @param b Blue Color Value
	 */
	public EntityDebugVector(Level w, Vector3f u, Vector3f v, int i, float r, float g, float b)
	{
		this(w);
		setPos(u.x, u.y, u.z);
		setPointing(v.x, v.y, v.z);
		setColor(r, g, b);
		life = i;
	}
	
	/**
	 * @param w Level for Entity Constructor
	 * @param u Position where the Vector starts
	 * @param v Position where the Vector ends
	 * @param i Lifetime given in ticks
	 */
	public EntityDebugVector(Level w, Vector3f u, Vector3f v, int i)
	{
		this(w, u, v, i, 1F, 1F, 1F);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		life--;
		if(life <= 0)
			discard();
	}
	
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(POINTING_X, 1F);
		builder.define(POINTING_Y, 1F);
		builder.define(POINTING_Z, 1F);
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput nbttagcompound)
	{
		super.readAdditionalSaveData(nbttagcompound);
		this.entityData.set(POINTING_X, nbttagcompound.getFloatOr("pointing_x", 0F));
		this.entityData.set(POINTING_Y, nbttagcompound.getFloatOr("pointing_y", 0F));
		this.entityData.set(POINTING_Z, nbttagcompound.getFloatOr("pointing_z", 0F));
	}
	
	@Override
	protected void addAdditionalSaveData(ValueOutput nbttagcompound)
	{
		super.addAdditionalSaveData(nbttagcompound);
		nbttagcompound.putFloat("pointing_x", getPointingX());
		nbttagcompound.putFloat("pointing_y", getPointingY());
		nbttagcompound.putFloat("pointing_z", getPointingZ());
	}
	
	/**
	 * @param x The X value of the Position the Vector points to (Relative to Entity Position)
	 */
	public void setPointingX(Float x)
	{
		this.entityData.set(POINTING_X, x);
	}
	
	/**
	 * @return The X value of the Position the Vector points to (Relative to Entity Position)
	 */
	public Float getPointingX()
	{
		return this.entityData.get(POINTING_X);
	}
	
	/**
	 * @param y The Y value of the Position the Vector points to (Relative to Entity Position)
	 */
	public void setPointingY(Float y)
	{
		this.entityData.set(POINTING_Y, y);
	}
	
	/**
	 * @return The Y value of the Position the Vector points to (Relative to Entity Position)
	 */
	public Float getPointingY()
	{
		return this.entityData.get(POINTING_Y);
	}
	
	/**
	 * @param z The Z value of the Position the Vector points to (Relative to Entity Position)
	 */
	public void setPointingZ(Float z)
	{
		this.entityData.set(POINTING_Z, z);
	}
	
	/**
	 * @return The Z value of the Position the Vector points to (Relative to Entity Position)
	 */
	public Float getPointingZ()
	{
		return this.entityData.get(POINTING_Z);
	}
	
	/**
	 * All Parameters are relative to the position of the Entity.
	 * These 3 Parameters describe the location of the Position the Vector points to.
	 *
	 * @param x The X Coordinate
	 * @param y The Y Coordinate
	 * @param z The Z Coordinate
	 */
	public void setPointing(Float x, Float y, Float z)
	{
		setPointingX(x);
		setPointingY(y);
		setPointingZ(z);
	}
}
