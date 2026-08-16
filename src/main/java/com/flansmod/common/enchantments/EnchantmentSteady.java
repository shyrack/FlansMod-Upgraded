package com.flansmod.common.enchantments;

import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentSteady
{
	public static Enchantment build(Identifier id, HolderSet<Item> supportedItems)
	{
		return Enchantment.enchantment(
				Enchantment.definition(supportedItems, 3, 2, Enchantment.constantCost(5), Enchantment.constantCost(25), 10, EquipmentSlotGroup.OFFHAND))
				.build(id);
	}
}
