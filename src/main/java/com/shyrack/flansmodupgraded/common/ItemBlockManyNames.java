package com.shyrack.flansmodupgraded.common;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ItemBlockManyNames extends BlockItem {

    public ItemBlockManyNames(Block b) {
        super(b);
        setHasSubtypes(true);
        setRegistryName(b.getRegistryName() + "_item");
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey() + "." + stack.getItemDamage();
    }

    @Override
    public int getMetadata(int par1) {
        return par1;
    }

    @Override
    public CreativeTabs[] getCreativeTabs() {
        return new CreativeTabs[]{FlansMod.tabFlanDriveables, FlansMod.tabFlanGuns, FlansMod.tabFlanTeams, FlansMod.tabFlanParts};
    }

}
