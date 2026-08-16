package com.flansmod.apocalypse.common.world.buildings;

import java.util.Random;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;


import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.minecraft.world.level.Level;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.entity.EntityAIMecha;
import com.flansmod.common.BlockItemHolder;
import com.flansmod.common.FlansMod;

import com.flansmod.common.ModuloHelper;
import com.flansmod.common.TileEntityItemHolder;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.mechas.EnumMechaSlotType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.parts.PartType;

public class WorldGenResearchLab extends WorldGenFlan
{
	@Override
	public boolean generate(Level world, Random rand, BlockPos pos)
	{
		int chunkX = ModuloHelper.divide(pos.getX(), 16);
		int chunkZ = ModuloHelper.divide(pos.getZ(), 16);
		
		int structureX = ModuloHelper.divide(chunkX, 3);
		int structureZ = ModuloHelper.divide(chunkZ, 3);
		
		int pieceX = ModuloHelper.modulo(chunkX, 3);
		int pieceZ = ModuloHelper.modulo(chunkZ, 3);
		
		int topLayerHeight = 99;
		
		//Generate empty rooms
		for(int i = (pieceX == 1 & pieceZ == 1 ? -1 : 0); i < 8; i++)
		{
			fillArea(world, chunkX * 16, topLayerHeight - 8 * i, chunkZ * 16, chunkX * 16 + 16, topLayerHeight - 8 * i + 8, chunkZ * 16 + 16, FlansModApocalypse.blockLabStone.defaultBlockState(), Blocks.AIR.defaultBlockState());
			fillArea(world, chunkX * 16, topLayerHeight - 8 * i + 6, chunkZ * 16, chunkX * 16 + 16, topLayerHeight - 8 * i + 7, chunkZ * 16 + 16, FlansModApocalypse.blockLabStone.defaultBlockState());
			//Add glowstone lights
			for(int j = 0; j < 2; j++)
			{
				for(int k = 0; k < 2; k++)
				{
					fillArea(world, chunkX * 16 + 3 + 8 * j, topLayerHeight - 8 * i + 6, chunkZ * 16 + 3 + 8 * k, chunkX * 16 + 5 + 8 * j, topLayerHeight - 8 * i + 7, chunkZ * 16 + 5 + 8 * k, Blocks.GLOWSTONE.defaultBlockState());
					if(world.isClientSide())
					{
						for(int x = 0; x < 2; x++)
						{
							for(int z = 0; z < 2; z++)
							{
							}
						}
					}
				}
			}
			
			//Make doors
			if(pieceX != 0)
				fillArea(world, chunkX * 16 + 0, topLayerHeight - 8 * i + 1, chunkZ * 16 + 7, chunkX * 16 + 1, topLayerHeight - 8 * i + 4, chunkZ * 16 + 9, Blocks.AIR.defaultBlockState());
			if(pieceX != 2)
				fillArea(world, chunkX * 16 + 15, topLayerHeight - 8 * i + 1, chunkZ * 16 + 7, chunkX * 16 + 16, topLayerHeight - 8 * i + 4, chunkZ * 16 + 9, Blocks.AIR.defaultBlockState());
			if(pieceZ != 0)
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 0, chunkX * 16 + 9, topLayerHeight - 8 * i + 4, chunkZ * 16 + 1, Blocks.AIR.defaultBlockState());
			if(pieceZ != 2)
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 15, chunkX * 16 + 9, topLayerHeight - 8 * i + 4, chunkZ * 16 + 16, Blocks.AIR.defaultBlockState());
			
