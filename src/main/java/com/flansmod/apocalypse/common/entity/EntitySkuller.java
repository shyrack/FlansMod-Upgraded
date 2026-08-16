package com.flansmod.apocalypse.common.entity;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.guns.ItemGun;

import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.ModEntities;

public class EntitySkuller extends Shulker 
{
	protected Level world;
	public ItemStack gunStack;
	public ItemStack ammoStack;
	
	public EntitySkuller(EntityType<? extends Shulker> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntitySkuller(Level worldIn)
	{
		this(ModEntities.SKULLER, worldIn);
	}
	
	public static AttributeSupplier.Builder createAttributes()
	{
		return Shulker.createAttributes();
	}
	
	public void AssignRandomGun()
	{
		gunStack = FlansModApocalypse.lootGenerator.getRandomLoadedGun(new java.util.Random(), false);
		ammoStack = new ItemStack(((ItemGun)gunStack.getItem()).GetType().nonExplosiveAmmo.get(0).item);
	}

    @Override
    protected void registerGoals()
    {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, true));
    }
}
