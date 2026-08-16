package com.flansmod.common.teams;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModBlockEntities;

public class BlockSpawner extends BaseEntityBlock
{
	public static final MapCodec<BlockSpawner> CODEC = simpleCodec(BlockSpawner::new);
	public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 2);
	protected static final VoxelShape CARPET_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
	public static boolean colouredPass = false;
	
	public BlockSpawner(BlockBehaviour.Properties properties)
	{
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TYPE, 0));
	}
	
	public BlockSpawner()
	{
		this(Block.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1F).pushReaction(PushReaction.BLOCK));
	}
	
	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return CODEC;
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return CARPET_AABB;
	}
	
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
	{
		return level.getBlockState(pos.below()).isSolid();
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TileEntitySpawner(pos, state);
	}
	
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
	{
		return type == ModBlockEntities.SPAWNER ? (level1, pos1, state1, be) -> TileEntitySpawner.tick(level1, pos1, state1, (TileEntitySpawner)be) : null;
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(world.isClientSide())
			return InteractionResult.SUCCESS;
		if(player instanceof ServerPlayer)
			TeamsManager.getInstance().playerInteracted((ServerPlayer)player, pos);
		TileEntitySpawner spawner = (TileEntitySpawner)world.getBlockEntity(pos);
		if(spawner != null && FlansMod.serverInstance.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile())))
		{
			ItemStack item = player.getMainHandItem();
			if(item.isEmpty())
			{
				spawner.spawnDelay = (spawner.spawnDelay + 200) % 6000;
				player.sendSystemMessage(Component.literal("Set spawn delay to " + spawner.spawnDelay / 20));
			}
			else if(!(item.getItem() instanceof ItemOpStick))
			{
				spawner.stacksToSpawn.add(item.copy());
				for(Entity entity : spawner.itemEntities)
				{
					entity.discard();
				}
				spawner.currentDelay = 10;
			}
		}
		return InteractionResult.SUCCESS;
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(TYPE);
	}
}
