package com.flansmod.apocalypse.common.blocks;

import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

import com.flansmod.apocalypse.common.FlansModApocalypse;

public class BlockSulphur extends Block
{
	public BlockSulphur()
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.SAND).strength(0.5F));
	}

	public BlockSulphur(BlockBehaviour.Properties properties)
	{
		super(properties);
	}

	// TODO APOCALYPSE: 1.12.2 dropped sulphur directly; kept via custom drops list
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
	{
		return List.of(new ItemStack(FlansModApocalypse.sulphur));
	}
}
