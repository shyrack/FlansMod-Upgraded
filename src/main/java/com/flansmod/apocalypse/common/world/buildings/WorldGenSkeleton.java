package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.BlockItemHolder;
import com.flansmod.common.TileEntityItemHolder;

public class WorldGenSkeleton extends WorldGenFlan
{
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		for(; pos.getY() < 256; pos = pos.above())
		{
			if(world.isEmptyBlock(pos) && world.getBlockState(pos.below()).isSolid())
			{
				world.setBlockAndUpdate(pos, FlansModApocalypse.skeleton.defaultBlockState().setValue(BlockItemHolder.FACING, Direction.values()[2 + rand.nextInt(4)]));
				if(world.getBlockEntity(pos) instanceof TileEntityItemHolder)
					FlansModApocalypse.getLootGenerator().addRandomLoot((TileEntityItemHolder)world.getBlockEntity(pos), rand, false);
				break;
			}
		}
		return false;
	}
}
