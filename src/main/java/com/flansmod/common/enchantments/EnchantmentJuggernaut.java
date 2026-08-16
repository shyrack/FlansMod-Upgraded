package com.flansmod.common.enchantments;

import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentJuggernaut
{
	public static Enchantment build(Identifier id, HolderSet<Item> supportedItems)
	{
		return Enchantment.enchantment(
				Enchantment.definition(supportedItems, 1, 4, Enchantment.constantCost(25), Enchantment.constantCost(75), 1, EquipmentSlotGroup.ARMOR))
				.build(id);
	}
}
