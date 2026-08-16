package com.flansmod.common.guns;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

/**
 * Implemented from old source.
 */
public class ItemBullet extends ItemShootable implements IFlanItem
{
	public BulletType type;
	
	public ItemBullet(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemBullet(BulletType infoType)
	{
		super(infoType);
		type = infoType;
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
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
}
