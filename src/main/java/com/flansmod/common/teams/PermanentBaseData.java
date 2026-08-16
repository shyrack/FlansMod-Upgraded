package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

public class PermanentBaseData
{
	public int baseID = 0;
	public List<BlockPos> spawnPoints = new ArrayList<>();
	public int teamID = 0;
	
	public void writeBaseToNBT(CompoundTag tags)
	{
		tags.putInt("NumObjects", spawnPoints.size());
		for(int i = 0; i < spawnPoints.size(); i++)
		{
			BlockPos pos = spawnPoints.get(i);
			CompoundTag objectTags = new CompoundTag();
			objectTags.putDouble("x", pos.getX());
			objectTags.putDouble("y", pos.getY());
			objectTags.putDouble("z", pos.getZ());
			tags.put("SP_" + i, objectTags);
		}
	}
	
	public void readBaseFromNBT(CompoundTag tags)
	{
		int iNumObj = tags.getIntOr("NumObjects", 0);
		for(int i = 0; i < iNumObj; i++)
		{
			CompoundTag objectTags = tags.getCompoundOrEmpty("SP_" + i);
			spawnPoints.add(BlockPos.containing(objectTags.getDoubleOr("x", 0D), objectTags.getDoubleOr("y", 0D), objectTags.getDoubleOr("z", 0D)));
		}
	}
	
	public void addObject(ITeamObject object)
	{
		BlockPos objPos = BlockPos.containing(object.getPosX(), object.getPosY(), object.getPosZ());
		for(BlockPos pos : spawnPoints)
		{
			if(pos.equals(objPos))
				return;
		}
		spawnPoints.add(objPos);
	}
	
}
