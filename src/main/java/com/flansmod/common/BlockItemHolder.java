package com.flansmod.common;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockItemHolder extends BaseEntityBlock
{
	public static final MapCodec<BlockItemHolder> CODEC = simpleCodec(BlockItemHolder::new);
	public ItemHolderType type;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	
	public BlockItemHolder(BlockBehaviour.Properties properties)
	{
		super(properties);
	}
	
	public BlockItemHolder(ItemHolderType type)
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2F, 4F), type);
	}

	public BlockItemHolder(BlockBehaviour.Properties properties, ItemHolderType type)
	{
		super(properties);
		this.type = type;
		type.block = this;
	}
	
	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}
	
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		Direction enumfacing = Direction.fromYRot((double)placer.getYRot());
		worldIn.setBlock(pos, state.setValue(FACING, enumfacing), 2);
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING);
	}
	
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
	{
		return level.getBlockState(pos.below()).isSolid();
	}
	
	protected static final VoxelShape AABB = Shapes.create(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context)
	{
		return AABB;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		TileEntityItemHolder tileEntity = new TileEntityItemHolder(pos, state);
		tileEntity.type = type;
		return tileEntity;
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(world.isClientSide())
		{
			FlansMod.playerHandler.getPlayerData(player, true).shootTimeLeft = FlansMod.playerHandler.getPlayerData(player, true).shootTimeRight = 10;
			return InteractionResult.SUCCESS;
		}
		
		TileEntityItemHolder holder = (TileEntityItemHolder)world.getBlockEntity(pos);
		ItemStack item = player.getMainHandItem();
		
		if(holder.getItem(0).isEmpty())
		{
			holder.setItem(0, item);
			player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY.copy());
		}
		else
		{
			((ServerLevel)world).addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), holder.getItem(0)));
			holder.setItem(0, ItemStack.EMPTY.copy());
			FlansMod.playerHandler.getPlayerData(player, false).shootTimeLeft = FlansMod.playerHandler.getPlayerData(player, false).shootTimeRight = 10;
		}
		
		return InteractionResult.SUCCESS;
	}
	
	@Override
	public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player)
	{
		if(!state.isAir())
		{
			BlockEntity tileentity = worldIn.getBlockEntity(pos);
			
			if(tileentity instanceof TileEntityItemHolder)
			{
				Containers.dropContents(worldIn, pos, (TileEntityItemHolder)tileentity);
			}
		}
		return super.playerWillDestroy(worldIn, pos, state, player);
	}
}
