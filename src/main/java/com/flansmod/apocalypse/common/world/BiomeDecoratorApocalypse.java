package com.flansmod.apocalypse.common.world;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import com.flansmod.apocalypse.common.FlansModApocalypse;

public class BiomeDecoratorApocalypse
{
	public boolean generateSulphurLakes = false;
	
	public void decorate(Level worldIn, Random random, Biome biome, BlockPos pos)
	{
		// TODO APOCALYPSE: 1.12.2 decorator also generated ore veins; ores are not generated in 26.1.2
		if(this.generateSulphurLakes)
		{
			for(int j = 0; j < 15; ++j)
			{
				BlockPos blockpos1 = pos.offset(random.nextInt(16) + 8, random.nextInt(10) + 30, random.nextInt(16) + 8);
				(new WorldGenSulphurPool(FlansModApocalypse.blockSulphuricAcid)).generate(worldIn, random, blockpos1);
			}
		}
	}
}
