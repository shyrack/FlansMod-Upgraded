package com.flansmod.common.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class BlockUtil
{
	public static boolean destroyBlock(ServerLevel world, BlockPos pos, Entity entity, boolean dropBlock)
	{
		world.destroyBlock(pos, dropBlock);
		return true;
	}
}
