package com.flansmod.common.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public class ItemStackUtil
{
	private static final RegistryAccess REGISTRY_ACCESS =
		RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
	
	public static void writeItemStack(CompoundTag tags, String name, ItemStack stack)
	{
		if(stack != null && !stack.isEmpty())
		{
			ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS), stack)
				.result().ifPresent(t -> tags.put(name, t));
		}
	}
	
	public static ItemStack readItemStack(CompoundTag tags, String name)
	{
		Tag tag = tags.get(name);
		if(tag == null)
			return ItemStack.EMPTY.copy();
		return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS), tag)
			.result().orElse(ItemStack.EMPTY.copy());
	}
}
