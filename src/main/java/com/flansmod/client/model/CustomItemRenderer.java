package com.flansmod.client.model;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public interface CustomItemRenderer
{
	void renderItem(CustomItemRenderType type, InteractionHand hand, ItemStack item, Object... data);
}
