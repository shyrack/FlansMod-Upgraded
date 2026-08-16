package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.entity.EntityAIMecha;
import com.flansmod.common.BlockItemHolder;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModuloHelper;
import com.flansmod.common.TileEntityItemHolder;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.mechas.EnumMechaSlotType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShootableType;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.minecraft.world.level.Level;

public class WorldGenAbandonedPortal extends WorldGenFlan
{
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		// Base
		replaceEmpty(world, x + 0, y - 3, z + 0, x + 12, y - 2, z + 12, FlansModApocalypse.blockLabStone.defaultBlockState());
		replaceEmpty(world, x + 1, y - 2, z + 1, x + 11, y - 1, z + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
		replaceEmpty(world, x + 2, y - 1, z + 2, x + 10, y - 0, z + 10, FlansModApocalypse.blockLabStone.defaultBlockState());
		replaceEmpty(world, x + 3, y - 0, z + 3, x + 9, y + 1, z + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
		
		//Teleporter Room
		{
			fillArea(world, x + 4, y + 1, z + 4, x + 8, y + 2, z + 8, Blocks.OBSIDIAN.defaultBlockState());
			for(int n = 0; n < 2; n++)
			{
				world.setBlockAndUpdate(new BlockPos(x + 4 + rand.nextInt(2) * 3, y + 2, z + 4 + rand.nextInt(2) * 3), FlansModApocalypse.blockPowerCube.defaultBlockState());
			}
			
			BlockPos chestPos = new BlockPos(x + 3 + rand.nextInt(2) * 5, y + 1, z + 3 + rand.nextInt(2) * 5);
			
			world.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
			
			//Fill chests
			BlockEntity tileentity = world.getBlockEntity(chestPos);
			if(tileentity instanceof ChestBlockEntity)
			{
				ChestBlockEntity chest = (ChestBlockEntity)tileentity;
				FlansModApocalypse.getLootGenerator().fillWeaponChest(rand, chest);
				chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Blocks.OBSIDIAN, rand.nextInt(8) + 1));
				chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(FlansModApocalypse.itemBlockPowerCube));
			}
		}
		return false;
	}
	


}