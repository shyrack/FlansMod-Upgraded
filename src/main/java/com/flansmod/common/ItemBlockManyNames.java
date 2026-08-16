package com.flansmod.common;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemBlockManyNames extends BlockItem
{
	
	public ItemBlockManyNames(Block b)
	{
		super(b, new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.BuiltInRegistries.ITEM.key(),
				net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b))));
	}

	public ItemBlockManyNames(Block b, Item.Properties properties)
	{
		super(b, properties);
	}
}
