package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.world.BiomeApocalypse;
import com.flansmod.common.FlansMod;

public class WorldGenDyeFactory extends WorldGenFlan
{
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		if(world.getBiome(pos) != BiomeApocalypse.deepCanyon)
			return false;
		int maxHeight = 0;
		int minHeight = 128;
		for(int i = 0; i < 16; i++)
		{
			for(int k = 0; k < 16; k++)
			{
				//Find the ground height at this block
				BlockPos testPos = new BlockPos(pos.getX() + i, 128, pos.getZ() + k);
				for(; world.isEmptyBlock(testPos); testPos = testPos.below())
				{
				}
				int height = testPos.getY();
				
				//Compare to our existing min and max
				if(height < minHeight)
					minHeight = height;
				if(height > maxHeight)
					maxHeight = height;
			}
		}
		//LevelChunk is too bumpy
		if(maxHeight - minHeight > 3)
		{
			return false;
		}
		
		int x = pos.getX();
		int y = maxHeight + 1;
		int z = pos.getZ();
		
		//Set our base
		fillArea(world, x, minHeight, z, x + 16, maxHeight + 1, z + 16, Blocks.RED_SAND.defaultBlockState());
		
		//Build walls
		fillArea(world, x, y, z, x + 6, y + 1, z + 16, Blocks.COBBLESTONE.defaultBlockState());
		fillArea(world, x, y + 1, z, x + 6, y + 5, z + 16, Blocks.OAK_PLANKS.defaultBlockState());
		//Horizontal logs
		fillArea(world, x, y + 4, z, x + 6, y + 5, z + 1, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fillArea(world, x, y + 4, z + 15, x + 6, y + 5, z + 16, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fillArea(world, x, y + 4, z, x + 1, y + 5, z + 16, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		fillArea(world, x + 5, y + 4, z, x + 6, y + 5, z + 16, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		//Vertical logs
		for(int i = 0; i < 6; i += 5)
			for(int k = 0; k < 16; k += 5)
				fillArea(world, x + i, y + 1, z + k, x + i + 1, y + 6, z + k + 1, Blocks.OAK_LOG.defaultBlockState());
		//Hollow out
		fillArea(world, x + 1, y, z + 1, x + 5, y + 4, z + 15, Blocks.AIR.defaultBlockState());
		//Open door
		fillArea(world, x + 5, y, z + 6, x + 6, y + 4, z + 10, Blocks.AIR.defaultBlockState());
		
		//Add tables (two random, one gun modification and one vehicle crafting)
		world.setBlockAndUpdate(new BlockPos(x + 1, y, z + 6), FlansModApocalypse.getLootGenerator().getRandomWeaponBox(rand).defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y, z + 7), FlansMod.workbench.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y, z + 8), FlansMod.workbench.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y, z + 9), FlansModApocalypse.getLootGenerator().getRandomWeaponBox(rand).defaultBlockState());
		
		//Build chest racks
		for(int i = 1; i < 6; i += 3)
			for(int k = 1; k < 6; k += 3)
				fillArea(world, x + i, y, z + k, x + i + 1, y + 3, z + k + 1, Blocks.OAK_FENCE.defaultBlockState());
		
		for(int j = 0; j < 3; j += 2)
		{
			fillArea(world, x + 1, y + j, z + 2, x + 2, y + j + 1, z + 4, Blocks.CHEST.defaultBlockState());
			fillArea(world, x + 4, y + j, z + 2, x + 5, y + j + 1, z + 4, Blocks.CHEST.defaultBlockState());
			fillArea(world, x + 2, y + j, z + 1, x + 4, y + j + 1, z + 2, Blocks.CHEST.defaultBlockState());
			
			fillChest(world, rand, x + 1, y + j, z + 2);
			fillChest(world, rand, x + 1, y + j, z + 3);
			fillChest(world, rand, x + 4, y + j, z + 2);
			fillChest(world, rand, x + 4, y + j, z + 3);
			fillChest(world, rand, x + 2, y + j, z + 1);
			fillChest(world, rand, x + 3, y + j, z + 1);
		}
		
		//Build sewing table
		fillArea(world, x + 1, y, z + 11, x + 2, y + 1, z + 15, Blocks.CRAFTING_TABLE.defaultBlockState());
		fillArea(world, x + 1, y, z + 12, x + 2, y + 1, z + 14, Blocks.OAK_SLAB.defaultBlockState());
		
		for(int k = 0; k < 2; k++)
		{
			ArmorStand stand = new ArmorStand(world, x + 4.5D, y, z + 11.5D + k * 2D);
			stand.setYRot(90F);
			FlansModApocalypse.getLootGenerator().dressMeUp(stand, rand);
			if(!world.isClientSide()) ((net.minecraft.server.level.ServerLevel)world).addFreshEntity(stand);
		}
		
		//Build vats
		for(int k = 1; k < 16; k += 5)
			buildVat(world, rand, x + 11, y, z + k);
		
		return true;
	}
	
	private void fillChest(Level world, Random rand, int i, int j, int k)
	{
		ChestBlockEntity chest = (ChestBlockEntity)world.getBlockEntity(new BlockPos(i, j, k));
		FlansModApocalypse.getLootGenerator().fillDyeFactoryChest(chest, rand);
	}

	private void buildVat(Level world, Random rand, int x, int y, int z)
	{
		boolean tall = rand.nextBoolean();
		
		//Create square tank
		fillArea(world, x, y, z, x + 4, y + 1, z + 4, Blocks.COBBLESTONE.defaultBlockState());
		fillArea(world, x, y + 1, z, x + 4, y + (tall ? 3 : 2), z + 4, Blocks.OAK_PLANKS.defaultBlockState());
		
		//Cut out corners
		for(int i = 0; i < 4; i += 3)
			for(int k = 0; k < 4; k += 3)
				fillArea(world, x + i, y, z + k, x + i + 1, y + 3, z + k + 1, Blocks.AIR.defaultBlockState());
		
		//Fill tank with wool
		fillArea(world, x + 1, y, z + 1, x + 3, y + 3, z + 3, Blocks.AIR.defaultBlockState());
		fillArea(world, x + 1, y, z + 1, x + 3, y + (tall ? 2 : 1), z + 3, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
	}
}
