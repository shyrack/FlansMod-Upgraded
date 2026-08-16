package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.common.ModEntities;

public class EntityFakePlayer extends EntityFlansModShooter
{
	private Container inventory;
	
	public EntityFakePlayer(EntityType<? extends EntityFlansModShooter> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntityFakePlayer(Level world)
	{
		this(ModEntities.FAKE_PLAYER, world);
		this.world = level();
	}
	
	public EntityFakePlayer(Level world, Player player)
	{
		this(world);
		
		setPos(player.getX(), player.getY(), player.getZ());
		
		//Copy the existing player's inventory
		inventory = new SimpleContainer(player.getInventory().getContainerSize());
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			inventory.setItem(i, player.getInventory().getItem(i).copy());
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean maybeBlock)
	{
		if(!world.isClientSide())
		{
			for(int j = 0; j < inventory.getContainerSize(); j++)
			{
				ItemStack stack = inventory.getItem(j);
				if(stack != null && !stack.isEmpty())
					world.addFreshEntity(new ItemEntity(world, getX(), getY(), getZ(), stack));
			}
		}
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
		if(inventory == null)
			inventory = new SimpleContainer(40);
		for(int i = 0; i < 40; i++)
		{
			String itemName = input.getStringOr("S" + i + "Item", "");
			if(!itemName.isEmpty())
			{
				Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
				inventory.setItem(i, new ItemStack(item, input.getIntOr("S" + i + "Count", 1)));
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output)
	{
		for(int i = 0; i < 40; i++)
		{
			ItemStack stack = inventory.getItem(i);
			if(stack != null && !stack.isEmpty())
			{
				output.putString("S" + i + "Item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
				output.putInt("S" + i + "Count", stack.getCount());
				// TODO APOCALYPSE: item NBT components are not saved
			}
		}
	}
}
