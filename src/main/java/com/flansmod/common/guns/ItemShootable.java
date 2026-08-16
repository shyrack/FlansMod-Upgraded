package com.flansmod.common.guns;

import com.flansmod.common.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

public abstract class ItemShootable extends Item
{
	public ShootableType type;
	
	public ItemShootable(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemShootable(ShootableType t)
	{
		super(new Item.Properties().stacksTo(t.maxStackSize).component(DataComponents.MAX_DAMAGE, t.roundsPerItem).setId(ModItems.itemKey(t)));
		type = t;
	}
}
