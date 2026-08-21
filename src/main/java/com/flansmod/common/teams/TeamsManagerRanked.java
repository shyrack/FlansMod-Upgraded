package com.flansmod.common.teams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import com.flansmod.client.gui.teams.EnumLoadoutSlot;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.network.PacketLoadoutData;
import com.flansmod.common.network.PacketOpenRewardBox;
import com.flansmod.common.network.PacketRoundFinished;
import com.flansmod.common.network.PacketTeamSelect;
import com.flansmod.common.network.PacketVoting;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class TeamsManagerRanked extends TeamsManager
{
	public static HashMap<UUID, PlayerRankData> rankData = new HashMap<>();
	
	public LoadoutPool currentPool;
	
	public float XPMultiplier = 1.0f;
	
	public RoundFinishedData roundFinishedTemplateData = new RoundFinishedData();
	
	public static TeamsManagerRanked GetInstance()
	{
		return (TeamsManagerRanked)TeamsManager.instance;
	}
	
	public TeamsManagerRanked()
	{
		super();
	}
	
	@Override
	public void startRound()
	{
		if(currentPool == null)
		{
			
			return;
		}
		
		for(Player player : getPlayers())
		{
			ProcessRankData((ServerPlayer)player);
		}
		
		super.startRound();
		
		for(Player player : getPlayers())
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			if(data != null && !data.builder)
			{
				sendLoadoutData((ServerPlayer)player);
			}
		}
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(interRoundTimeLeft > 0 && time % 10 == 0
				&& roundFinishedTemplateData.votingOptions != null && roundFinishedTemplateData.votingOptions.length > 0)
		{
			for(int i = 0; i < roundFinishedTemplateData.votingOptions.length; i++)
			{
				roundFinishedTemplateData.votingOptions[i].numVotes = 0;
			}
			for(Player player : getPlayers())
			{
				PlayerData data = PlayerHandler.getPlayerData(player);
				if(!data.builder && data.vote != 0 && data.vote - 1 < roundFinishedTemplateData.votingOptions.length)
					roundFinishedTemplateData.votingOptions[data.vote - 1].numVotes++;
			}
			for(Player player : getPlayers())
			{
				PlayerData data = PlayerHandler.getPlayerData(player);
				if(!data.builder)
					sendPacketToPlayer(new PacketVoting(roundFinishedTemplateData), (ServerPlayer)player);
			}
		}
	}
	
	public void sendLoadoutData(ServerPlayer player)
	{
		PacketLoadoutData data = new PacketLoadoutData();
		
		//Get the available teams from the gametype
		Team[] availableTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
		//Add in the spectators as an option and "none" if the player is an op
		boolean playerIsOp = FlansMod.serverInstance.getPlayerList().isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
		Team[] allAvailableTeams = new Team[availableTeams.length + (playerIsOp ? 2 : 1)];
		System.arraycopy(availableTeams, 0, allAvailableTeams, 0, availableTeams.length);
		allAvailableTeams[availableTeams.length] = Team.spectators;
		
		data.teamsAvailable = allAvailableTeams;
		data.currentPool = currentPool;
		data.myRankData = rankData.get(player.getUUID());
		data.motd = motd;
		
		FlansMod.getPacketHandler().sendTo(data, player);
	}
	
	@Override
	public void onPlayerLogin(Player player)
	{
		if(!rankData.containsKey(player.getUUID()))
		{
			PlayerRankData data = new PlayerRankData();
			
			if(currentPool != null)
			{
				for(int i = 0; i < 5; i++)
				{
					for(int j = 0; j < EnumLoadoutSlot.values().length; j++)
					{
						if(currentPool.defaultLoadouts[i].slots[j] != null)
						{
							data.loadouts[i].slots[j] = currentPool.defaultLoadouts[i].slots[j].copy();
						}
					}
				}
			}
			
			rankData.put(player.getUUID(), data);
		}
		
		//super.onPlayerLogin(player);
		
		if(!enabled || currentRound == null)
			return;
		
		if(player instanceof ServerPlayer)
		{
			ServerPlayer playerMP = (ServerPlayer)player;
			//sendTeamsMenuToPlayer(playerMP);
			sendLoadoutData(playerMP);
			currentRound.gametype.playerJoined(playerMP);
		}
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
		
		PlayerData victimData = PlayerHandler.getPlayerData(victim);
		
		if(source.getEntity() instanceof ServerPlayer)
		{
			ServerPlayer attacker = ((ServerPlayer)source.getEntity());
			PlayerData attackerData = PlayerHandler.getPlayerData(attacker);
			if(attackerData != null && attackerData.team != null)
			{
				// Make sure players are on opposing teams
				if(attackerData.team != victimData.team)
				{
					AwardXP(attacker, Mth.floor(currentPool.XPForKill * XPMultiplier));
					AwardXP(victim, Mth.floor(currentPool.XPForDeath * XPMultiplier));
				}
			}
		}
	}
	
	public static void AwardXP(ServerPlayer player, int amount)
	{
		PlayerRankData data = rankData.get(player.getUUID());
		if(data != null)
		{
			data.AddXP(amount);
		}
	}
	
	public static void ResetRank(ServerPlayer player)
	{
		PlayerRankData data = rankData.get(player.getUUID());
		if(data != null)
		{
			data.currentLevel = 0;
			data.currentXP = 0;
		}
	}
	
	@Override
	protected void OnRoundEnded()
	{
		pickVoteOptions();
		
		UpdateRoundFinishedTemplate();
		
		for(Player player : getPlayers())
		{
			SendRoundFinishedDataToPlayer((ServerPlayer)player);
		}
		
		super.OnRoundEnded();
	}
	
	private void UpdateRoundFinishedTemplate()
	{
		roundFinishedTemplateData.votingTime = votingTime;
		roundFinishedTemplateData.scoresTime = scoreDisplayTime;
		roundFinishedTemplateData.rankUpdateTime = rankUpdateTime;
		
		roundFinishedTemplateData.votingEnabled = voting;
		if(voting)
		{
			roundFinishedTemplateData.FillVoteOptions(voteOptions);
		}
	}
	
	private void SendRoundFinishedDataToPlayer(ServerPlayer player)
	{
		PlayerData pData = PlayerHandler.getPlayerData(player);
		if(pData != null && pData.builder)
		{
			return;
		}
		
		RoundFinishedData finishedData = new RoundFinishedData(roundFinishedTemplateData);
		PlayerRankData data = rankData.get(player.getUUID());
		
		int resultantXP = data.pendingXP + data.currentXP;
		int resultantLevel = data.currentLevel;
		
		int XPForNextLevel = currentPool.GetXPForLevel(resultantLevel + 1);
		while(XPForNextLevel > 0 && resultantXP >= XPForNextLevel)
		{
			resultantXP -= XPForNextLevel;
			resultantLevel++;
			
			XPForNextLevel = currentPool.GetXPForLevel(resultantLevel + 1);
		}
		
		finishedData.pendingXP = data.pendingXP;
		finishedData.resultantXP = resultantXP;
		finishedData.resultantLevel = resultantLevel;
		
		FlansMod.getPacketHandler().sendTo(new PacketRoundFinished(finishedData), player);
	}
	
	
	private void SendRankDataToPlayer(ServerPlayer player)
	{
		/*
		FlansMod.log("Sending rank data to " + player.getDisplayNameString());
		PacketRankUpdate packet = new PacketRankUpdate();
		PlayerRankData data = rankData.get(player.getUUID());
		if(data != null)
		{
			packet.pendingXP = data.pendingXP;
			int resultantXP = data.pendingXP + data.currentXP;
			int resultantLevel = data.currentLevel;
			while(resultantXP >= currentPool.XPPerLevel[resultantLevel + 1])
			{
				resultantXP -= currentPool.XPPerLevel[resultantLevel + 1];
				resultantLevel++;
			}
			
			packet.resultantXP = resultantXP;
			packet.resultantLevel = resultantLevel;
			packet.showScoresFor = scoreDisplayTime + (voting ? votingTime : 0);
			
			FlansMod.getPacketHandler().sendTo(packet, player);
			
			data.pendingXP = 0;
			data.currentLevel = resultantLevel;
			data.currentXP = resultantXP;
		}
		else
		{
			FlansMod.Assert(false, "Failed to send rank data");
		}
		*/
	}
	
	private void ProcessRankData(ServerPlayer player)
	{
		PlayerRankData data = rankData.get(player.getUUID());
		if(data != null)
		{
			int resultantXP = data.pendingXP + data.currentXP;
			int resultantLevel = data.currentLevel;
			
			int XPForNextLevel = currentPool.GetXPForLevel(resultantLevel + 1);
			while(XPForNextLevel > 0 && resultantXP >= XPForNextLevel)
			{
				resultantXP -= XPForNextLevel;
				resultantLevel++;
				GiveRewardsForLevelUp(resultantLevel, player);
				
				XPForNextLevel = currentPool.GetXPForLevel(resultantLevel + 1);
			}
			
			data.pendingXP = 0;
			data.currentLevel = resultantLevel;
			data.currentXP = resultantXP;
			data.currentKillstreak = 0;
			data.bestKillstreak = 0;
		}
	}
	
	private void GiveRewardsForLevelUp(int level, ServerPlayer player)
	{
		for(RewardBox box : currentPool.rewardsPerLevel[level - 1])
		{
			RewardBoxInstance instance = RewardBoxInstance.CreateLevelUpReward(box, player);
			PlayerRankData data = TeamsManagerRanked.GetRankData(player);
			data.AddRewardBoxInstance(instance);
		}
	}
	
	@Override
	public void showTeamsMenuToAll(boolean info)
	{
		// Do nothing. We never need this
	}
	
	@Override
	public void sendTeamsMenuToPlayer(ServerPlayer player, boolean info)
	{
		if(!enabled || currentRound == null || currentRound.teams == null)
			return;
		
		sendLoadoutData(player);
	}
	
	@Override
	public void sendClassMenuToPlayer(ServerPlayer player)
	{
		// Don't need this either
	}
	
	@Override
	protected void ReadFromNBT(CompoundTag tags, Level world)
	{
		super.ReadFromNBT(tags, world);
		
		int iPoolHash = tags.getIntOr("pool", 0);
		currentPool = LoadoutPool.GetPool(iPoolHash);
		
		ListTag ranks = tags.getListOrEmpty("playerRanks");
		if(ranks != null)
		{
			for(int i = 0; i < ranks.size(); i++)
			{
				CompoundTag playerTags = ranks.getCompoundOrEmpty(i);
				UUID uuid = new UUID(playerTags.getLongOr("uuid1", 0L), playerTags.getLongOr("uuid2", 0L));
				PlayerRankData rData = new PlayerRankData();
				rData.readFromNBT(playerTags);
				rankData.put(uuid, rData);
			}
		}
	}
	
	@Override
	protected void WriteToNBT(CompoundTag tags)
	{
		super.WriteToNBT(tags);
		
		if(currentPool != null)
		{
			tags.putInt("pool", currentPool.shortName.hashCode());
		}
		
		ListTag ranks = new ListTag();
		for(Map.Entry<UUID, PlayerRankData> entry : rankData.entrySet())
		{
			CompoundTag playerTags = new CompoundTag();
			playerTags.putLong("uuid1", entry.getKey().getMostSignificantBits());
			playerTags.putLong("uuid2", entry.getKey().getLeastSignificantBits());
			entry.getValue().writeToNBT(playerTags);
			
			ranks.add(playerTags);
		}
		
		tags.put("playerRanks", ranks);
	}
	
	@Override
	public void playerSelectedClass(ServerPlayer player, String className)
	{
		if(!enabled || currentRound == null)
			return;
		
		//Get player class requested
		int selection = Integer.parseInt(className);
		PlayerRankData data = rankData.get(player.getUUID());
		//PlayerData data = PlayerHandler.getPlayerData(player);
		
		IPlayerClass playerClass = new PlayerClassCustom(selection, data.loadouts[selection]);
		
		playerSelectedClass(player, playerClass);
	}
	
	/**
	 * Client-only GUI/data hooks live in com.flansmod.client.teams.
	 * TeamsClientHook and are reached reflectively, so a dedicated server
	 * never resolves Screen/Minecraft/ClientTeamsData from this class.
	 */
	private static Object clientHook(String method, Class<?>[] parameterTypes, Object... args)
	{
		if(FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT)
			return null;
		try
		{
			Class<?> hook = Class.forName("com.flansmod.client.teams.TeamsClientHook");
			return hook.getMethod(method, parameterTypes).invoke(null, args);
		}
		catch(ReflectiveOperationException e)
		{
			FlansMod.log.error("[TeamsManagerRanked] client hook '" + method + "' failed", e);
			return null;
		}
	}

	public static void ConfirmLoadoutChanges()
	{
		clientHook("confirmLoadoutChanges", new Class[0]);
	}

	public static void ChooseLoadout(int id)
	{
		PacketTeamSelect packet = new PacketTeamSelect();
		packet.classChoicesPacket = true;
		packet.info = false;
		packet.selection = "" + id;
		packet.selectionPacket = true;

		FlansMod.getPacketHandler().sendToServer(packet);
	}

	@Override
	public void SelectTeam(Team team)
	{
		FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(team == null ? "null" : team.shortName, false));
		clientHook("selectTeam", new Class[] {Team.class}, team);
	}

	public static boolean LocalPlayerOwnsUnlock(int unlockHash)
	{
		Object result = clientHook("localPlayerOwnsUnlock", new Class[] {int.class}, unlockHash);
		return result instanceof Boolean && (Boolean)result;
	}
	
	public static boolean PlayerOwnsUnlock(int hashCode, UUID uuid)
	{
		return false;
	}
	
	public static void OpenRewardBox(ServerPlayer player, RewardBox box)
	{
		PlayerRankData data = rankData.get(player.getUUID());
		for(RewardBoxInstance instance : data.rewardBoxData)
		{
			if(!instance.opened
					&& instance.boxHash == box.hashCode()
					&& instance.unlockHash == 0)
			{
				int unlockHash = instance.OpenBox(data);
				FlansMod.getPacketHandler().sendTo(new PacketOpenRewardBox(box.hashCode(), unlockHash), player);
				return;
			}
		}
		
		FlansMod.Assert(false, "Player " + player.getName().getString() + " tried to open box they don't have");
	}
	
	public static PlayerRankData GetRankData(Player player)
	{
		return GetInstance().rankData.get(player.getUUID());
	}
	
	public static PlayerRankData GetRankData(UUID id)
	{
		return GetInstance().rankData.get(id);
	}
}
