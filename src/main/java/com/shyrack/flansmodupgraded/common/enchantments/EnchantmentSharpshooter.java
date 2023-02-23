package com.shyrack.flansmodupgraded.common.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentDamage;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.inventory.EntityEquipmentSlot;

public class EnchantmentSharpshooter extends Enchantment
{
	protected EnchantmentSharpshooter() 
	{
		super(Rarity.COMMON, EnchantmentModule.OFF_HAND, new EntityEquipmentSlot[] { EntityEquipmentSlot.OFFHAND } );
	}
	
	@Override
    public int getMaxLevel()
    {
        return 3;
    }
	
	@Override
    public boolean canApplyTogether(Enchantment ench)
    {
        if(ench instanceof EnchantmentSharpshooter)
        	return false;
        
        if(ench instanceof EnchantmentLumberjack)
        	return false;
        
        if(ench instanceof EnchantmentDuelist)
        	return false;
        
        return true;
    }
}
