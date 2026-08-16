package com.flansmod.common.parts;

import com.flansmod.common.ModItems;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class ItemPart extends Item implements IFlanItem
{
	public PartType type;
	
	public ItemPart(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemPart(PartType type1)
	{
		this(new Item.Properties().stacksTo(type1.stackSize).setId(ModItems.itemKey(type1)));
		type = type1;
		type.item = this;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type != null && type.category == EnumPartCategory.FUEL)
		{
			tooltip.accept(Component.literal("Fuel Stored: " + (type.fuel - stack.getDamageValue()) + " / " + type.fuel));
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
