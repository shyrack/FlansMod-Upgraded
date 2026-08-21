package com.flansmod.common.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.network.PacketTeamSelect;

public class TeamsManagerClassic extends TeamsManager
{
	private static TeamsManagerClassic INSTANCE;
	
	public static TeamsManagerClassic GetInstance()
	{
		return INSTANCE;
	}
	
	public TeamsManagerClassic()
	{
		super();
		INSTANCE = this;
	}
	
	@Override
	protected void OnRoundEnded()
	{
		super.OnRoundEnded();
	}
	
	@Override
	public void onPlayerLogout(Player player)
	{
		super.onPlayerLogout(player);
	}
	
	@Override
	public void OnPlayerKilled(ServerPlayer victim, DamageSource source)
	{
		super.OnPlayerKilled(victim, source);
		
	}
	
	@Override
	public void startRound()
	{
		super.startRound();
	}
	
	@Override
	public void tick()
	{
		super.tick();
	}
	
	@Override
	public void onPlayerLogin(Player player)
	{
		if(!enabled || currentRound == null)
			return;
		
		if(player instanceof ServerPlayer)
		{
			ServerPlayer playerMP = (ServerPlayer)player;
			sendTeamsMenuToPlayer(playerMP);
			currentRound.gametype.playerJoined(playerMP);
		}
	}
	
	@Override
	public void showTeamsMenuToAll(boolean info)
	{
		for(Player player : getPlayers())
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			//Catch for broken player data
			if(data == null)
				continue;
			//Catch for people not on a team, such as builders
			if(data.builder && playerIsOp(player))
				continue;
			
			sendTeamsMenuToPlayer((ServerPlayer)player, info);
		}
	}
	
	@Override
	public void sendTeamsMenuToPlayer(ServerPlayer player, boolean info)
	{
		if(!enabled || currentRound == null || currentRound.teams == null)
			return;
		//Get the available teams from the gametype
		Team[] availableTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
		//Add in the spectators as an option and "none" if the player is an op
		boolean playerIsOp = FlansMod.serverInstance.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
		Team[] allAvailableTeams = new Team[availableTeams.length + (playerIsOp ? 2 : 1)];
		System.arraycopy(availableTeams, 0, allAvailableTeams, 0, availableTeams.length);
		allAvailableTeams[availableTeams.length] = Team.spectators;
		
		sendPacketToPlayer(new PacketTeamSelect(allAvailableTeams, info), player);
	}
	
	@Override
	public void sendClassMenuToPlayer(ServerPlayer player)
	{
		Team team = PlayerHandler.getPlayerData(player).newTeam;
		if(team == null)
		{
			sendTeamsMenuToPlayer(player);
		}
		else if(team != Team.spectators && team.classes.size() > 0)
		{
			sendPacketToPlayer(new PacketTeamSelect(team.classes.toArray(new PlayerClass[team.classes.size()])), player);
		}
	}
	
	@Override
	protected void ReadFromNBT(CompoundTag tags, Level world)
	{
		super.ReadFromNBT(tags, world);
	}
	
	@Override
	protected void WriteToNBT(CompoundTag tags)
	{
		super.WriteToNBT(tags);
	}
	
	@Override
	public void playerSelectedClass(ServerPlayer player, String className)
	{
		if(!enabled || currentRound == null)
			return;
		
		//Get player class requested
		PlayerClass playerClass = PlayerClass.getClass(className);
		PlayerData data = PlayerHandler.getPlayerData(player);
		
		//Validate class
		if(!data.newTeam.classes.contains(playerClass))
		{
			player.sendSystemMessage(Component.literal("You may not select " + playerClass.name + ". Please try again"));
			FlansMod.log.warn(player.getName() + " tried to pick an invalid class : " + playerClass.name);
			//sendClassMenuToPlayer(player);
			return;
		}
		
		playerSelectedClass(player, playerClass);
	}
	
	@Override
	public void SelectTeam(Team team)
	{
		FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(team == null ? "null" : team.shortName, false));
		FlansMod.proxy.closeScreen();
	}
}
