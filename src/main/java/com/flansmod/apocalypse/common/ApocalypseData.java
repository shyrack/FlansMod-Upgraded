package com.flansmod.apocalypse.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import com.flansmod.common.FlansMod;

public class ApocalypseData
{
	/**
	 * The point at which each player entered the apocalypse. For deciding where they should come out
	 */
	public HashMap<UUID, BlockPos> entryPoints = new HashMap<>();

	public void savePerWorldData(ServerLevel world)
	{
		if(world.dimension() != Level.OVERWORLD)
			return;
		try
		{
			//Make directory
			File dir = world.getServer().getWorldPath(LevelResource.ROOT).resolve("apocalypse").toFile();
			if(!dir.exists())
				dir.mkdirs();
			
			//Save per-world file
			File file = new File(dir, "apocalypse.dat");
			if(!file.exists())
				file.createNewFile();

			NbtIo.write(new CompoundTag(), file.toPath());
			
			//Save per-player file
			for(Map.Entry<UUID, BlockPos> uuidBlockPosEntry : entryPoints.entrySet())
			{
				UUID uuid = (uuidBlockPosEntry).getKey();
				File playerFile = new File(dir, uuid.toString() + ".dat");
				CompoundTag playerTags = new CompoundTag();
				if(!playerFile.exists())
					playerFile.createNewFile();
				
				BlockPos pos = entryPoints.get(uuid);
				playerTags.putIntArray("EntryPoint", new int[]{pos.getX(), pos.getY(), pos.getZ()});
				
				NbtIo.write(playerTags, playerFile.toPath());
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Failed to save apocalypse data", e);
		}
	}

	public void loadPerWorldData(ServerLevel world)
	{
		if(world.dimension() != Level.OVERWORLD)
			return;
		try
		{
			//Make directory
			File dir = world.getServer().getWorldPath(LevelResource.ROOT).resolve("apocalypse").toFile();
			if(!dir.exists())
				return;
			
			//Load per-player file
			for(File playerFile : dir.listFiles())
			{
				if(playerFile.getName().equals("apocalypse.dat"))
					continue;
				UUID uuid = UUID.fromString(playerFile.getName().split("\\.")[0]);
				CompoundTag playerTags = NbtIo.read(playerFile.toPath());
				int[] entryPoint = playerTags.getIntArray("EntryPoint").orElse(new int[0]);
				if(entryPoint.length == 3)
					entryPoints.put(uuid, new BlockPos(entryPoint[0], entryPoint[1], entryPoint[2]));
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Failed to load apocalypse data", e);
		}
	}
	
	
}
