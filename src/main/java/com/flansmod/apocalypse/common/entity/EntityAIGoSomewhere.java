package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class EntityAIGoSomewhere extends Goal
{
	private PathfinderMob theEntityCreature;
	protected double speed;
	private double directionX, directionZ;
	private double randPosX;
	private double randPosY;
	private double randPosZ;

	public EntityAIGoSomewhere(PathfinderMob creature, double speed, double dirX, double dirZ)
	{
		this.theEntityCreature = creature;
		this.speed = speed;

		this.directionX = dirX;
		this.directionZ = dirZ;
	}

	@Override
	public boolean canUse()
	{
		Vec3 vec3 = net.minecraft.world.entity.ai.util.RandomPos.generateRandomPos(theEntityCreature, () -> theEntityCreature.blockPosition().offset((int)directionX, 0, (int)directionZ));

		if(vec3 == null)
		{
			return false;
		}
		else
		{
			this.randPosX = vec3.x;
			this.randPosY = vec3.y;
			this.randPosZ = vec3.z;
			return true;
		}
	}

	@Override
	public void start()
	{
		this.theEntityCreature.getNavigation().moveTo(this.randPosX, this.randPosY, this.randPosZ, this.speed);
	}

	@Override
	public boolean canContinueToUse()
	{
		return !this.theEntityCreature.getNavigation().isDone();
	}
}
