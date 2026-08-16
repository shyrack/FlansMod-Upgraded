package com.flansmod.common.teams;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.network.PacketBase;
import com.flansmod.common.types.InfoType;

public abstract class Gametype
{
	public static HashMap<String, Gametype> gametypes = new HashMap<>();
	public static TeamsManager teamsManager = TeamsManager.getInstance();
	public static Random rand = new Random();
	
	public static Gametype getGametype(String type)
	{
		return gametypes.get(type);
	}
	
	public String name;
	public String shortName;
	public int numTeamsRequired;
	
	public Gametype(String s, String s1, int numTeams)
	{
		name = s;
		shortName = s1;
		numTeamsRequired = numTeams;
		gametypes.put(shortName, this);
	}
	
	/**
	 * Called when a round starts
	 */
	public abstract void roundStart();
	
	/**
	 * Called when a round ends. (The point at which scoreboards are displayed)
	 */
	public abstract void roundEnd();
	
	/**
	 * Called when the scoreboards and voting are finished
	 */
	public abstract void roundCleanup();
	
	public abstract boolean teamHasWon(Team team);
	
	public void tick()
	{
	}
	
	public Team[] getTeamsCanSpawnAs(TeamsRound currentRound, Player player)
	{
		return currentRound.teams;
	}
	
	public void playerJoined(ServerPlayer player)
	{
	}
	
	public void playerRespawned(ServerPlayer player)
	{
	}
	
	public void playerQuit(ServerPlayer player)
	{
	}
	
	//Return true if damage should be dealt.
	public boolean playerAttacked(ServerPlayer player, DamageSource source)
	{
		return true;
	}
	
	public void playerKilled(ServerPlayer player, DamageSource source)
	{
	}
	
	public void baseAttacked(ITeamBase base, DamageSource source)
	{
	}
	
	public void objectAttacked(ITeamObject object, DamageSource source)
	{
	}
	
	public void baseClickedByPlayer(ITeamBase base, ServerPlayer player)
	{
	}
	
	public void objectClickedByPlayer(ITeamObject object, ServerPlayer player)
	{
	}
	
	public boolean playerCanLoot(ItemStack stack, InfoType infoType, Player player, Team playerTeam)
	{
		return true;
	}
	
	public abstract Vec3 getSpawnPoint(ServerPlayer player);
	
	//Return whether or not the variable exists
	public boolean setVariable(String variable, String value)
	{
		return false;
	}
	
	public abstract void readFromNBT(CompoundTag tags);
	
	public abstract void saveToNBT(CompoundTag tags);
	
	public boolean sortScoreboardByTeam()
	{
		return true;
	}
	
	public boolean showZombieScore()
	{
		return false;
	}
	
	/**
	 * Whether "attacker" can attack "victim"
	 */
	public boolean playerCanAttack(ServerPlayer attacker, Team attackerTeam, ServerPlayer victim, Team victimTeam)
	{
		return true;
	}
	
	/**
	 * Called when any entity is killed. This allows one to track mob deaths too
	 */
	public void entityKilled(Entity entity, DamageSource source)
	{
	}
	
	public void playerChoseTeam(ServerPlayer player, Team team, Team newTeam)
	{
	}
	
	public void playerChoseNewClass(ServerPlayer player, IPlayerClass playerClass)
	{
	}
	
	public void playerDefected(ServerPlayer player, Team team, Team newTeam)
	{
	}
	
	public void playerEnteredTheGame(ServerPlayer player, Team team, IPlayerClass playerClass)
	{
	}
	
	//--------------------------------------
	// Helper methods - Do not override
	//--------------------------------------
	
	public ServerPlayer getPlayer(String username)
	{
		return FlansMod.serverInstance == null ? null : FlansMod.serverInstance.getPlayerList().getPlayerByName(username);
	}
	
	public static PlayerData getPlayerData(ServerPlayer player)
	{
		return PlayerHandler.getPlayerData(player);
	}
	
	public static void sendPacketToPlayer(PacketBase packet, ServerPlayer player)
	{
		FlansMod.getPacketHandler().sendTo(packet, player);
	}
	
	public static String[] getPlayerNames()
	{
		if(FlansMod.serverInstance == null)
			return new String[0];
		List<ServerPlayer> players = FlansMod.serverInstance.getPlayerList().getPlayers();
		String[] names = new String[players.size()];
		for(int i = 0; i < players.size(); i++)
			names[i] = players.get(i).getName().getString();
		return names;
	}
	
	public static List<ServerPlayer> getPlayers()
	{
		return FlansMod.serverInstance == null ? java.util.Collections.emptyList() : FlansMod.serverInstance.getPlayerList().getPlayers();
	}
	
	public static void givePoints(ServerPlayer player, int points)
	{
		PlayerData data = getPlayerData(player);
		data.score += points;
		if(data.team != null)
			data.team.score += points;
	}
	
	public static ServerPlayer getPlayerFromDamageSource(DamageSource source)
	{
		ServerPlayer attacker = null;
		if(source.getEntity() instanceof ServerPlayer)
			attacker = (ServerPlayer)source.getEntity();
		return attacker;
	}
	
	public boolean shouldAutobalance()
	{
		return true;
	}
}
