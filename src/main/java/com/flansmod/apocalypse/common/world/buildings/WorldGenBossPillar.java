package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import com.flansmod.common.ModuloHelper;


import net.minecraft.world.level.block.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;


public class WorldGenBossPillar extends WorldGenFlan
{
	public static final double kPillarInnerEdge = 12d;
	public static final double kPillarInnerRadius = kPillarInnerEdge * Math.sqrt(2);
	public static final double kPillarOuterEdge = 300d;
	public static final double kPillarMaxHeight = 240d;
	public static final double kBossSpawnHeight = 220d;
	
	//private static final double kA = kPillarMaxHeight * Math.exp(kPillarInnerEdge);
	private static final double kB = -Math.log(kPillarMaxHeight) / (kPillarOuterEdge - kPillarInnerRadius);
	private static final double kA = Math.exp(-kB * kPillarOuterEdge);
	
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos) 
	{
		for(int i = 8; i < 24; i++)
		{
			for(int k = 8; k < 24; k++)
			{
				BlockPos p = pos.offset(i, 0, k);
				
				if(Math.abs(p.getX()) > kPillarInnerEdge && Math.abs(p.getZ()) > kPillarInnerEdge)
				{
					double dist = Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY() + p.getZ() * p.getZ());
					double theta = Math.atan2(p.getZ(), p.getX());

					double pillarBaseHeight = kA * Math.exp(kB * dist);
					
					if(pillarBaseHeight > 1d)
					{
						BlockPos downIterate = p.offset(0, (int)pillarBaseHeight, 0);
						//world.setBlockAndUpdate(p.add(0,pillarBaseHeight,0), Blocks.BEDROCK.defaultBlockState());

						while(world.isEmptyBlock(downIterate) && downIterate.getY() > 1d)
						{
							if((Math.abs(p.getX()) == kPillarInnerEdge + 1 || Math.abs(p.getZ()) == kPillarInnerEdge + 1) && rand.nextInt(500) == 0)
							{
								world.setBlockAndUpdate(downIterate, Blocks.MAGMA_BLOCK.defaultBlockState());
							}
							else
							{
								world.setBlockAndUpdate(downIterate, Blocks.BEDROCK.defaultBlockState());
							}
							downIterate = downIterate.below();
						}
					}
				}
			}
		}
		return false;
	}
	
}