			for(int j = 0; j < 16; j++)
			{
				for(int k = 0; k < 16; k++)
				{
					//world.checkLightFor(EnumSkyBlock.BLOCK, new BlockPos(chunkX * 16 + j, topLayerHeight - 8 * i + 4, chunkZ * 18 + k));
				}
			}
		}
		//Populate rooms
		for(int i = 0; i < 8; i++)
		{
			if(i == 7 && pieceX == 1 && pieceZ == 1)
			{
				//Teleporter Room
				fillArea(world, chunkX * 16 + 3, topLayerHeight - 8 * i, chunkZ * 16 + 3, chunkX * 16 + 13, topLayerHeight - 8 * i + 1, chunkZ * 16 + 13, Blocks.GLOWSTONE.defaultBlockState());
				fillArea(world, chunkX * 16 + 4, topLayerHeight - 8 * i, chunkZ * 16 + 4, chunkX * 16 + 12, topLayerHeight - 8 * i + 1, chunkZ * 16 + 12, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 6, topLayerHeight - 8 * i + 1, chunkZ * 16 + 6, chunkX * 16 + 10, topLayerHeight - 8 * i + 2, chunkZ * 16 + 10, Blocks.OBSIDIAN.defaultBlockState());
				fillArea(world, chunkX * 16 + 6, topLayerHeight - 8 * i + 1, chunkZ * 16 + 7, chunkX * 16 + 7, topLayerHeight - 8 * i + 2, chunkZ * 16 + 9, Blocks.STONE_SLAB.defaultBlockState());
				fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i + 1, chunkZ * 16 + 7, chunkX * 16 + 10, topLayerHeight - 8 * i + 2, chunkZ * 16 + 9, Blocks.STONE_SLAB.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 6, chunkX * 16 + 9, topLayerHeight - 8 * i + 2, chunkZ * 16 + 7, Blocks.STONE_SLAB.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 9, chunkX * 16 + 9, topLayerHeight - 8 * i + 2, chunkZ * 16 + 10, Blocks.STONE_SLAB.defaultBlockState());
				world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 6, topLayerHeight - 8 * i + 2, chunkZ * 16 + 6), FlansModApocalypse.blockPowerCube.defaultBlockState());
				
				for(int k = 0; k < 8; k++)
					spawnMecha(world, rand, chunkX * 16 + 4 + rand.nextInt(8), topLayerHeight - 8 * i + 2, chunkZ * 16 + 4 + rand.nextInt(8));
			}
			//Build entrance
			else if(i == 0 && pieceX == 1 && pieceZ == 1)
			{
				i--;
				
				//Make hole
				fillArea(world, chunkX * 16 + 4, topLayerHeight - 8 * i + 1, chunkZ * 16 + 4, chunkX * 16 + 12, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.NETHER_BRICK_FENCE.defaultBlockState());
				fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 2, chunkZ * 16 + 5, chunkX * 16 + 11, topLayerHeight - 8 * i + 2, chunkZ * 16 + 11, Blocks.AIR.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 11, chunkX * 16 + 9, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.AIR.defaultBlockState());
				
				//Build stairs
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 1, chunkZ * 16 + 9, chunkX * 16 + 9, topLayerHeight - 8 * i, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, chunkX * 16 + 7, topLayerHeight - 8 * i - 1, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, chunkX * 16 + 11, topLayerHeight - 8 * i - 1, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, chunkX * 16 + 11, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, chunkX * 16 + 7, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 4, chunkZ * 16 + 5, chunkX * 16 + 11, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 5, chunkZ * 16 + 7, chunkX * 16 + 9, topLayerHeight - 8 * i - 4, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 6, chunkZ * 16 + 9, chunkX * 16 + 9, topLayerHeight - 8 * i - 5, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 7, chunkZ * 16 + 11, chunkX * 16 + 9, topLayerHeight - 8 * i - 6, chunkZ * 16 + 13, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 7, chunkZ * 16 + 9, chunkX * 16 + 7, topLayerHeight - 8 * i - 6, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 7, chunkZ * 16 + 9, chunkX * 16 + 11, topLayerHeight - 8 * i - 6, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
				
				i++;
			}
			else
			{
				boolean spawnMecha = true;
				switch(i == 6 && pieceX == 1 && pieceZ == 1 ? 4 : rand.nextInt(7))
				{
					case 0: //Stairs
					{
						//Make hole
						fillArea(world, chunkX * 16 + 4, topLayerHeight - 8 * i + 1, chunkZ * 16 + 4, chunkX * 16 + 12, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.NETHER_BRICK_FENCE.defaultBlockState());
						fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 2, chunkZ * 16 + 5, chunkX * 16 + 11, topLayerHeight - 8 * i + 2, chunkZ * 16 + 11, Blocks.AIR.defaultBlockState());
						fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i + 1, chunkZ * 16 + 11, chunkX * 16 + 9, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.AIR.defaultBlockState());

						//Build stairs
						fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 1, chunkZ * 16 + 9, chunkX * 16 + 9, topLayerHeight - 8 * i, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, chunkX * 16 + 7, topLayerHeight - 8 * i - 1, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, chunkX * 16 + 11, topLayerHeight - 8 * i - 1, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, chunkX * 16 + 11, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, chunkX * 16 + 7, topLayerHeight - 8 * i - 2, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 4, chunkZ * 16 + 5, chunkX * 16 + 11, topLayerHeight - 8 * i - 3, chunkZ * 16 + 7, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 5, chunkZ * 16 + 7, chunkX * 16 + 9, topLayerHeight - 8 * i - 4, chunkZ * 16 + 9, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 6, chunkZ * 16 + 9, chunkX * 16 + 9, topLayerHeight - 8 * i - 5, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 7, topLayerHeight - 8 * i - 7, chunkZ * 16 + 11, chunkX * 16 + 9, topLayerHeight - 8 * i - 6, chunkZ * 16 + 13, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 5, topLayerHeight - 8 * i - 7, chunkZ * 16 + 9, chunkX * 16 + 7, topLayerHeight - 8 * i - 6, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());
						fillArea(world, chunkX * 16 + 9, topLayerHeight - 8 * i - 7, chunkZ * 16 + 9, chunkX * 16 + 11, topLayerHeight - 8 * i - 6, chunkZ * 16 + 11, FlansModApocalypse.blockLabStone.defaultBlockState());

						i++;

						spawnMecha = false;
						break;
					}
					case 1: //Liquids room
					{
						for(int j = 0; j < 2; j++)
						{
							for(int k = 0; k < 2; k++)
							{
								if(rand.nextInt(3) == 0)
								{
									generateLiquidsLab(world, rand, chunkX * 16 + 1 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1 + 8 * k);
								}
								else
									generateLiquidContainer(world, rand, chunkX * 16 + 2 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 2 + 8 * k, getRandomLiquid(rand));
							}
						}
						break;
					}
					case 2: //Gun range
					{
						for(int j = 0; j < 2; j++)
						{
							generateTarget(world, rand, chunkX * 16 + 2 + 7 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1);
						}
						fillArea(world, chunkX * 16 + 3, topLayerHeight - 8 * i + 1, chunkZ * 16 + 6, chunkX * 16 + 4, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.OAK_PLANKS.defaultBlockState());
						fillArea(world, chunkX * 16 + 12, topLayerHeight - 8 * i + 1, chunkZ * 16 + 6, chunkX * 16 + 13, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.OAK_PLANKS.defaultBlockState());
						fillArea(world, chunkX * 16 + 4, topLayerHeight - 8 * i + 1, chunkZ * 16 + 11, chunkX * 16 + 12, topLayerHeight - 8 * i + 2, chunkZ * 16 + 12, Blocks.STONE_SLAB.defaultBlockState());
						world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 6, topLayerHeight - 8 * i + 1, chunkZ * 16 + 11), Blocks.OAK_PLANKS.defaultBlockState());
						world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 9, topLayerHeight - 8 * i + 1, chunkZ * 16 + 11), Blocks.OAK_PLANKS.defaultBlockState());

						generateGunRack(world, rand, chunkX * 16 + 1, topLayerHeight - 8 * i + 1, chunkZ * 16 + 14);
						generateGunRack(world, rand, chunkX * 16 + 4, topLayerHeight - 8 * i + 1, chunkZ * 16 + 14);
						generateGunRack(world, rand, chunkX * 16 + 10, topLayerHeight - 8 * i + 1, chunkZ * 16 + 14);
						generateGunRack(world, rand, chunkX * 16 + 13, topLayerHeight - 8 * i + 1, chunkZ * 16 + 14);

						break;
					}
					case 3: //Plant room
					{
						for(int j = 0; j < 2; j++)
						{
							for(int k = 0; k < 2; k++)
							{
								switch(rand.nextInt(2))
								{
									case 0: generatePlantPots(world, rand, chunkX * 16 + 1 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1 + 8 * k);
										break;
									case 1: generateFarm(world, rand, chunkX * 16 + 9 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 9 * k);
										break;
								}
							}
						}
						break;
					}
					case 4: //Forge
					{
						for(int j = 0; j < 2; j++)
						{
							if(rand.nextBoolean())
								generateFurnace(world, rand, chunkX * 16 + 2 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1);
							else
							{
								if(rand.nextBoolean())
									world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 2 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1), Blocks.CRAFTING_TABLE.defaultBlockState());
								else
									world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 2 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1), FlansMod.workbench.defaultBlockState());
								world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 4 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1), Blocks.IRON_BLOCK.defaultBlockState());
								world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 5 + 8 * j, topLayerHeight - 8 * i + 2, chunkZ * 16 + 1), Blocks.IRON_BLOCK.defaultBlockState());
								world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 5 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 1), Blocks.IRON_BLOCK.defaultBlockState());
								world.setBlockAndUpdate(new BlockPos(chunkX * 16 + 4 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 4), Blocks.ANVIL.defaultBlockState());
							}

							{
								generateWeapons(world, rand, chunkX * 16 + 2 + 8 * j, topLayerHeight - 8 * i + 1, chunkZ * 16 + 10);
							}
						}
						break;
					}
					case 5: //Power Room
					{
						generateServerRack(world, rand, chunkX * 16 + 1, topLayerHeight - 8 * i + 1, chunkZ * 16 + 2, true);
						generateServerRack(world, rand, chunkX * 16 + 1, topLayerHeight - 8 * i + 1, chunkZ * 16 + 5, false);
						generateServerRack(world, rand, chunkX * 16 + 1, topLayerHeight - 8 * i + 1, chunkZ * 16 + 10, false);
						generateServerRack(world, rand, chunkX * 16 + 1, topLayerHeight - 8 * i + 1, chunkZ * 16 + 13, true);

						generateServerRack(world, rand, chunkX * 16 + 10, topLayerHeight - 8 * i + 1, chunkZ * 16 + 2, true);
						generateServerRack(world, rand, chunkX * 16 + 12, topLayerHeight - 8 * i + 1, chunkZ * 16 + 5, false);
						generateServerRack(world, rand, chunkX * 16 + 12, topLayerHeight - 8 * i + 1, chunkZ * 16 + 10, false);
						generateServerRack(world, rand, chunkX * 16 + 10, topLayerHeight - 8 * i + 1, chunkZ * 16 + 13, true);

						generateServerPower(world, rand, chunkX * 16 + 6, topLayerHeight - 8 * i + 1, chunkZ * 16 + 6);

						spawnMecha = false;

						break;
					}
				}
				if(spawnMecha && rand.nextBoolean())
				{
					spawnMecha(world, rand, chunkX * 16 + 8, topLayerHeight - 8 * i + 1, chunkZ * 16 + 8);
				}
			}
		}
		return false;
	}
	
	private void spawnMecha(Level world, Random rand, int x, int y, int z)
	{
		MechaType type = FlansModApocalypse.getLootGenerator().getRandomDungeonMecha(rand);
		CompoundTag tags = new CompoundTag();
		PartType engine = FlansModApocalypse.getLootGenerator().getRandomEngine(type, rand);
		if(engine == null && PlaneType.types.size() > 0)
		{
			engine = FlansModApocalypse.getLootGenerator().getRandomEngine(PlaneType.types.get(0), rand);
		}
		if(engine == null && PartType.parts.size() > 0)
			engine = PartType.parts.get(0);
		
		tags.putString("Engine", engine.shortName);
		tags.putString("Type", type.shortName);
		for(EnumDriveablePart part : EnumDriveablePart.values())
		{
			tags.putInt(part.getShortName() + "_Health", type.health.get(part) == null ? 0 : type.health.get(part).health);
			tags.putBoolean(part.getShortName() + "_Fire", false);
		}
		for(int k = 0; k < 2; k++)
		{
			ItemStack randomGun = FlansModApocalypse.getLootGenerator().getRandomLoadedGun(rand, false);
			GunType gunType = ((ItemGun)randomGun.getItem()).GetType();
			tags.putString(k == 1 ? EnumMechaSlotType.rightTool.toString() : EnumMechaSlotType.leftTool.toString(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(randomGun.getItem()).toString()); // TODO APOCALYPSE: item NBT not saved
			if(gunType.nonExplosiveAmmo.size() > 0)
			{
				for(int j = 0; j < 1 + rand.nextInt(2); j++)
				{
					ShootableType ammo = gunType.nonExplosiveAmmo.get(rand.nextInt(gunType.nonExplosiveAmmo.size()));
					tags.putString("Cargo " + rand.nextInt(type.numCargoSlots), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo.item).toString()); // TODO APOCALYPSE: item NBT not saved
				}
			}
		}
		EntityAIMecha entity = new EntityAIMecha(world, x + 0.5D, y, z + 0.5D, type, new DriveableData(tags), tags);
		if(!world.isClientSide()) ((net.minecraft.server.level.ServerLevel)world).addFreshEntity(entity);
	}
	
	private void generateServerPower(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 2, y + 3, z + 2, Blocks.OBSIDIAN.defaultBlockState());
		fillArea(world, x + 1, y, z + 1, x + 3, y + 3, z + 3, Blocks.OBSIDIAN.defaultBlockState());
		fillArea(world, x + 2, y, z + 2, x + 4, y + 3, z + 4, Blocks.OBSIDIAN.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y + 1, z + 1), FlansModApocalypse.blockPowerCube.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 2, y + 1, z + 2), Blocks.AIR.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y + 2, z + 1), Blocks.IRON_TRAPDOOR.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 2, y + 2, z + 2), Blocks.IRON_TRAPDOOR.defaultBlockState());
	}
	
	private void generateServerRack(Level world, Random rand, int x, int y, int z, boolean big)
	{
		fillArea(world, x, y, z, x + 3, y + 3, z + 1, Blocks.OBSIDIAN.defaultBlockState());
		fillArea(world, x + 1, y, z, x + 2, y + 3, z + 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
		if(big)
		{
			fillArea(world, x + 3, y, z, x + 4, y + 3, z + 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
			fillArea(world, x + 4, y, z, x + 5, y + 3, z + 1, Blocks.OBSIDIAN.defaultBlockState());
		}
	}
	
	private void generateWeapons(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x + 1, y, z, x + 3, y + 1, z + 2, Blocks.OAK_PLANKS.defaultBlockState());
		fillArea(world, x, y, z, x + 1, y + 1, z + 2, Blocks.CHEST.defaultBlockState());
		fillArea(world, x + 3, y, z, x + 4, y + 1, z + 2, Blocks.CHEST.defaultBlockState());

		fillArea(world, x + 1, y + 1, z, x + 3, y + 2, z + 1, FlansModApocalypse.gunRack.defaultBlockState().setValue(BlockItemHolder.FACING, Direction.SOUTH));
		fillArea(world, x + 1, y + 1, z + 1, x + 3, y + 2, z + 2, FlansModApocalypse.gunRack.defaultBlockState().setValue(BlockItemHolder.FACING, Direction.NORTH));
		
		for(int i = 0; i < 2; i++)
			for(int j = 0; j < 2; j++)
				if(rand.nextInt(3) != 0)
					FlansModApocalypse.getLootGenerator().addRandomLoot((TileEntityItemHolder)world.getBlockEntity(new BlockPos(x + 1 + i, y + 1, z + j)), rand, true);
		
		for(int i = 0; i < 2; i++)
		{
			FlansModApocalypse.getLootGenerator().fillWeaponChest(rand, ((ChestBlockEntity)world.getBlockEntity(new BlockPos(x, y, z + i))));
			FlansModApocalypse.getLootGenerator().fillWeaponChest(rand, ((ChestBlockEntity)world.getBlockEntity(new BlockPos(x + 3, y, z + i))));
		}
	}

	private void generateFurnace(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 1, y + 2, z + 2, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x + 3, y, z, x + 4, y + 2, z + 2, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x + 1, y, z + 2, x + 3, y + 1, z + 3, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x + 1, y + 2, z, x + 3, y + 3, z + 2, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x + 1, y + 3, z, x + 3, y + 5, z + 1, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x + 1, y, z, x + 3, y + 1, z + 2, Blocks.LAVA.defaultBlockState());
	}
	
	private void generatePlantPots(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 6, y + 1, z + 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
		fillArea(world, x + 1, y, z, x + 5, y + 1, z + 1, Blocks.STONE_SLAB.defaultBlockState());
		for(int i = 0; i < 6; i++)
			world.setBlockAndUpdate(new BlockPos(x + i, y + 1, z), Blocks.FLOWER_POT.defaultBlockState());
		
		fillArea(world, x, y, z + 5, x + 6, y + 1, z + 6, Blocks.QUARTZ_BLOCK.defaultBlockState());
		fillArea(world, x + 1, y, z + 5, x + 5, y + 1, z + 6, Blocks.STONE_SLAB.defaultBlockState());
		for(int i = 0; i < 6; i++)
			world.setBlockAndUpdate(new BlockPos(x + i, y + 1, z + 5), Blocks.FLOWER_POT.defaultBlockState());
		
	}
	
	private void generateFarm(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 7, y + 1, z + 7, FlansModApocalypse.blockLabStone.defaultBlockState());
		
		if(world.isClientSide())
		{
			for(int i = 0; i < 2; i++)
			{
				for(int j = 0; j < 2; j++)
				{
					world.setBlockAndUpdate(new BlockPos(x + 1 + 4 * i, y + 2, z + 1 + 4 * j), Blocks.GLOWSTONE.defaultBlockState());
					world.setBlockAndUpdate(new BlockPos(x + 1 + 4 * i, y + 3, z + 1 + 4 * j), Blocks.OAK_FENCE.defaultBlockState());
					world.setBlockAndUpdate(new BlockPos(x + 1 + 4 * i, y + 4, z + 1 + 4 * j), Blocks.OAK_FENCE.defaultBlockState());
				}
			}
		}
		
		fillArea(world, x + 1, y, z + 1, x + 6, y + 1, z + 6, Blocks.FARMLAND.defaultBlockState());
		switch(rand.nextInt(3))
		{
			case 0: fillArea(world, x + 1, y + 1, z + 1, x + 6, y + 2, z + 6, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, rand.nextInt(5) + 2));
				break;
			case 1: fillArea(world, x + 1, y + 1, z + 1, x + 6, y + 2, z + 6, Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, rand.nextInt(5) + 2));
				break;
			case 2: fillArea(world, x + 1, y + 1, z + 1, x + 6, y + 2, z + 6, Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, rand.nextInt(5) + 2));
				break;
		}
		world.setBlockAndUpdate(new BlockPos(x + 3, y + 1, z + 3), Blocks.AIR.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 3, y, z + 3), Blocks.WATER.defaultBlockState());
	}
	
	private void generateGunRack(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 2, y + 1, z + 1, Blocks.OAK_PLANKS.defaultBlockState());
		fillArea(world, x, y + 1, z, x + 2, y + 2, z + 1, FlansModApocalypse.gunRack.defaultBlockState().setValue(BlockItemHolder.FACING, Direction.SOUTH));
		for(int i = 0; i < 2; i++)
		{
			if(rand.nextInt(3) != 0)
				FlansModApocalypse.getLootGenerator().addRandomLoot((TileEntityItemHolder)world.getBlockEntity(new BlockPos(x + i, y + 1, z)), rand, true);
		}
	}
	
	private void generateTarget(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x + 1, y + 1, z, x + 4, y + 4, z + 1, Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
		world.setBlockAndUpdate(new BlockPos(x + 2, y, z), Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
		world.setBlockAndUpdate(new BlockPos(x + 2, y + 4, z), Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
		world.setBlockAndUpdate(new BlockPos(x, y + 2, z), Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
		world.setBlockAndUpdate(new BlockPos(x + 4, y + 2, z), Blocks.WHITE_WOOL.defaultBlockState()); // TODO APOCALYPSE: wool colour not ported
		world.setBlockAndUpdate(new BlockPos(x + 2, y + 1, z), Blocks.WHITE_WOOL.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 2, y + 3, z), Blocks.WHITE_WOOL.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 1, y + 2, z), Blocks.WHITE_WOOL.defaultBlockState());
		world.setBlockAndUpdate(new BlockPos(x + 3, y + 2, z), Blocks.WHITE_WOOL.defaultBlockState());
	}
	
	private void generateLiquidsLab(Level world, Random rand, int x, int y, int z)
	{
		fillArea(world, x, y, z, x + 4, y + 1, z + 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
		fillArea(world, x + 1, y, z, x + 3, y + 1, z + 1, Blocks.STONE_SLAB.defaultBlockState());
		
		fillArea(world, x, y, z + 5, x + 5, y + 1, z + 6, Blocks.QUARTZ_BLOCK.defaultBlockState());
		fillArea(world, x + 1, y, z + 5, x + 4, y + 1, z + 6, Blocks.STONE_SLAB.defaultBlockState());
		
		world.setBlockAndUpdate(new BlockPos(x + 5, y, z + 5), Blocks.CAULDRON.defaultBlockState());
		
		world.setBlockAndUpdate(new BlockPos(x + 4, y, z), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		world.setBlockAndUpdate(new BlockPos(x + 5, y, z), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		
		//Fill chests
		BlockEntity tileentity = world.getBlockEntity(new BlockPos(x + 4, y, z));
		if(tileentity instanceof ChestBlockEntity)
		{
			FlansModApocalypse.getLootGenerator().fillLiquidLabChest(rand, (ChestBlockEntity)tileentity);
		}

		tileentity = world.getBlockEntity(new BlockPos(x + 5, y, z));
		if(tileentity instanceof ChestBlockEntity)
		{
			FlansModApocalypse.getLootGenerator().fillLiquidLabChest(rand, (ChestBlockEntity)tileentity);
		}
		
		//Brewing stands
		BlockPos pos = new BlockPos(x + rand.nextInt(4), y + 1, z);
		world.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
		tileentity = world.getBlockEntity(pos);
		if(tileentity instanceof BrewingStandBlockEntity)
		{
			FlansModApocalypse.getLootGenerator().fillBrewingStand(rand, (BrewingStandBlockEntity)tileentity);
		}
		
		pos = new BlockPos(x + rand.nextInt(5), y + 1, z + 5);
		world.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
		tileentity = world.getBlockEntity(pos);
		if(tileentity instanceof BrewingStandBlockEntity)
		{
			FlansModApocalypse.getLootGenerator().fillBrewingStand(rand, (BrewingStandBlockEntity)tileentity);
		}
	}
	
	private void generateLiquidContainer(Level world, Random rand, int x, int y, int z, BlockState liquid)
	{
		fillArea(world, x, y, z, x + 4, y + 5, z + 4, FlansModApocalypse.blockLabStone.defaultBlockState());
		fillArea(world, x, y + 1, z, x + 4, y + 4, z + 4, Blocks.GLASS.defaultBlockState());
		
		fillArea(world, x + 1, y, z + 1, x + 3, y + 5, z + 3, Blocks.AIR.defaultBlockState());
		fillArea(world, x + 1, y, z + 1, x + 3, y + rand.nextInt(4), z + 3, liquid);
		
		fillArea(world, x, y, z, x + 1, y + 5, z + 1, Blocks.AIR.defaultBlockState());
		fillArea(world, x + 3, y, z, x + 4, y + 5, z + 1, Blocks.AIR.defaultBlockState());
		fillArea(world, x + 3, y, z + 3, x + 4, y + 5, z + 4, Blocks.AIR.defaultBlockState());
		fillArea(world, x, y, z + 3, x + 1, y + 5, z + 4, Blocks.AIR.defaultBlockState());
	}
	
	private BlockState getRandomLiquid(Random rand)
	{
		switch(rand.nextInt(3))
		{
			case 0: return Blocks.WATER.defaultBlockState();
			case 1: return Blocks.LAVA.defaultBlockState();
			case 2: return FlansModApocalypse.blockSulphuricAcid.defaultBlockState();
		}
		
		return Blocks.WATER.defaultBlockState();
	}
	
	private void generateRoom(Level world, Random rand, int chunkX, int layerY, int chunkZ)
	{
		
	}
}
