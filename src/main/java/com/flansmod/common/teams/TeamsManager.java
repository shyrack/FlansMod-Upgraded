package com.flansmod.common.teams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemAAGun;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.network.PacketBase;
import com.flansmod.common.network.PacketTeamInfo;
import com.flansmod.common.network.PacketTeamSelect;
import com.flansmod.common.types.InfoType;

public class TeamsManager
{
	/**
	 * Overall switch for teams mod
	 */
	public static boolean enabled = true;
	/**
	 * The instance
	 */
	public static TeamsManager instance;
	
	//Configuration variables
	// Player changeable stuff
	public static boolean voting = false, explosions = true, driveablesBreakBlocks = true,
		bombsEnabled = true, shellsEnabled = true, missilesEnabled = true, bulletsEnabled = true, forceAdventureMode = true, canBreakGuns = true, canBreakGlass = true,
		armourDrops = true, vehiclesNeedFuel = true, overrideHunger = true;
	
	public static int weaponDrops = 1; //0 = no drops, 1 = drops, 2 = smart drops
	//Life of certain entity types. 0 is eternal.
	public static int mgLife = 0, planeLife = 0, vehicleLife = 0, mechaLove = 0, aaLife = 0;
	
	/**
	 * The number of ticks for which to display the round summary page
	 */
	public static int scoreDisplayTime = 200;
	/**
	 * The number of ticks for which to display the voting box, if enabled
	 */
	public static int votingTime = 200;
	/**
	 * The number of ticks for which to display the rank update page
	 */
	public static int rankUpdateTime = 200;
	
	/**
	 * The current round in play. This class replaces the old set of 3 fields "currentGametype", "currentMap" and
	 * "teams"
	 */
	public TeamsRound currentRound;
	/**
	 * This contains a list of all the valid rounds, similar to the old RotationEntry and map rotation
	 */
	public ArrayList<TeamsRound> rounds;
	/**
	 * The list of all available maps
	 */
	public HashMap<String, TeamsMap> maps;
	
	/**
	 * For assigning base IDs to bases. Used primarily in client-server syncing and saving
	 */
	private int nextBaseID = 1;
	public ArrayList<ITeamBase> bases;
	public ArrayList<ITeamObject> objects;
	
	protected long time;
	
	/**
	 * A downwards counter that times the round (in ticks)
	 */
	public int roundTimeLeft;
	/**
	 * A downwards counter that times inter-round phases (in ticks)
	 */
	public int interRoundTimeLeft;
	/**
	 * The list of rounds currently being voted upon
	 */
	public TeamsRound[] voteOptions;
	/**
	 * For forcing the next round. Not normally used
	 */
	public TeamsRound nextRound;
	
	/**
	 * Whether to use autobalance
	 */
	public static boolean autoBalance;
	/**
	 * Time between autobalance attempts
	 */
	public static int autoBalanceInterval;
	/**
	 * The current message of the day. Displays at the top of the landing page
	 */
	public String motd = "Welcome to the Teams server";
	
	//Disused. Delete when done
	//public Gametype currentGametype;
	//public TeamsMap currentMap;
	//public Team[] teams;
	//public List<RotationEntry> rotation;
	//public int currentRotationEntry;
	
