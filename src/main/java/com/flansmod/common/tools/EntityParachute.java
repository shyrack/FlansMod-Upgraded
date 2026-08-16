package com.flansmod.common.tools;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;

public class EntityParachute extends Entity
{
	public ToolType type;
	
	/** The level this entity is in, mirrors the 1.12.2 world field */
	protected Level world;
		public EntityParachute(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityParachute(Level w)
	{
		this(ModEntities.PARACHUTE, w);
		this.world = level();
		FlansMod.log.debug(w.isClientSide() ? "Client paraspawn" : "Server paraspawn");
	}
	
	public EntityParachute(Level w, ToolType t, Player player)
	{
		this(w);
		type = t;
		setPos(player.getX(), player.getY(), player.getZ());
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(!world.isClientSide() && (getControllingPassenger() == null || getControllingPassenger().getVehicle() != this))
		{
			discard();
		}
		
		if(getControllingPassenger() != null)
			getControllingPassenger().fallDistance = 0F;
		
		double motionX = getDeltaMovement().x;
		double motionY = -0.1D;
		double motionZ = getDeltaMovement().z;
		
		if(getControllingPassenger() != null && getControllingPassenger() instanceof LivingEntity)
		{
			float speedMultiplier = 0.002F;
			double moveForwards = ((LivingEntity)this.getControllingPassenger()).zza;
			double moveStrafing = ((LivingEntity)this.getControllingPassenger()).xxa;
			double sinYaw = -Math.sin((getControllingPassenger().getYRot() * (float)Math.PI / 180.0F));
			double cosYaw = Math.cos((this.getControllingPassenger().getYRot() * (float)Math.PI / 180.0F));
			motionX += (moveForwards * sinYaw + moveStrafing * cosYaw) * speedMultiplier;
			motionZ += (moveForwards * cosYaw - moveStrafing * sinYaw) * speedMultiplier;
			
			yRotO = getYRot();
			setYRot(getControllingPassenger().getYRot());
		}
		
		motionX *= 0.8F;
		motionZ *= 0.8F;
		
		move(MoverType.SELF, new Vec3(motionX, motionY, motionZ));
		
		if(onGround() || world.getFluidState(new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()))).is(net.minecraft.tags.FluidTags.WATER))
		{
			discard();
		}
	}
	
	@Override
	public boolean causeFallDamage(double par1, float k, DamageSource source)
	{
		//Ignore fall damage
		return false;
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float f)
	{
		discard();
		return true;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
	}
	
	@Override
	public LivingEntity getControllingPassenger()
	{
		List<Entity> list = this.getPassengers();
		return list.isEmpty() ? null : (LivingEntity)list.get(0);
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
		CompoundTag tags = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		type = ToolType.getType(tags.getStringOr("Type", ""));
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		CompoundTag tags = new CompoundTag();
		if(type != null)
			tags.putString("Type", type.shortName);
		output.store("FlanData", CompoundTag.CODEC, tags);
	}
	
	public ItemStack getPickedResult(HitResult target)
	{
		return new ItemStack(type.item);
	}
}
