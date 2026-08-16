package com.flansmod.common.driveables.mechas;

import com.flansmod.common.ModItems;
import java.util.Collections;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class ItemMechaAddon extends Item implements IFlanItem
{
	public MechaItemType type;
	
	public ItemMechaAddon(Item.Properties properties)
	{
		super(properties.stacksTo(1));
	}
	
	public ItemMechaAddon(MechaItemType type1)
	{
		this(new Item.Properties().stacksTo(1).setId(ModItems.itemKey(type1)));
		type = type1;
		type.item = this;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type != null && type.description != null)
		{
			for(String line : type.description.split("_"))
			{
				tooltip.accept(Component.literal(line));
			}
		}
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}
}
