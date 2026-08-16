package com.flansmod.client.debug;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Class Skeleton for DebugEntities which use a color
 */
public abstract class EntityDebugColor extends Entity
{
	protected Level world;

	private static final EntityDataAccessor<Float> COLOR_RED = SynchedEntityData.defineId(EntityDebugColor.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> COLOR_GREEN = SynchedEntityData.defineId(EntityDebugColor.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> COLOR_BLUE = SynchedEntityData.defineId(EntityDebugColor.class, EntityDataSerializers.FLOAT);
	
	/**
	 * @param w Level for Entity Constructor
	 */
	public EntityDebugColor(EntityType<?> type, Level w)
	{
		super(type, w);
		this.world = level();
	}
	
	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
	{
		return false;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(COLOR_RED, 1F);
		builder.define(COLOR_GREEN, 1F);
		builder.define(COLOR_BLUE, 1F);
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput nbttagcompound)
	{
		this.setColorRed(nbttagcompound.getFloatOr("color_red", 0F));
		this.setColorGreen(nbttagcompound.getFloatOr("color_green", 0F));
		this.setColorBlue(nbttagcompound.getFloatOr("color_blue", 0F));
	}
	
	@Override
	protected void addAdditionalSaveData(ValueOutput nbttagcompound)
	{
		nbttagcompound.putFloat("color_red", getColorRed());
		nbttagcompound.putFloat("color_green", getColorGreen());
		nbttagcompound.putFloat("color_blue", getColorBlue());
	}
	
	/**
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @param red Red color value
	 */
	public void setColorRed(Float red)
	{
		this.entityData.set(COLOR_RED, red);
	}
	
	/**
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @return Red color value
	 */
	public Float getColorRed()
	{
		return this.entityData.get(COLOR_RED);
	}
	
	/**
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @param green Green color value
	 */
	public void setColorGreen(Float green)
	{
		this.entityData.set(COLOR_GREEN, green);
	}
	
	/**
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @return Green color value
	 */
	public Float getColorGreen()
	{
		return this.entityData.get(COLOR_GREEN);
	}
	
	/**
	 * Color values range from 0 (Nonexistend) to 1 (Fully Visible)
	 *
	 * @param blue Blue color value
	 */
	public void setColorBlue(Float blue)
	{
		this.entityData.set(COLOR_BLUE, blue);
	}
	
	/**
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @return Blue color value
	 */
	public Float getColorBlue()
	{
		return this.entityData.get(COLOR_BLUE);
	}
	
	/**
	 * Combined Setter for all three color values
	 * Color values range from 0 (Nonexistent) to 1 (Fully Visible)
	 *
	 * @param red   Red color value
	 * @param green Green color value
	 * @param blue  Blue color value
	 */
	public void setColor(Float red, Float green, Float blue)
	{
		setColorRed(red);
		setColorGreen(green);
		setColorBlue(blue);
	}
}
