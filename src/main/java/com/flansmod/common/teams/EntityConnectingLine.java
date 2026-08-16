package com.flansmod.common.teams;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityConnectingLine extends FishingHook
{
	
	public Object connectedTo;
	public Player player;
	
	public EntityConnectingLine(Level world, Player player)
	{
		super(player, world, 0, 0);
		this.player = player;
		player.fishing = this;
	}
	
	public EntityConnectingLine(Level world, Player player, ITeamBase base)
	{
		this(world, player);
		connectedTo = base;

		setPos(base.getPosX(), base.getPosY(), base.getPosZ());
		setDeltaMovement(0, 0, 0);
	}
	
	public EntityConnectingLine(Level world, Player player, ITeamObject object)
	{
		this(world, player);
		connectedTo = object;

		setPos(object.getPosX(), object.getPosY(), object.getPosZ());
		setDeltaMovement(0, 0, 0);
	}
	
	@Override
	public void tick()
	{
		ItemStack currentItemstack = player.getMainHandItem();
		if(currentItemstack == null || !(currentItemstack.getItem() instanceof ItemOpStick) || currentItemstack.getDamageValue() != 1)
		{
			discard();
			player.fishing = null;
		}
	}
	
}
