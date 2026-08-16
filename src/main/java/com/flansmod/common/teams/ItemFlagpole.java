package com.flansmod.common.teams;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;

public class ItemFlagpole extends Item
{
	public ItemFlagpole()
	{
		this(new Item.Properties());
	}
	
	public ItemFlagpole(Item.Properties properties)
	{
		super(properties);
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}
	
	@Override
	public InteractionResult use(Level world, Player entityplayer, InteractionHand hand)
	{
		ItemStack itemstack = entityplayer.getItemInHand(hand);
		
		float f = 1.0F;
		HitResult HitResult = entityplayer.pick(5D, f, false);
		if(HitResult == null)
		{
			return InteractionResult.PASS;
		}
		if(HitResult.getType() == Type.BLOCK)
		{
			BlockPos pos = ((net.minecraft.world.phys.BlockHitResult)HitResult).getBlockPos();
			if(!world.isClientSide())
			{
				if(world.getBlockState(pos).getBlock() == Blocks.SNOW)
				{
					pos = pos.below();
				}
				if(isSolid(world, pos))
				{
					if(world instanceof ServerLevel)
						((ServerLevel)world).addFreshEntity(new EntityFlagpole(world, pos));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	
	private boolean isSolid(Level world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		if(state == null)
			return false;
		return state.isSolid() && state.canOcclude();
	}
}
