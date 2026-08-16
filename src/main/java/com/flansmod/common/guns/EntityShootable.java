package com.flansmod.common.guns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class EntityShootable extends Entity
{
	protected Level world;

	public EntityShootable(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}
}
