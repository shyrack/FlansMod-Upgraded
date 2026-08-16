package com.flansmod.common.guns;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class ItemGrenade extends ItemShootable implements IFlanItem
{
	public GrenadeType type;
	
	public ItemGrenade(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemGrenade(GrenadeType t)
	{
		super(t);
		type = t;
		type.item = this;
	}
	
	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		
		PlayerData data = PlayerHandler.getPlayerData(player);
		//If can throw grenade
		if(type.canThrow && data != null && data.shootTimeRight <= 0 && data.shootTimeLeft <= 0)
		{
			//Delay the next throw / weapon fire / whatnot
			data.shootTimeRight = type.throwDelay;
			//Create a new grenade entity
			EntityGrenade grenade = new EntityGrenade(player, this.type);
			//Spawn the entity server side
			if(!world.isClientSide())
				((ServerLevel)world).addFreshEntity(grenade);
			//If this can be remotely detonated, add it to the players detonate list
			if(type.remote)
				data.remoteExplosives.add(grenade);
			//Consume an item
			if(!player.getAbilities().instabuild)
				stack.setCount(stack.getCount() - 1);
			//Drop an item upon throwing if necessary
			if(type.dropItemOnThrow != null)
			{
				ItemStack dropStack = InfoType.getRecipeElement(type.dropItemOnDetonate);
				if(!world.isClientSide())
					((ServerLevel)world).addFreshEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), dropStack));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.FAIL;
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
	
	private EntityGrenade getGrenade(Level world, LivingEntity thrower)
	{
		//Create a new grenade entity
		EntityGrenade grenade = new EntityGrenade(thrower, type);
		//If this can be remotely detonated, add it to the players detonate list
		if(type.remote && thrower instanceof Player)
			PlayerHandler.getPlayerData((Player)thrower).remoteExplosives.add(grenade);
		return grenade;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type.description != null)
		{
			for(String line : type.description.split("_"))
				tooltip.accept(Component.literal(line));
		}
	}
	
	public void throwGrenade(Level world, LivingEntity thrower)
	{
		EntityGrenade grenade = getGrenade(world, thrower);
		if(!world.isClientSide())
			((ServerLevel)world).addFreshEntity(grenade);
	}
}
