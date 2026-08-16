package com.flansmod.common.driveables.mechas;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum EnumMechaToolType
{
	pickaxe, axe, shovel, shears, sword;
	
	public static EnumMechaToolType getToolType(String s)
	{
		for(EnumMechaToolType type : values())
		{
			if(type.toString().equals(s))
				return type;
		}
		return sword;
	}
	
	public boolean effectiveAgainst(BlockState state)
	{
		if(state == null)
			return false;
		Block block = state.getBlock();
		switch(this)
		{
			case pickaxe: return block == Blocks.IRON_BLOCK || block == Blocks.ANVIL || block == Blocks.STONE || block == Blocks.ICE;
			case axe: return block == Blocks.OAK_LOG || block == Blocks.WHEAT || block == Blocks.VINE;
			case shovel: return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.SPONGE || block == Blocks.SAND || block == Blocks.SNOW_BLOCK || block == Blocks.SNOW || block == Blocks.CLAY;
			case shears: return block == Blocks.OAK_LEAVES || block == Blocks.VINE || block == Blocks.WHITE_WOOL || block == Blocks.WHITE_CARPET;
			case sword: return block == Blocks.COBWEB;
		}
		return false;
	}
}
