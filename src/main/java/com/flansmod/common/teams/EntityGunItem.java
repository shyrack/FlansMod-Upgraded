package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.EntityItemCustomRender;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;

public class EntityGunItem extends EntityItemCustomRender
{
	
	public List<ItemStack> ammoStacks;
	private boolean teamsModEnabled = false;
	private int age;
	private int lifespan = 6000;
	
		public EntityGunItem(EntityType<? extends net.minecraft.world.entity.item.ItemEntity> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityGunItem(Level w)
	{
		this(ModEntities.GUN_ITEM, w);
		this.world = level();
	}
	
	public EntityGunItem(ItemEntity entity)
	{
		super(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity
				.getItem().copy());

		ammoStacks = new ArrayList<>();
	}
	
	public EntityGunItem(Level w, double x, double y, double z,
						 ItemStack stack, List<ItemStack> stacks)
	{
		super(w, x, y, z, stack);
		this.world = level();

		ammoStacks = new ArrayList<>();
		for(ItemStack ammoStack : stacks)
		{
			if(ammoStack != null && (ammoStack.getItem() instanceof ItemBullet))
				ammoStacks.add(ammoStack);
		}
		teamsModEnabled = true;
	}
	
	public EntityGunItem(Level w, double x, double y, double z)
	{
		super(w, x, y, z);
		this.world = level();
	}
	
	@Override
	public boolean isPickable()
	{
		return true;
	}
	
	@Override
	public void tick()
	{
		if(getItem().isEmpty() || !(getItem().getItem() instanceof ItemGun))
		{
			discard();
			return;
		}
		
		if(!world.isClientSide() && ammoStacks == null)
		{
			discard();
			return;
		}
		
		xo = getX();
		yo = getY();
		zo = getZ();
		setDeltaMovement(getDeltaMovement().subtract(0D, 0.03999999910593033D, 0D));
		
		float var2 = 0.98F;
		
		if(onGround())
		{
			var2 = 0.58800006F;
			BlockPos blockPos = new BlockPos(Mth.floor(getX()),
					Mth.floor(getBoundingBox().minY) - 1, Mth.floor(getZ()));
			BlockState blockState = world.getBlockState(blockPos);
			Block block = blockState.getBlock();
			
			if(!blockState.isAir())
			{
				var2 = block.getFriction() * 0.98F;
			}
		}
		
		Vec3 motion = getDeltaMovement();
		setDeltaMovement(motion.x * var2, motion.y * 0.9800000190734863D, motion.z * var2);
		
		if(onGround())
		{
			Vec3 currentMotion = getDeltaMovement();
			setDeltaMovement(currentMotion.x, currentMotion.y * -0.5D, currentMotion.z);
		}
		
		move(MoverType.SELF, getDeltaMovement());
		
		++age;
		
		ItemStack item = getItem();
		
		if(!world.isClientSide() && age >= lifespan)
		{
			if(!item.isEmpty())
			{
				discard();
			}
			else
			{
				discard();
			}
		}
		
		if(item.isEmpty())
		{
			discard();
		}
		
		// Temporary fire glitch fix
		if(world.isClientSide())
			extinguishFire();
	}
	
	@Override
	public void playerTouch(Player player)
	{
		if(!world.isClientSide())
		{
			if(ammoStacks != null && ammoStacks.size() > 0)
			{
				for(int i = 0; i < player.getInventory().getContainerSize(); i++)
				{
					ItemStack stack = player.getInventory().getItem(i);
					if(!stack.isEmpty() && stack.getItem() instanceof ItemGun)
					{
						GunType type = ((ItemGun)stack.getItem()).GetType();
						for(int j = ammoStacks.size() - 1; j >= 0; j--)
						{
							ItemStack ammoStack = ammoStacks.get(j);
							if(type.isCorrectAmmo(((ItemShootable)ammoStack.getItem()).type))
							{
								if(player.getInventory().add(ammoStack))
								{
									playSound(
											SoundEvents.ITEM_PICKUP,
											0.2F,
											((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
									ammoStacks.remove(j);
								}
							}
						}
						if(ammoStacks.isEmpty())
							discard();
					}
				}
			}
			else if(!teamsModEnabled)
			{
				super.playerTouch(player);
			}
		}
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 vec)
	{
		if(world.isClientSide())
			return InteractionResult.SUCCESS;
		if(!TeamsManager.getInstance().playerCanLoot(player, getItem()))
			return InteractionResult.PASS;
		ItemStack currentItem = player.getMainHandItem();
		if(!currentItem.isEmpty() && currentItem.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)currentItem.getItem()).GetType();
			List<ItemStack> newAmmoStacks = new ArrayList<>();
			for(int i = 0; i < player.getInventory().getContainerSize(); i++)
			{
				ItemStack stack = player.getInventory().getItem(i);
				if(!stack.isEmpty() && stack.getItem() instanceof ItemShootable)
				{
					ShootableType bulletType = ((ItemShootable)stack
							.getItem()).type;
					if(gunType.isCorrectAmmo(bulletType))
					{
						newAmmoStacks.add(stack.copy());
						player.getInventory().setItem(i, ItemStack.EMPTY.copy());
					}
				}
			}
			EntityGunItem newGunItem = new EntityGunItem(world, getX(),
					getY(), getZ(), currentItem.copy(), newAmmoStacks);
			if(world instanceof ServerLevel)
				((ServerLevel)world).addFreshEntity(newGunItem);
			player.getInventory().setItem(
					player.getInventory().getSelectedSlot(), getItem());
			for(ItemStack stack : ammoStacks)
			{
				player.getInventory().add(stack);
			}
			discard();
			PlayerHandler.getPlayerData(player).shootClickDelay = 10;
			PlayerHandler.getPlayerData(player).isShootingRight = false;
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
