package com.flansmod.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class BlockGunpowder extends Block
{
	public BlockGunpowder()
	{
		this(Block.Properties.of());
	}

	public BlockGunpowder(BlockBehaviour.Properties properties)
	{
		super(properties);
	}
}
