package com.flansmod.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Helpers for the 26.1.2 item data component system, preserving the classic
 * "type tag on the item stack" behaviour of Flan's Mod.
 */
public class FlansModUtil
{
	public static CompoundTag getItemTag(ItemStack stack)
	{
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	public static void setItemTag(ItemStack stack, CompoundTag tag)
	{
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static CompoundTag getOrCreateItemTag(ItemStack stack)
	{
		return getItemTag(stack);
	}

	public static void updateItemTag(ItemStack stack, java.util.function.Consumer<CompoundTag> consumer)
	{
		CustomData.update(DataComponents.CUSTOM_DATA, stack, consumer);
	}
}
