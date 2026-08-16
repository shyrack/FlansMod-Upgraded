package com.flansmod.common.guns;

import com.flansmod.common.ModItems;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.paintjob.IPaintableItem;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.types.InfoType;

public class ItemAttachment extends Item implements IPaintableItem
{
	public AttachmentType type;
	
	public ItemAttachment(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemAttachment(AttachmentType t)
	{
		super(new Item.Properties().stacksTo(t.maxStackSize).setId(ModItems.itemKey(t)));
		type = t;
		type.item = this;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type.description != null)
		{
			for(String line : type.description.split("_"))
				tooltip.accept(Component.literal(line));
		}
		
		if(type.shootDelayMultiplier != 1.0f)
			tooltip.accept(Component.literal("Rate of Fire x" + Mth.floor(100.0f / type.shootDelayMultiplier) + "%"));
		
		if(type.damageMultiplier != 1.0f)
			tooltip.accept(Component.literal("Damage x" + Mth.floor(type.damageMultiplier * 100.0f) + "%"));
	
		if(type.recoilMultiplier != 1.0f)
			tooltip.accept(Component.literal("Recoil x" + Mth.floor(type.recoilMultiplier * 100.0f) + "%"));

		if(type.spreadMultiplier != 1.0f)
			tooltip.accept(Component.literal("Bullet Spread x" + Mth.floor(type.spreadMultiplier * 100.0f) + "%"));
		
		if(type.reloadTimeMultiplier != 1.0f)
			tooltip.accept(Component.literal("Reload Time x" + Mth.floor(type.reloadTimeMultiplier * 100.0f) + "%"));
		
		if(type.bulletSpeedMultiplier != 1.0f)
			tooltip.accept(Component.literal("Projectile Speed x" + Mth.floor(type.bulletSpeedMultiplier * 100.0f) + "%"));
		
		if(type.silencer)
			tooltip.accept(Component.literal("Silenced"));
		
		if(type.meleeDamageMultiplier != 1.0f)
			tooltip.accept(Component.literal("Melee Damage x" + Mth.floor(type.meleeDamageMultiplier * 100.0f) + "%"));
		
		if(type.flashlight)
			tooltip.accept(Component.literal("Flashlight " + type.flashlightStrength));

	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
	
	@Override
	public PaintableType GetPaintableType()
	{
		return type;
	}
}
