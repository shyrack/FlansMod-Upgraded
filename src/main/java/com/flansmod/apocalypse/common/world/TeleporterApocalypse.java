package com.flansmod.apocalypse.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class TeleporterApocalypse
{
	private ServerLevel world;
	private BlockPos targetTeleporter;
	
	public TeleporterApocalypse(ServerLevel world, BlockPos targetTeleporter)
	{
		this.world = world;
		this.targetTeleporter = targetTeleporter;
	}
	
	public BlockPos getTargetTeleporter()
	{
		return targetTeleporter;
	}
	
	public ServerLevel getWorld()
	{
		return world;
	}
	
	// TODO APOCALYPSE: 1.12.2 extended Teleporter (makePortal/placeInExistingPortal); dimension
	// transfer now uses ServerPlayer.teleport(TeleportTransition) directly
}
