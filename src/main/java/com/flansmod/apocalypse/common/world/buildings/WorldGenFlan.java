package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class WorldGenFlan extends Feature<NoneFeatureConfiguration>
{
	public WorldGenFlan()
	{
		super(NoneFeatureConfiguration.CODEC);
	}
	
	public abstract boolean generate(Level world, Random rand, BlockPos pos);
	
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx)
	{
		return generate(ctx.level().getLevel(), new Random(ctx.random().nextLong()), ctx.origin());
	}
	
	protected void fillArea(Level world, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)
	{
		fillArea(world, x1, y1, z1, x2, y2, z2, state, state);
	}
	
	protected void fillArea(Level world, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state, BlockState innerState)
	{
		for(int i = x1; i < x2; i++)
		{
			for(int j = y1; j < y2; j++)
			{
				for(int k = z1; k < z2; k++)
				{
					if(i == x1 || i == x2 - 1 || j == y1 || j == y2 - 1 || k == z1 || k == z2 - 1)
						world.setBlockAndUpdate(new BlockPos(i, j, k), state);
					else world.setBlockAndUpdate(new BlockPos(i, j, k), innerState);
				}
			}
		}
	}
	
	protected void replaceEmpty(Level world, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)
	{
		for(int i = x1; i < x2; i++)
		{
			for(int j = y1; j < y2; j++)
			{
				for(int k = z1; k < z2; k++)
				{
					BlockPos pos = new BlockPos(i, j, k);
					if(world.isEmptyBlock(pos))
						world.setBlockAndUpdate(pos, state);
				}
			}
		}
	}
}
