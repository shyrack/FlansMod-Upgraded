package com.flansmod.apocalypse.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.flansmod.apocalypse.common.FlansModApocalypse;

public class TileEntityPowerCube extends BlockEntity
{
	public int age;
	
	public TileEntityPowerCube(BlockPos pos, BlockState state)
	{
		super(FlansModApocalypse.POWER_CUBE_BE, pos, state);
	}
	
	public void tick()
	{
		age++;
	}
}
