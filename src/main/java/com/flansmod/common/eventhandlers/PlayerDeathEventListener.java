package com.flansmod.common.eventhandlers;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.guns.EntityDamageSourceFlan;
import com.flansmod.common.network.PacketKillMessage;
import com.flansmod.common.teams.Team;

public class PlayerDeathEventListener
{
	public PlayerDeathEventListener()
	{
		ServerLivingEntityEvents.AFTER_DEATH.register(this::PlayerDied);
	}
	
	public void PlayerDied(LivingEntity entity, DamageSource source)
	{
		if(source instanceof EntityDamageSourceFlan && entity instanceof Player)
		{
			EntityDamageSourceFlan flanSource = (EntityDamageSourceFlan)source;
			Player died = (Player)entity;
			
			Team killedTeam = PlayerHandler.getPlayerData(died).team;
			if(flanSource.getCausedPlayer() != null)
			{
				Team killerTeam = PlayerHandler.getPlayerData(flanSource.getCausedPlayer()).team;
				
				ResourceKey<Level> dimension = died.level().dimension();
				int dimensionID = dimension == Level.NETHER ? -1 : (dimension == Level.END ? 1 : 0);
				FlansMod.getPacketHandler().sendToDimension(new PacketKillMessage(flanSource.isHeadshot(), flanSource.getWeapon(),
						(killedTeam == null ? "f" : String.valueOf(killedTeam.textColour)) + died.getName().getString(),
						(killerTeam == null ? "f" : String.valueOf(killerTeam.textColour)) + flanSource.getCausedPlayer().getName().getString()), dimensionID);
			}
		}
	}
}