	public TeamsManager()
	{
		instance = this;
		
		//Init arrays
		bases = new ArrayList<>();
		objects = new ArrayList<>();
		maps = new HashMap<>();
		rounds = new ArrayList<>();
		
		//Hook the server-side events that replaced the old Forge event bus handlers
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> onEntityHurt(entity, source, amount));
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> onEntityKilled(entity, source));
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> respawnPlayer(newPlayer, false));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerLogin(handler.getPlayer()));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLogout(handler.getPlayer()));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> entityJoinedWorld(entity, world));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.LOAD.register((server, world) ->
		{
			loadPerWorldData(world);
			savePerWorldData(world);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, closing) ->
		{
			for(ServerLevel world : server.getAllLevels())
			{
				savePerWorldData(world);
			}
		});
		
		//Testing stuff. TODO : Replace with automatic Gametype loader
		new GametypeTDM();
		new GametypeZombies();
		//new GametypeConquest();
		new GametypeDM();
		new GametypeCTF();
		//new GametypeNerf();
		//-----
	}
	
	public void reset()
	{
		//currentGametype = null;
		//currentMap = TeamsMap.def;
		//teams = null;
		
		currentRound = null;
		
		bases = new ArrayList<>();
		objects = new ArrayList<>();
		maps = new HashMap<>();
		rounds = new ArrayList<>();
		
		//rotation = new ArrayList<RotationEntry>();
	}
	
	public static TeamsManager getInstance()
	{
		return instance;
	}
	
	public void tick()
	{
		//Send a full team info update to players every 2 seconds.
		if(time % 40 == 0)
		{
			FlansMod.getPacketHandler().sendToAll(new PacketTeamInfo());
			showTeamsMenuToAll(true);
		}
		
		if(!enabled)
			return;
		
		if(currentRound != null)
			currentRound.gametype.tick();
		time++;
		
		
		//Tick bases and objects
		for(ITeamBase base : bases)
			base.tick();
		for(ITeamObject object : objects)
			object.tick();
		if(overrideHunger && currentRound != null)
			for(ServerLevel world : FlansMod.serverInstance.getAllLevels())
				for(Player player : world.players())
					player.getFoodData().eat(20, 10F);
		
		//Check round timer
		//If inbetween rounds
		if(interRoundTimeLeft > 0)
		{
			interRoundTimeLeft--;
			//If we're done showing scores, show the voting box
			if(voting)
			{
				//If the next round is forced, go to it
				if(nextRound != null)
				{
					startNextRound();
					interRoundTimeLeft = 0;
					return;
				}
				else
				{
					//if(interRoundTimeLeft == votingTime)
					//	pickVoteOptions();
					if(interRoundTimeLeft <= votingTime)
					{
						if(voteOptions == null)
							pickVoteOptions();
						displayVotingGUI();
					}
				}
			}
			//If the timer is finished, start the next round
			if(interRoundTimeLeft == 0)
			{
				startNextRound();
			}
		}
		
		//If in a round
		if(currentRound != null && roundTimeLeft > 0)
		{
			//10 seconds before autobalance, display a message
			if(autoBalance() && time % autoBalanceInterval == autoBalanceInterval - 200 && needAutobalance())
			{
				TeamsManager.messageAll("\u00a7fAutobalancing teams...");
			}
			if(autoBalance() && time % autoBalanceInterval == 0 && needAutobalance())
			{
				autobalance();
			}
			
			roundTimeLeft--;
			boolean roundEnded = roundTimeLeft == 0;
			if(roundEnded)
				messageAll(randomTimeOutString());
			for(Team team : currentRound.teams)
			{
				if(currentRound.gametype.teamHasWon(team))
				{
					roundEnded = true;
					messageAll(team.name + " won the round!");
				}
			}
			
			if(roundEnded)
			{
				OnRoundEnded();
			}
		}
	}
	
	protected void OnRoundEnded()
	{
		//The round has ended on a timer, so display the scoreboard summary
		roundTimeLeft = 0;
		interRoundTimeLeft = scoreDisplayTime + rankUpdateTime;
		if(voting) interRoundTimeLeft += votingTime;
		displayScoreboardGUI();
		currentRound.gametype.roundEnd();
		PlayerHandler.roundEnded();
	}
	
	public boolean needAutobalance()
	{
		if(!autoBalance() || currentRound == null || currentRound.teams.length != 2)
			return false;
		int membersTeamA = currentRound.teams[0].members.size();
		int membersTeamB = currentRound.teams[1].members.size();
		if(Math.abs(membersTeamA - membersTeamB) > 1)
			return true;
		return false;
	}
	
	public void autobalance()
	{
		if(!autoBalance() || currentRound == null || currentRound.teams.length != 2)
			return;
		int membersTeamA = currentRound.teams[0].members.size();
		int membersTeamB = currentRound.teams[1].members.size();
		if(membersTeamA - membersTeamB > 1)
		{
			for(int i = 0; i < (membersTeamA - membersTeamB) / 2; i++)
			{
				//My goodness this is convoluted...
				ServerPlayer playerToKick = getPlayer(currentRound.teams[1]
					.addPlayer(currentRound.teams[0].removeWorstPlayer()));
				this.messagePlayer(playerToKick, "You were moved to the other team by the autobalancer.");
				sendClassMenuToPlayer(playerToKick);
			}
		}
		if(membersTeamB - membersTeamA > 1)
		{
			for(int i = 0; i < (membersTeamB - membersTeamA) / 2; i++)
			{
				ServerPlayer playerToKick = getPlayer(currentRound.teams[0]
					.addPlayer(currentRound.teams[1].removeWorstPlayer()));
				this.messagePlayer(playerToKick, "You were moved to the other team by the autobalancer.");
				sendClassMenuToPlayer(playerToKick);
			}
		}
	}
	
	public String randomTimeOutString()
	{
		switch(Gametype.rand.nextInt(4))
		{
			case 0:
				return "That's time!";
			case 1:
				return "How dull; a tie...";
			case 2:
				return "Everybody's a loser but the clock.";
			default:
				return "Time up.";
		}
	}
	
	public void displayScoreboardGUI()
	{
		/*
		for(Player player : getPlayers())
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			if(!data.builder)
				sendPacketToPlayer(new PacketRoundFinished(scoreDisplayTime), (ServerPlayer)player);
		}
		*/
	}
	
	public void displayVotingGUI()
	{
	}
	
	public void pickVoteOptions()
	{
		Collections.sort(rounds);
		voteOptions = new TeamsRound[Math.min(5, rounds.size())];
		for(int i = 0; i < voteOptions.length; i++)
		{
			voteOptions[i] = rounds.get(i);
		}
		
		//Wildcard option!
		voteOptions[Gametype.rand.nextInt(voteOptions.length)] = rounds.get(Gametype.rand.nextInt(rounds.size()));
	}
	
	public void start()
	{
		if(!enabled || rounds.isEmpty())
			return;
		
		//Can only start once
		//if(currentRound != null)
		//	return;
		
		if(currentRound != null)
		{
			
			currentRound.gametype.roundCleanup();
			resetScores();
		}
		
		currentRound = rounds.get(0);
		startRound();
	}
	
	public void startNextRound()
	{
		if(!enabled || rounds.isEmpty())
			return;
		
		//If the next round has not been forced
		if(nextRound == null)
		{
			if(voting)
			{
				//Gather votes and decide which map to play
				int winner = 0;
				int mostVotes = 0;
				
				//Collect the votes from player data
				int[] numVotes = new int[voteOptions.length];
				for(PlayerData data : PlayerHandler.serverSideData.values())
				{
					if(data.vote > 0)
						numVotes[data.vote - 1]++;
				}
				
				//Find the highest one
				for(int i = 0; i < voteOptions.length; i++)
				{
					if(numVotes[i] > mostVotes)
					{
						mostVotes = numVotes[i];
						winner = i;
					}
				}
				nextRound = voteOptions[winner];
				
				
				//Update ratings
				for(TeamsRound round : rounds)
					round.roundsSincePlayed++;
				
				for(int i = 0; i < voteOptions.length; i++)
				{
					if(i == winner)
					{
						voteOptions[i].popularity = 1F - (1F - voteOptions[i].popularity) * 0.8F;
						voteOptions[i].roundsSincePlayed = 0;
					}
					else
					{
						voteOptions[i].popularity *= 0.9F;
						voteOptions[i].popularity += 0.01F;
					}
				}
				
				//Clear votes
				for(PlayerData data : PlayerHandler.serverSideData.values())
					data.vote = 0;
			}
			else //Use standard rotation. Go to next map
			{
				int lastRoundID = rounds.indexOf(currentRound);
				int nextRoundID = ++lastRoundID % rounds.size();
				nextRound = rounds.get(nextRoundID);
			}
		}
		
		//End the last round
		if(currentRound != null)
		{
			for(ITeamBase base : currentRound.map.bases)
				base.roundCleanup();
			currentRound.gametype.roundCleanup();
		}
		resetScores();
		
		//Advance to next round
		if(nextRound != null)
			currentRound = nextRound;
		//Note that if nextRound is null, we stay on the round we just played
		
		//Begin the next round
		startRound();
		
		//Reset this. Used for round forcing only.
		nextRound = null;
	}
	
	public void startRound()
	{
		currentRound.gametype.roundStart();
		roundTimeLeft = currentRound.timeLimit * 60 * 20;
		for(ITeamBase base : bases)
		{
			base.startRound();
		}
		
		for(Player player : getPlayers())
			forceRespawn((ServerPlayer)player);
		
		showTeamsMenuToAll();
		
		messageAll("\u00a7fA new round has started!");
	}
	
	/**
	 * Called at the start of a round. Shows all players the team selection menu. Exludes people on the building / op
	 * team
	 */
	public void showTeamsMenuToAll()
	{
		showTeamsMenuToAll(false);
	}
	
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
	
	/**
	 * Called from the interact methods of team entities when the player right clicks them
	 */
	public void playerClickedEntity(ServerPlayer player, Entity target)
	{
		ItemStack currentItem = player.getMainHandItem();
		if(currentItem != null && !currentItem.isEmpty() && currentItem.getItem() instanceof ItemOpStick)
		{
			((ItemOpStick)currentItem.getItem()).clickedEntity(player.level(), player, target);
			return;
		}
		if(!enabled || currentRound == null)
			return;
		if(target instanceof ITeamObject)
			currentRound.gametype.objectClickedByPlayer((ITeamObject)target, player);
		if(target instanceof ITeamBase)
			currentRound.gametype.baseClickedByPlayer((ITeamBase)target, player);
	}
	
	/**
	 * Stop damage being taken when it shouldn't N - NoTeam, S - Spectator, 1 - Team 1, 2 - Team 2, O - Other (mobs and
	 * world inflicted damage etc)
	 * <p>
	 * | N S O 1 2 ------------ N| y n y n n S| n n n n n O| y n y y y 1| n n y G G 2| n n y G G
	 * <p>
	 * y - yes, can hurt n - no, can't hurt G - decided by gametype
	 */
	public boolean onEntityHurt(LivingEntity entity, DamageSource source, float amount)
	{
		if(!enabled || currentRound == null)
			return true;
		if(entity instanceof ServerPlayer)
		{
			ServerPlayer player = (ServerPlayer)entity;
			PlayerData data = PlayerHandler.getPlayerData(player);
			
			if(data.team == Team.spectators && source != entity.level().damageSources().generic())
			{
				return false;
			}
			
			if(source.getDirectEntity() instanceof ServerPlayer)
			{
				ServerPlayer attacker = ((ServerPlayer)source.getDirectEntity());
				PlayerData attackerData = PlayerHandler.getPlayerData(attacker);
				
				if(attackerData == null)
					return true;
				
				//Can hurt self
				if(attacker == player)
					return true;
				
				//Cannot be attacked by a spectator
				if(attackerData.team == Team.spectators)
				{
					return false;
				}
				
				//Cannot be fights between people in the game and outside the game
				if((attackerData.team == null && data.team != null) || (attackerData.team != null && data.team == null))
				{
					return false;
				}
				
				//Final case. Either the two players are not in the game (in which case, ignore) or they are both in the game.
				//At this point, we pass over to the gametype
				if(attackerData.team != null && data.team != null)
				{
					//The roundTimeLeft check ensures that players do not fight during the cooldown period
					if(roundTimeLeft > 0 &&
						!currentRound.gametype.playerCanAttack(attacker, attackerData.team, player, data.team))
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Handles entity deaths. Passes information to gametype for scoring
	 */
	public void onEntityKilled(LivingEntity entity, DamageSource source)
	{
		if(!enabled)
			return;
		if(currentRound != null)
		{
			currentRound.gametype.entityKilled(entity, source);
		}
		
		if(entity instanceof ServerPlayer)
		{
			OnPlayerKilled((ServerPlayer)entity, source);
		}
	}
	
	public void OnPlayerKilled(ServerPlayer player, DamageSource source)
	{
		if(currentRound != null)
		{
			currentRound.gametype.playerKilled(player, source);
		}
	}
	
	/**
	 * Base and object gathering hooks for entities, not tile entities
	 */
	public void entityJoinedWorld(Entity entity, ServerLevel world)
	{
		if(entity instanceof ITeamBase)
		{
			registerBase((ITeamBase)entity);
			if(((ITeamBase)entity).getBaseID() > nextBaseID)
			{
				FlansMod.log.warn("Loaded base with ID higher than the supposed highest ID. Adjusted highest ID");
				nextBaseID = ((ITeamBase)entity).getBaseID();
			}
		}
		if(entity instanceof ITeamObject)
		{
			objects.add((ITeamObject)entity);
		}
	}
	
	/**
	 * Called from block useWithoutItem when a player right clicks a team block
	 */
	public void playerInteracted(ServerPlayer player, BlockPos pos)
	{
		if(!enabled)
			return;
		if(player.getMainHandItem() != null && !player.getMainHandItem().isEmpty() &&
			player.getMainHandItem().getItem() instanceof ItemGun)
		{
			return;
		}
		
		BlockEntity te = player.level().getBlockEntity(pos);
		if(te != null)
		{
			ItemStack currentItem = player.getMainHandItem();
			if(currentItem.getItem() instanceof ItemOpStick)
			{
				if(te instanceof ITeamObject)
					((ItemOpStick)currentItem.getItem()).clickedObject(player.level(),
						player,
						(ITeamObject)te);
				if(te instanceof ITeamBase)
					((ItemOpStick)currentItem.getItem()).clickedBase(player.level(),
						player,
						(ITeamBase)te);
			}
			else if(currentRound != null)
			{
				if(te instanceof ITeamObject)
					currentRound.gametype
						.objectClickedByPlayer((ITeamObject)te, player);
				if(te instanceof ITeamBase)
					currentRound.gametype.baseClickedByPlayer((ITeamBase)te, player);
			}
		}
	}
	
	/**
	 * Called on player death to decide what happens to their drops. Not hooked up yet; the old Forge PlayerDropsEvent
	 * has no Fabric equivalent.
	 */
	public void playerDrops(ServerPlayer player, List<ItemEntity> drops)
	{
		ArrayList<ItemEntity> dropsToThrow = new ArrayList<>();
		//First collect together guns and ammo if smart drops are enabled
		if(weaponDrops == 2)
		{
			for(ItemEntity entity : drops)
			{
				ItemStack stack = entity.getItem();
				if(stack != null && !stack.isEmpty())
				{
					if(stack.getItem() instanceof ItemGun)
					{
						EntityGunItem gunEntity = new EntityGunItem(entity);
						stack.setCount(0);
						boolean alreadyAdded = false;
						for(ItemEntity check : dropsToThrow)
						{
							if(check.getItem().isEmpty() || !(check.getItem().getItem() instanceof ItemGun))
								continue;
							
							if(((ItemGun)stack.getItem()).GetType() == ((ItemGun)check.getItem().getItem()).GetType())
								alreadyAdded = true;
						}
						if(!alreadyAdded)
						{
							((ServerLevel)player.level()).addFreshEntity(gunEntity);
							dropsToThrow.add(gunEntity);
						}
					}
				}
			}
		}
		//Now iterate again and look for ammo
		for(ItemEntity entity : dropsToThrow)
		{
			EntityGunItem gunEntity = (EntityGunItem)entity;
			GunType gunType = ((ItemGun)gunEntity.getItem().getItem()).GetType();
			for(ItemEntity ammoEntity : drops)
			{
				ItemStack ammoItemstack = ammoEntity.getItem();
				if(ammoItemstack != null && ammoItemstack.getItem() instanceof ItemShootable)
				{
					ShootableType bulletType = ((ItemShootable)ammoItemstack.getItem()).type;
					if(gunType.isCorrectAmmo(bulletType))
					{
						gunEntity.ammoStacks.add(ammoItemstack.copy());
						ammoItemstack.setCount(0);
					}
				}
			}
		}
		//Now check the remaining items to see if they should be dropped
		for(ItemEntity entity : drops)
		{
			ItemStack stack = entity.getItem();
			if(stack != null && !stack.isEmpty())
			{
				if(stack.getItem() instanceof ItemGun || stack.getItem() instanceof ItemPlane ||
					stack.getItem() instanceof ItemVehicle || stack.getItem() instanceof ItemAAGun ||
					stack.getItem() instanceof ItemBullet)
				{
					if(weaponDrops != 1)
						dropsToThrow.add(entity);
				}
				else if(stack.getItem() instanceof ItemTeamArmour)
				{
					if(!armourDrops)
						dropsToThrow.add(entity);
				}
			}
		}
		drops.removeAll(dropsToThrow);
		
	}
	
	/**
	 * Stop spectators looting items
	 */
	public boolean playerCanLoot(Player player, ItemStack itemStack)
	{
		if(enabled && currentRound != null && PlayerHandler.getPlayerData(player) != null)
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			if(data.team == Team.spectators || !currentRound.gametype
				.playerCanLoot(itemStack, InfoType.getType(itemStack), player, data.team))
				return false;
		}
		return true;
	}
	
	public void onPlayerRespawn(ServerPlayer player)
	{
		respawnPlayer(player, false);
	}
	
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
	
	public void onPlayerLogout(Player player)
	{
		for(Team team : Team.teams)
			team.removePlayer(player);
	}
	
	public void respawnPlayer(Player player, boolean firstSpawn)
	{
		if(player.level().isClientSide())
			return;
		
		if(!enabled || currentRound == null)
			return;
		
		ServerPlayer playerMP = ((ServerPlayer)player);
		PlayerData data = PlayerHandler.getPlayerData(playerMP);
		
		if(data == null || (data.builder && playerIsOp(playerMP)))
			return;
		
		//On the first spawn, we don't kill the player, we simply move them over, so do a /tp like command
		if(firstSpawn)
		{
			Vec3 spawnPoint = currentRound.gametype.getSpawnPoint(playerMP);
			if(spawnPoint != null)
			{
				playerMP.stopRiding();
				playerMP.teleportTo(spawnPoint.x, spawnPoint.y, spawnPoint.z);
			}
		}
		
		//To set their next spawn position, override their bed position
		setPlayersNextSpawnpoint(playerMP);
		
		if(forceAdventureMode)
			playerMP.setGameMode(GameType.ADVENTURE);
		resetInventory(player);
		currentRound.gametype.playerRespawned((ServerPlayer)player);
	}
	
	private void setPlayersNextSpawnpoint(ServerPlayer player, BlockPos pos, int dimension)
	{
		net.minecraft.resources.ResourceKey<Level> dim = dimension == -1 ? Level.NETHER : dimension == 1 ? Level.END : Level.OVERWORLD;
		player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(dim, pos, 0F, 0F), true), true);
	}
	
	private void setPlayersNextSpawnpoint(ServerPlayer player)
	{
		if(!enabled || currentRound == null)
			return;
		
		PlayerData data = PlayerHandler.getPlayerData(player);
		
		Vec3 spawnPoint = currentRound.gametype.getSpawnPoint(player);
		if(spawnPoint != null)
			setPlayersNextSpawnpoint(player,
				new BlockPos(Mth.floor(spawnPoint.x),
					Mth.floor(spawnPoint.y) + 1,
					Mth.floor(spawnPoint.z)),
				0);
		else
			FlansMod.log.warn("Could not find spawn point for " + player.getDisplayName() + " on team " +
				(data.newTeam == null ? "null" : data.newTeam.name));
	}
	
	/**
	 * Force a respawn
	 */
	public void forceRespawn(ServerPlayer player)
	{
		if(playerIsOp(player) && PlayerHandler.getPlayerData(player).builder)
			return;
		player.getInventory().clearContent();
		player.heal(9001);
		if(forceAdventureMode)
			player.setGameMode(GameType.ADVENTURE);
		respawnPlayer(player, true);
	}
	
	public void sendTeamsMenuToPlayer(ServerPlayer player)
	{
		sendTeamsMenuToPlayer(player, false);
	}
	
	public void sendTeamsMenuToPlayer(ServerPlayer player, boolean info)
	{
		if(!enabled || currentRound == null || currentRound.teams == null)
			return;
		//Get the available teams from the gametype
		Team[] availableTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
		//Add in the spectators as an option and "none" if the player is an op
		boolean playerIsOp = FlansMod.serverInstance.getPlayerList()
			.isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
		Team[] allAvailableTeams = new Team[availableTeams.length + (playerIsOp ? 2 : 1)];
		System.arraycopy(availableTeams, 0, allAvailableTeams, 0, availableTeams.length);
		allAvailableTeams[availableTeams.length] = Team.spectators;
		
		sendPacketToPlayer(new PacketTeamSelect(allAvailableTeams, info), player);
	}
	
	public void sendClassMenuToPlayer(ServerPlayer player)
	{
		Team team = PlayerHandler.getPlayerData(player).newTeam;
		if(team == null)
		{
			sendTeamsMenuToPlayer(player);
		}
		else if(team != Team.spectators && team.classes.size() > 0)
		{
			sendPacketToPlayer(new PacketTeamSelect(team.classes.toArray(new PlayerClass[team.classes.size()])),
				player);
		}
	}
	
	public boolean playerIsOp(Player player)
	{
		return FlansMod.serverInstance.getPlayerList()
			.isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
	}
	
	public boolean autoBalance()
	{
		return !(currentRound != null && !currentRound.gametype.shouldAutobalance()) && autoBalance;
	}
	
	//
	public void playerSelectedTeam(ServerPlayer player, String teamName)
	{
		if(!enabled || currentRound == null)
			return;
		
		PlayerData data = PlayerHandler.getPlayerData(player);
		
		data.builder = false;
		
		//The player picked the op / builder team
		if(teamName.equals("null"))
		{
			if(playerIsOp(player))
			{
				data.team = null;
				data.builder = true;
				return;
			}
			else teamName = "spectators";
		}
		
		//The team the player selected
		Team selectedTeam = Team.getTeam(teamName);
		//They cannot pick no team
		if(selectedTeam == null)
			selectedTeam = Team.spectators;
		
		//Validate the selected team
		boolean isValid = selectedTeam == Team.spectators;
		Team[] validTeams = currentRound.gametype.getTeamsCanSpawnAs(currentRound, player);
		for(Team validTeam : validTeams)
		{
			if(selectedTeam == validTeam)
				isValid = true;
		}
		//Default to spectator
		if(!isValid)
		{
			player.sendSystemMessage(Component.literal(
				"You may not join " + selectedTeam.name + " for it is invalid. Please try again"));
			FlansMod.log.warn(player.getName() + " tried to spawn on an invalid team : " + selectedTeam.name);
			selectedTeam = Team.spectators;
		}
		
		//Spawn spectators immediately
		if(selectedTeam == Team.spectators)
		{
			messageAll(player.getName() + " joined \u00a7" + selectedTeam.textColour + selectedTeam.name);
			if(data.team != null)
				data.team.removePlayer(player);
			data.newTeam = data.team = Team.spectators;
			player.getInventory().clearContent();
			data.team.addPlayer(player);
			player.heal(9001);
			respawnPlayer(player, true);
		}
		//Give other players the chance to select a class
		else
		{
			Team otherTeam = currentRound.getOtherTeam(selectedTeam);
			if(autoBalance() && selectedTeam.members.size() > otherTeam.members.size() + 1)
			{
				player.sendSystemMessage(Component.literal(
					"You may not join " + selectedTeam.name + " due to imbalance. Please try again"));
				sendTeamsMenuToPlayer(player);
				return;
			}
			data.newTeam = selectedTeam;
			sendClassMenuToPlayer(player);
		}
		
		currentRound.gametype.playerChoseTeam(player, data.team, selectedTeam);
	}
	
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
			player
				.sendSystemMessage(Component.literal("You may not select " + playerClass.name + ". Please try again"));
			FlansMod.log.warn(player.getName() + " tried to pick an invalid class : " + playerClass.name);
			//sendClassMenuToPlayer(player);
			return;
		}
		
		playerSelectedClass(player, playerClass);
	}
	
	public void playerSelectedClass(ServerPlayer player, IPlayerClass playerClass)
	{
		if(playerClass == null)
		{
			FlansMod.log.warn("Error in class selection");
			return;
		}
		
		PlayerData data = PlayerHandler.getPlayerData(player);
		
		//Check cases
		//1 : Player switched class only
		if(data.team == data.newTeam && data.playerClass != null &&
			!data.playerClass.GetShortName().equals(playerClass.GetShortName()))
		{
			currentRound.gametype.playerChoseNewClass(player, playerClass);
			data.newPlayerClass = playerClass;
			player
				.sendSystemMessage(Component.literal("You will respawn with the " + playerClass.GetName() + " class"));
		}
		//2 : Player switched team
		else if(data.team != null && data.team != data.newTeam)
		{
			messageAll(player.getName() + " switched to \u00a7" + data.newTeam.textColour + data.newTeam.name);
			currentRound.gametype.playerDefected(player, data.team, data.newTeam);
			setPlayersNextSpawnpoint(player);
			player.hurt(player.level().damageSources().generic(), 10000F);
			if(data.team != null)
				data.team.removePlayer(player);
			data.newTeam.addPlayer(player);
			data.team = data.newTeam;
			data.newPlayerClass = playerClass;
		}
		//3 : Player has only just joined
		else if(data.team == null)
		{
			if(data.newTeam == null)
			{
				FlansMod.Assert(false, "NULL TEAM");
			}
			else
			{
				messageAll(player.getName() + " joined \u00a7" + data.newTeam.textColour + data.newTeam.name);
				currentRound.gametype.playerEnteredTheGame(player, data.newTeam, playerClass);
				data.newTeam.addPlayer(player);
				data.team = data.newTeam;
				data.newPlayerClass = playerClass;
				currentRound.gametype.playerChoseNewClass(player, playerClass);
				respawnPlayer(player, true);
			}
		}
	}
	
	public void resetInventory(Player player)
	{
		Team team = PlayerHandler.getPlayerData(player).team;
		IPlayerClass playerClass = PlayerHandler.getPlayerData(player).getPlayerClass();
		
		if(team == null)
			return;
		
		player.getInventory().clearContent();
		
		//Set team armour
		if(team.hat != null)
			player.setItemSlot(EquipmentSlot.HEAD, team.hat.copy());
		if(team.chest != null)
			player.setItemSlot(EquipmentSlot.CHEST, team.chest.copy());
		if(team.legs != null)
			player.setItemSlot(EquipmentSlot.LEGS, team.legs.copy());
		if(team.shoes != null)
			player.setItemSlot(EquipmentSlot.FEET, team.shoes.copy());
		
		if(playerClass == null)
			return;
		
		//Override with class armour
		if(playerClass.GetHat() != null)
			player.setItemSlot(EquipmentSlot.HEAD, playerClass.GetHat().copy());
		if(playerClass.GetChest() != null)
			player.setItemSlot(EquipmentSlot.CHEST, playerClass.GetChest().copy());
		if(playerClass.GetLegs() != null)
			player.setItemSlot(EquipmentSlot.LEGS, playerClass.GetLegs().copy());
		if(playerClass.GetShoes() != null)
			player.setItemSlot(EquipmentSlot.FEET, playerClass.GetShoes().copy());
		
		for(ItemStack stack : playerClass.GetStartingItems())
		{
			player.getInventory().add(stack.copy());
			//Load up as many guns as possible
		}
		
		//Preload each gun
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack stack = player.getInventory().getItem(i);
			if(stack != null && stack.getItem() instanceof ItemGun)
			{
				((ItemGun)stack.getItem())
					.Reload(stack, player.level(), player, player.getInventory(), InteractionHand.MAIN_HAND, false, true, false);
			}
		}
	}
	
	//---------------------------------------------------------
	// Saving and Loading
	//---------------------------------------------------------

	private void loadPerWorldData(ServerLevel world)
	{
		//Reset the teams manager before loading a new world
		reset();
		//Read the teams dat file
		if(!getTeamsFile(world).exists())
		{
			return;
		}
		
		try
		{
			CompoundTag tags = NbtIo.readCompressed(new FileInputStream(getTeamsFile(world)), NbtAccounter.unlimitedHeap());
			ReadFromNBT(tags, world);
			//Start the rotation
			if(enabled && rounds.size() > 0)
				start();
		}
		catch(Exception e)
		{
			FlansMod.log.error("Failed to load from teams.dat", e);
			
		}
		
		//Reset all infotypes. Specifically, send this to player classes so that they may create itemstacks from strings regarding attachments for guns
		//for(InfoType type : InfoType.infoTypes.values())
			//type.onWorldLoad(world);
	}
	
	private void savePerWorldData(ServerLevel world)
	{
		// TODO: Move to SavedData saving
		if(!createTeamsFile(world))
		{
			return;
		}
		
		CompoundTag tags = new CompoundTag();
		WriteToNBT(tags);
		try
		{
			NbtIo.writeCompressed(tags, new FileOutputStream(getTeamsFile(world)));
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to save to teams.dat", e);
		}
	}
	
	protected void ReadFromNBT(CompoundTag tags, Level world)
	{
		nextBaseID = tags.getIntOr("NextBaseID", 0);
		//Read maps
		for(int i = 0; i < tags.getIntOr("NumberOfMaps", 0); i++)
		{
			TeamsMap map = new TeamsMap(world, tags.getCompoundOrEmpty("Map_" + i));
			maps.put(map.shortName, map);
		}
		
		int dimension = 0; //TODO : FIX THIS
		if(maps.isEmpty())
		{
			maps.put("default" + dimension,
				new TeamsMap(world, "default" + dimension, "Default " + world.getServer().getWorldData().getLevelName()));
		}
		
		//Read the rounds list		
		for(int i = 0; i < tags.getIntOr("RoundsSize", 0); i++)
		{
			TeamsRound round = new TeamsRound(tags.getCompoundOrEmpty("Round_" + i));
			rounds.add(round);
		}
		
		//Read variables
		enabled = tags.getBooleanOr("Enabled", false);
		voting = tags.getBooleanOr("Voting", false);
		votingTime = tags.getIntOr("VotingTime", 0);
		scoreDisplayTime = tags.getIntOr("ScoreTime", 0);
		rankUpdateTime = tags.getIntOr("RankUpdateTime", 0);
		bombsEnabled = tags.getBooleanOr("Bombs", false);
		bulletsEnabled = tags.getBooleanOr("Bullets", false);
		explosions = tags.getBooleanOr("Explosions", false);
		forceAdventureMode = tags.getBooleanOr("ForceAdventure", false);
		canBreakGuns = tags.getBooleanOr("CanBreakGuns", false);
		canBreakGlass = tags.getBooleanOr("CanBreakGlass", false);
		armourDrops = tags.getBooleanOr("ArmourDrops", false);
		weaponDrops = tags.getIntOr("WeaponDrops", 0);
		vehiclesNeedFuel = tags.getBooleanOr("NeedFuel", false);
		mgLife = tags.getIntOr("MGLife", 0);
		aaLife = tags.getIntOr("AALife", 0);
		vehicleLife = tags.getIntOr("VehicleLife", 0);
		mechaLove = tags.getIntOr("MechaLove", 0);
		planeLife = tags.getIntOr("PlaneLife", 0);
		driveablesBreakBlocks = tags.getBooleanOr("BreakBlocks", false);
	}
	
	protected void WriteToNBT(CompoundTag tags)
	{
		tags.putInt("NextBaseID", nextBaseID);
		//Changed name so that it does not try to read old maps
		tags.putInt("NumberOfMaps", maps.size());
		//Write the maps to memory
		if(maps != null)
		{
			int i = 0;
		for(TeamsMap map : maps.values())
		{
			CompoundTag mapTags = new CompoundTag();
			map.writeToNBT(mapTags);
			tags.put("Map_" + i, mapTags);
			i++;
		}
		}
		//Write the rounds list to memory
		if(rounds != null)
		{
			tags.putInt("RoundsSize", rounds.size());
			for(int i = 0; i < rounds.size(); i++)
			{
				TeamsRound entry = rounds.get(i);
				if(entry != null)
				{
				CompoundTag roundTags = new CompoundTag();
				entry.writeToNBT(roundTags);
				tags.put("Round_" + i, roundTags);
				}
			}
		}
		else tags.putInt("RoundsSize", 0);
		//Write the current round to memory
		if(currentRound != null)
			tags.putInt("CurrentRound", rounds.indexOf(currentRound));
		//Save gametype settings to memory
		for(Gametype gametype : Gametype.gametypes.values())
		{
			gametype.saveToNBT(tags);
		}
		
		//Save variables
		tags.putBoolean("Enabled", enabled);
		tags.putBoolean("Voting", voting);
		tags.putInt("VotingTime", votingTime);
		tags.putInt("ScoreTime", scoreDisplayTime);
		tags.putInt("RankUpdateTime", rankUpdateTime);
		tags.putBoolean("Bombs", bombsEnabled);
		tags.putBoolean("Bullets", bulletsEnabled);
		tags.putBoolean("Explosions", explosions);
		tags.putBoolean("ForceAdventure", forceAdventureMode);
		tags.putBoolean("CanBreakGuns", canBreakGuns);
		tags.putBoolean("CanBreakGlass", canBreakGlass);
		tags.putBoolean("ArmourDrops", armourDrops);
		tags.putInt("WeaponDrops", weaponDrops);
		tags.putBoolean("NeedFuel", vehiclesNeedFuel);
		tags.putInt("MGLife", mgLife);
		tags.putInt("AALife", aaLife);
		tags.putInt("VehicleLife", vehicleLife);
		tags.putInt("MechaLove", mechaLove);
		tags.putInt("PlaneLife", planeLife);
		tags.putBoolean("BreakBlocks", driveablesBreakBlocks);
	}
	
	/**
	 * Attempts to create a teams file for the given world.
	 *
	 * @return True if a new file was created, False if not.
	 */
	private static boolean createTeamsFile(ServerLevel world)
	{
		String worldName = world.dimension().identifier().toString();
		File file = getTeamsFile(world);
		
		// Backwards compatibility (added v5.6)
		File oldFile = new File(world.getServer().getWorldPath(LevelResource.ROOT).toFile(),
			"teams_" + world.dimension().identifier().toString() + ".dat");
		if(oldFile.exists())
		{
			if(oldFile.renameTo(file))
			{
				FlansMod.log.info("Updated teams data to new save location for world: " + worldName);
			}
			else
			{
				FlansMod.log.error("Failed to update teams data to new save location for world: " + worldName);
			}
		}
		
		try
		{
			if(file.createNewFile())
			{
				FlansMod.log.info("Created teams file for world: " + worldName + " " + file.getAbsolutePath());
				return true;
			}
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to create teams file for world: " + worldName, e);
		}
		return false;
	}
	
	private static File getTeamsFile(ServerLevel world)
	{
		return new File(world.getServer().getWorldPath(LevelResource.ROOT).toFile(), "teams.dat");
	}
	
	//------------------------------------------------------------------------------
	// Getters, setters, registers, loggers and the likes 
	//------------------------------------------------------------------------------
	
	public void resetScores()
	{
		for(Team team : Team.teams)
		{
			team.score = 0;
			team.members.clear();
		}
		for(Player player : getPlayers())
			if(PlayerHandler.getPlayerData(player) != null)
				PlayerHandler.getPlayerData(player).resetScore();
	}
	
	public ITeamBase getBase(int ID)
	{
		for(ITeamBase base : bases)
		{
			if(base.getBaseID() == ID)
				return base;
		}
		return null;
	}
	
	public void registerBase(ITeamBase base)
	{
		if(base.getBaseID() == 0)
			base.setBaseID(nextBaseID++);
		bases.add(base);
	}
	
	public void registerObject(ITeamObject obj)
	{
		objects.add(obj);
	}
	
	public static ServerPlayer getPlayer(String username)
	{
		return FlansMod.serverInstance.getPlayerList().getPlayerByName(username);
	}
	
	public static void log(String s)
	{
		FlansMod.log.info("Teams Info : " + s);
	}
	
	public static void messagePlayer(ServerPlayer player, String s)
	{
		player.sendSystemMessage(Component.literal(s));
	}
	
	public static void messageAll(String s)
	{
		FlansMod.log.info("Teams Announcement : " + s);
		for(ServerPlayer player : getPlayers())
		{
			player.sendSystemMessage(Component.literal(s));
		}
	}
	
	public static void sendPacketToPlayer(PacketBase packet, ServerPlayer player)
	{
		FlansMod.getPacketHandler().sendTo(packet, player);
	}
	
	public static List<ServerPlayer> getPlayers()
	{
		return FlansMod.serverInstance.getPlayerList().getPlayers();
	}
	
	/**
	 * Returns the team associated with the given ID
	 */
	public Team getTeam(int spawnerTeamID)
	{
		if(!enabled || currentRound == null || spawnerTeamID == 0)
			return null;
		if(spawnerTeamID == 1)
			return Team.spectators;
		return currentRound.teams[spawnerTeamID - 2];
	}
	
	/**
	 * The maps HashMap is indexed by shortName, not full name, so this method helps there
	 */
	public TeamsMap getMapFromFullName(String string)
	{
		for(TeamsMap map : maps.values())
		{
			if(map.name.equals(string))
				return map;
		}
		return null;
	}
	
	public void SelectTeam(Team team)
	{
		FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(team == null ? "null" : team.shortName, false));
		Minecraft.getInstance().setScreen(null);
	}
}
