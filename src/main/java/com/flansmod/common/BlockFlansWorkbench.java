package com.flansmod.common;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import com.flansmod.common.guns.ContainerGunModTable;

public class BlockFlansWorkbench extends Block
{
	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 2);
	
	public BlockFlansWorkbench()
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 6F));
	}
	
	public BlockFlansWorkbench(BlockBehaviour.Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, 0));
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		switch(state.getValue(TYPE))
		{
			case 0:
				if(world.isClientSide())
					FlansMod.proxy.openWorkbenchCrafting(player.getInventory());
				break;
			case 1:
				if(!world.isClientSide())
					FlansMod.proxy.openGunModTable(player.getInventory(), world);
				break;
		}
		return InteractionResult.SUCCESS;
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(TYPE);
	}
}
