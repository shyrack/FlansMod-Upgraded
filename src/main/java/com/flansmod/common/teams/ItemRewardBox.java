package com.flansmod.common.teams;

import com.flansmod.common.ModItems;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.FlansMod;

// Does nothing. Just here for rendering purposes
public class ItemRewardBox extends Item
{
	public RewardBox type;
	
	public ItemRewardBox(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemRewardBox(RewardBox box)
	{
		super(new Item.Properties().setId(ModItems.itemKey(box)));
		type = box;
		type.item = this;
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		tooltip.accept(Component.literal("Useless item. Never used outside of rank-based PVP"));
	}
}
