package com.flansmod.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityItemCustomRender extends ItemEntity
{
	protected Level world;
	public EntityItemCustomRender(Entity entity, ItemStack itemStack)
	{
		super(entity.level(), entity.getX(), entity.getY(), entity.getZ(), itemStack);
		this.world = entity.level();
		this.setDeltaMovement(entity.getDeltaMovement());
		this.setPickUpDelay(40);
	}
	
	public EntityItemCustomRender(Level world, double posX, double posY, double posZ, ItemStack stack)
	{
		super(world, posX, posY, posZ, stack);
		this.world = world;
	}
	
		public EntityItemCustomRender(EntityType<? extends ItemEntity> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityItemCustomRender(Level world)
	{
		this(ModEntities.CUSTOM_ITEM, world);
	}
	
	public EntityItemCustomRender(Level w, double x, double y, double z)
	{
		super(w, x, y, z, ItemStack.EMPTY);
		this.world = w;
	}
}
