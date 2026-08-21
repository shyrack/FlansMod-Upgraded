package com.flansmod.common.paintjob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModBlockEntities;
import com.flansmod.common.PlayerData;
import com.flansmod.common.util.ItemStackUtil;

public class BlockPaintjobTable extends BaseEntityBlock
{
	public static final MapCodec<BlockPaintjobTable> CODEC = simpleCodec(BlockPaintjobTable::new);
	
	public BlockPaintjobTable(BlockBehaviour.Properties properties)
	{
		super(properties);
	}
	
	public BlockPaintjobTable()
	{
		this(Block.Properties.of().mapColor(MapColor.WOOD).strength(2F, 4F).pushReaction(PushReaction.BLOCK));
	}
	
	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TileEntityPaintjobTable(pos, state);
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(world.isClientSide())
		{
			PlayerData data = FlansMod.playerHandler.getPlayerData(player);
			if(data != null)
				data.shootTimeLeft = data.shootTimeRight = 10;
			return InteractionResult.SUCCESS;
		}
		
		TileEntityPaintjobTable table = (TileEntityPaintjobTable)world.getBlockEntity(pos);
		
		if(!world.isClientSide() && table != null)
		{
			FlansMod.proxy.openPaintjobTable(player.getInventory(), world, table);
		}
		return InteractionResult.SUCCESS;
	}
	
	@Override
	public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player)
	{
		if(!state.isAir())
		{
			BlockEntity tileentity = worldIn.getBlockEntity(pos);
			if(tileentity instanceof Container)
			{
				for(int i = 0; i < ((Container)tileentity).getContainerSize(); i++)
				{
					ItemStack stack = ((Container)tileentity).getItem(i);
					if(!stack.isEmpty())
						popResource(worldIn, pos, stack);
				}
				worldIn.updateNeighbourForOutputSignal(pos, this);
			}
		}
		return super.playerWillDestroy(worldIn, pos, state, player);
	}
}
