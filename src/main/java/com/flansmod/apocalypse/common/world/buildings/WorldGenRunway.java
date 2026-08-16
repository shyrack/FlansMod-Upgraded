package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.world.level.block.ChestBlock;


import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModuloHelper;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EnumDriveablePart;

public class WorldGenRunway extends WorldGenFlan
{
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		int chunkX = ModuloHelper.divide(pos.getX(), 16);
		
		int x = pos.getX();
		int z = pos.getZ();
		
		int yHeight = 108;
		
		//Create runway
		for(int j = 1; j < 8; j++)
		{
			fillArea(world, x, yHeight - j, z + j, x + 16, yHeight - j + 1, z + 16 - j, Blocks.STONE.defaultBlockState());
		}
		fillArea(world, x, yHeight, z, x + 16, yHeight + 1, z + 16, Blocks.BLACK_TERRACOTTA.defaultBlockState());
		
		fillArea(world, x + 2, yHeight, z + 7, x + 6, yHeight + 1, z + 9, Blocks.QUARTZ_BLOCK.defaultBlockState());
		fillArea(world, x + 10, yHeight, z + 7, x + 14, yHeight + 1, z + 9, Blocks.QUARTZ_BLOCK.defaultBlockState());
		

		fillArea(world, x, yHeight + 1, z, x + 16, yHeight + 11, z + 16, Blocks.AIR.defaultBlockState());
		
		if(ModuloHelper.modulo(chunkX, 4) == 1)
		{
			//Create hangar
			fillArea(world, x, yHeight + 1, z, x + 16, yHeight + 5, z + 1, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 5, z + 1, x + 16, yHeight + 7, z + 2, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 7, z + 2, x + 16, yHeight + 8, z + 3, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 8, z + 3, x + 16, yHeight + 9, z + 5, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 9, z + 5, x + 16, yHeight + 10, z + 11, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			
			fillArea(world, x, yHeight + 8, z + 11, x + 16, yHeight + 9, z + 13, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 7, z + 13, x + 16, yHeight + 8, z + 14, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 5, z + 14, x + 16, yHeight + 7, z + 15, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 1, z + 15, x + 16, yHeight + 5, z + 16, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			
			fillArea(world, x, yHeight + 1, z + 1, x + 1, yHeight + 5, z + 15, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 5, z + 2, x + 1, yHeight + 7, z + 14, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 7, z + 3, x + 1, yHeight + 8, z + 13, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			fillArea(world, x, yHeight + 8, z + 5, x + 1, yHeight + 9, z + 11, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
			
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 1, z + 1), Blocks.CRAFTING_TABLE.defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 2, yHeight + 1, z + 1), FlansMod.workbench.defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 1, z + 14), FlansMod.workbench.defaultBlockState());
			
			
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 4, z + 14), Blocks.GLOWSTONE.defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 4, z + 1), Blocks.GLOWSTONE.defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 7, z + 12), Blocks.GLOWSTONE.defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 1, yHeight + 7, z + 3), Blocks.GLOWSTONE.defaultBlockState());
			
			world.setBlockAndUpdate(new BlockPos(x + 3, yHeight + 1, z + 14), FlansModApocalypse.getLootGenerator().getRandomWeaponBox(rand).defaultBlockState());
			world.setBlockAndUpdate(new BlockPos(x + 4, yHeight + 1, z + 14), FlansModApocalypse.getLootGenerator().getRandomWeaponBox(rand).defaultBlockState());

			world.setBlockAndUpdate(new BlockPos(x + 4, yHeight + 1, z + 1), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
			world.setBlockAndUpdate(new BlockPos(x + 5, yHeight + 1, z + 1), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
			
			BlockPos chestPos = new BlockPos(x + 4, yHeight + 1, z + 1);
			BlockEntity tileentity = world.getBlockEntity(chestPos);
			if(tileentity instanceof ChestBlockEntity)
			{
				FlansModApocalypse.getLootGenerator().fillVillageChest(rand, (ChestBlockEntity)tileentity);
			}

			chestPos = new BlockPos(x + 5, yHeight + 1, z + 1);
			tileentity = world.getBlockEntity(chestPos);
			if(tileentity instanceof ChestBlockEntity)
			{
				FlansModApocalypse.getLootGenerator().fillVillageChest(rand, (ChestBlockEntity)tileentity);
			}

		}
		
		if(ModuloHelper.modulo(chunkX, 4) == 0)
		{
			//Spawn a plane
			DriveableType type = FlansModApocalypse.getLootGenerator().getRandomPlane(rand);
			CompoundTag tags = new CompoundTag();
			tags.putString("Engine", FlansModApocalypse.getLootGenerator().getRandomEngine(type, rand).shortName);
			tags.putString("Type", type.shortName);
			for(EnumDriveablePart part : EnumDriveablePart.values())
			{
				tags.putInt(part.getShortName() + "_Health", type.health.get(part) == null ? 0 : rand.nextInt(type.health.get(part).health));
				tags.putBoolean(part.getShortName() + "_Fire", false);
			}

			EntityDriveable entity = type.createDriveable(world, x + 8, yHeight + 3, z + 8, new DriveableData(tags));

			entity.setRotation(0F, 0, 0);

			if(!world.isClientSide()) ((net.minecraft.server.level.ServerLevel)world).addFreshEntity(entity);
		}

		return false;
	}
}
