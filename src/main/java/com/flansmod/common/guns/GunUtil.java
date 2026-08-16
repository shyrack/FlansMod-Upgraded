package com.flansmod.common.guns;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Small helpers used across the guns subsystem. ItemStack NBT is stored in the
 * CUSTOM_DATA component in 26.1.2, so the old getTag/setTag calls are
 * translated here.
 */
public final class GunUtil
{
	private GunUtil()
	{
	}

	public static boolean hasTag(ItemStack stack)
	{
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && !data.isEmpty();
	}

	public static CompoundTag getTag(ItemStack stack)
	{
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag();
	}

	public static void setTag(ItemStack stack, CompoundTag tag)
	{
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static CompoundTag getOrCreateTag(ItemStack stack)
	{
		CompoundTag tag = getTag(stack);
		if(tag == null)
		{
			tag = new CompoundTag();
			setTag(stack, tag);
		}
		return tag;
	}

	/**
	 * Serialises an ItemStack into a CompoundTag (id/count/damage only, which is
	 * all the guns subsystem needs for ammo and attachments).
	 */
	public static void stackToTag(CompoundTag tag, ItemStack stack)
	{
		if(stack == null || stack.isEmpty())
		{
			tag.putString("id", "");
			return;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		tag.putString("id", id == null ? "" : id.toString());
		tag.putInt("Count", stack.getCount());
		if(stack.getDamageValue() > 0)
			tag.putInt("Damage", stack.getDamageValue());
	}

	/**
	 * Deserialises an ItemStack from a CompoundTag written by stackToTag. Empty
	 * or unknown tags yield an empty stack, mirroring the old
	 * new ItemStack(CompoundTag) behaviour.
	 */
	public static ItemStack stackFromTag(CompoundTag tag)
	{
		if(tag == null || tag.isEmpty())
			return ItemStack.EMPTY.copy();
		String id = tag.getStringOr("id", "");
		if(id.isEmpty())
			return ItemStack.EMPTY.copy();
		Identifier identifier = Identifier.tryParse(id);
		if(identifier == null)
			return ItemStack.EMPTY.copy();
		Item item = BuiltInRegistries.ITEM.getValue(identifier);
		if(item == null)
			return ItemStack.EMPTY.copy();
		int count = Math.max(1, tag.getIntOr("Count", 1));
		ItemStack stack = new ItemStack(item, count);
		int damage = tag.getIntOr("Damage", 0);
		if(damage > 0)
			stack.setDamageValue(damage);
		return stack;
	}

	/**
	 * Old code passed a numeric dimension id (0 overworld, -1 nether, 1 end)
	 * around in packets. This converts the modern ResourceKey back to that id.
	 */
	public static int getDimensionId(Level world)
	{
		ResourceKey<Level> dimension = world.dimension();
		if(dimension == Level.NETHER)
			return -1;
		if(dimension == Level.END)
			return 1;
		return 0;
	}
}
