package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class WorldGenDeadTree extends WorldGenFlan
{
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		for(; pos.getY() < 256; pos = pos.above())
		{
			if(world.isEmptyBlock(pos) && world.getBlockState(pos.below()).isSolid())
			{
				int treeHeight = rand.nextInt(3) + 3;
				for(int i = 0; i < treeHeight; i++)
				{
					world.setBlockAndUpdate(pos.offset(0, i, 0), Blocks.OAK_LOG.defaultBlockState());
				}
				for(int j = 0; j < rand.nextInt(2) + 2; j++)
				{
					int dx = 0, dy = 0, dz = 0;
					int branchXDir = rand.nextInt(3) - 1;
					int branchZDir = rand.nextInt(3) - 1;
					int branchStartPoint = rand.nextInt(treeHeight / 2) + treeHeight / 2;
					for(int i = 0; i < treeHeight; i++)
					{
						if(rand.nextBoolean())
						{
							dx += branchXDir;
							dz += branchZDir;
						}
						dy++;
						world.setBlockAndUpdate(pos.offset(dx, dy + treeHeight - 1, dz), Blocks.OAK_LOG.defaultBlockState());
					}
				}
				break;
			}
		}
		return false;
	}
}
