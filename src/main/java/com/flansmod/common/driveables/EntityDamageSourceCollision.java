package com.flansmod.common.driveables;

import net.minecraft.world.damagesource.DamageSource;

public class EntityDamageSourceCollision extends DamageSource
{
	public EntityDriveable source;
	
	public EntityDamageSourceCollision(EntityDriveable driveable)
	{
		super(driveable.level().damageSources().generic().typeHolder(), driveable);
		source = driveable;
	}
	
}
