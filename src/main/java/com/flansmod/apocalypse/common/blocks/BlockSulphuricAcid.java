package com.flansmod.apocalypse.common.blocks;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class BlockSulphuricAcid extends BlockStatic
{
	public BlockSulphuricAcid()
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().strength(100.0F));
	}

	public BlockSulphuricAcid(BlockBehaviour.Properties properties)
	{
		super(properties);
	}
	
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier insideBlockEffectApplier, boolean isStepOn)
	{
		DamageSource acidDamage = level.damageSources().magic();
		if(!level.isClientSide())
			entity.hurtServer((ServerLevel)level, acidDamage, 5.0F);
	}
}
