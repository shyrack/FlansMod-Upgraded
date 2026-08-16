package com.flansmod.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.teams.TeamsManager;

public class PlayerHandler
{
	public static Map<String, PlayerData> serverSideData = new HashMap<>();
	public static Map<String, PlayerData> clientSideData = new HashMap<>();
	public static ArrayList<String> clientsToRemoveAfterThisRound = new ArrayList<>();
	
	/**
	 * Kept for compatibility with existing code. The flight kick counter no longer
	 * needs to be reset as modern Minecraft does not kick players for flying in vehicles.
	 */
	public static java.lang.reflect.Field floatingTickCount = null;
	
	public PlayerHandler()
	{
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onEntityHurt);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onPlayerRespawn(newPlayer));
	}
	
	public boolean onEntityHurt(LivingEntity entity, DamageSource source, float amount)
	{
		if(entity.getVehicle() instanceof EntityDriveable || entity.getVehicle() instanceof EntitySeat)
		{
			//TODO Set Drivable damage
			return false;
		}
		return true;
	}
	
	public void onLivingDeath(LivingEntity entity, DamageSource source)
	{
		if(entity instanceof Player)
		{
			getPlayerData((Player)entity).playerKilled();
		}
	}
	
	public void serverTick()
	{
		if(FlansMod.serverInstance == null)
		{
			FlansMod.log.warn("Receiving server ticks when server is null");
			return;
		}
		for(ServerLevel world : FlansMod.serverInstance.getAllLevels())
		{
			for(Player player : world.players())
			{
				getPlayerData(player).tick(player);
			}
		}
	}
	
	public void clientTick()
	{
		if(Minecraft.getInstance().level != null)
		{
			for(Player player : Minecraft.getInstance().level.players())
			{
				getPlayerData(player).tick(player);
			}
		}
	}
	
	public static PlayerData getPlayerData(Player player)
	{
		if(player == null)
			return null;
		return getPlayerData(player.getGameProfile().name(), player.level().isClientSide());
	}
	
	public static PlayerData getPlayerData(String username)
	{
		return getPlayerData(username, false);
	}
	
	public static PlayerData getPlayerData(Player player, boolean clientSide)
	{
		if(player == null)
			return null;
		return getPlayerData(player.getGameProfile().name(), clientSide);
	}
	
	public static PlayerData getPlayerData(String username, boolean clientSide)
	{
		if(clientSide)
		{
			if(!clientSideData.containsKey(username))
				clientSideData.put(username, new PlayerData(username));
		}
		else
		{
			if(!serverSideData.containsKey(username))
				serverSideData.put(username, new PlayerData(username));
		}
		return clientSide ? clientSideData.get(username) : serverSideData.get(username);
	}
	
	public void playerLoggedIn(ServerPlayer player)
	{
		String username = player.getGameProfile().name();
		
		PlayerData data = new PlayerData(username);
		data.ReadFromFile();
		
		if(!serverSideData.containsKey(username))
			serverSideData.put(username, data);
		clientsToRemoveAfterThisRound.remove(username);
	}
	
	public void playerLoggedOut(ServerPlayer player)
	{
		String username = player.getGameProfile().name();
		
		clientsToRemoveAfterThisRound.add(username);
		
		if(TeamsManager.getInstance().currentRound == null)
		{
			roundEnded();
		}
	}
	
	public void onPlayerRespawn(ServerPlayer player)
	{
		String username = player.getGameProfile().name();
		if(!serverSideData.containsKey(username))
			serverSideData.put(username, new PlayerData(username));
	}
	
	/**
	 * Called by teams manager to remove lingering player data
	 */
	public static void roundEnded()
	{
		for(String username : clientsToRemoveAfterThisRound)
		{
			PlayerData data = serverSideData.get(username);
			if(data != null)
			{
				data.WriteToFile();
			}
			serverSideData.remove(username);
		}
	}
}
