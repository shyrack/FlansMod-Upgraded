package com.flansmod.common.enchantments;

import com.flansmod.common.ModItems;
import java.util.function.Consumer;

import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemGlove extends Item implements IFlanItem
{
	
	private GloveType mType;
	
	public ItemGlove(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemGlove(GloveType glove)
	{
		this(new Item.Properties().stacksTo(1).durability(glove.Durability).enchantable(glove.Enchantability).setId(ModItems.itemKey(glove)));
		mType = glove;	
		glove.item = this;
	}
	
	@Override
	public InfoType getInfoType() 
	{
		return mType;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		tooltip.accept(Component.literal("\u00a73Improves gun, sword or axe handling when enchanted and held in off hand"));
		tooltip.accept(Component.literal("\u00a73Works with two-handed guns"));
	}
}
