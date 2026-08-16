package com.flansmod.apocalypse.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.entity.EntityTeleporter;
import com.flansmod.apocalypse.common.world.buildings.WorldGenBossPillar;

public class BlockPowerCube extends Block implements EntityBlock
{
	public BlockPowerCube()
	{
		this(BlockBehaviour.Properties.of().noOcclusion().strength(3F, 5F));
	}

	public BlockPowerCube(BlockBehaviour.Properties properties)
	{
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new TileEntityPowerCube(pos, state);
	}

	@Override
	public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos)
	{
		return world.getBlockState(pos.below()).isSolid() || world.getBlockState(pos.below()).canOcclude();
	}

	@Override
	public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context)
	{
		return Shapes.block();
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if(!world.isClientSide())
		{
			for(int i = 0; i < 2; i++)
			{
				for(int j = 0; j < 2; j++)
				{
					if((world.dimension() == FlansModApocalypse.APOCALYPSE_DIMENSION_KEY || world.dimension() == Level.OVERWORLD) && isPortal(world, pos.offset(-3 * i, 0, -3 * j)))
					{
						((ServerLevel)world).addFreshEntity(new EntityTeleporter(world, pos.offset(-3 * i, 0, -3 * j)));
					}
				}
			}
			
			final int checkY = Mth.floor(WorldGenBossPillar.kPillarMaxHeight + 1);
			final int checkXZ = Mth.floor(WorldGenBossPillar.kPillarInnerEdge + 1);
			
			if(world.dimension() == FlansModApocalypse.APOCALYPSE_DIMENSION_KEY &&
			   world.getBlockState(pos.below()).getBlock() == Blocks.BEDROCK)
			{
				if(Math.abs(pos.getX()) == checkXZ &&
				   Math.abs(pos.getZ()) == checkXZ)
				{
					boolean allPresent = true;
							
					for(int i = 0; i < 2; i++)
						for(int k = 0; k < 2; k++)
							if(world.getBlockState(new BlockPos(checkXZ * (i == 0 ? 1 : -1), pos.getY(), checkXZ * (k == 0 ? 1 : -1))).getBlock() != this)
								allPresent = false;
					
					if(allPresent)
					{
						FlansModApocalypse.INSTANCE.TriggerBossFight(world, placer);
						
						for(int i = 0; i < 2; i++)
							for(int k = 0; k < 2; k++)
								world.destroyBlock(new BlockPos(checkXZ * (i == 0 ? 1 : -1), pos.getY(), checkXZ * (k == 0 ? 1 : -1)), false);
					}
				}
			}
		}
	}
	
	private boolean isPortal(Level world, BlockPos pos)
	{
		if(world.getBlockState(pos).getBlock() != FlansModApocalypse.blockPowerCube || world.getBlockState(pos.offset(3, 0, 0)).getBlock() != FlansModApocalypse.blockPowerCube
				|| world.getBlockState(pos.offset(0, 0, 3)).getBlock() != FlansModApocalypse.blockPowerCube || world.getBlockState(pos.offset(3, 0, 3)).getBlock() != FlansModApocalypse.blockPowerCube)
			return false;
		for(int i = 0; i < 2; i++)
			for(int j = 0; j < 2; j++)
				if(world.getBlockState(pos.offset(i * 3, -1, j * 3)).getBlock() != Blocks.OBSIDIAN || world.getBlockState(pos.offset(1 + i, -1, 1 + j)).getBlock() != Blocks.OBSIDIAN)
					return false;
		return true;
	}
}
