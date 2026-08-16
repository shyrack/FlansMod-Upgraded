package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.PlayerData;

public class GametypeCTF extends Gametype
{
	public boolean friendlyFire = false;
	public boolean autoBalance = true;
	public int time;
	public int autoBalanceInterval = 1200;
	public int flagReturnTime = 60;
	
	public GametypeCTF()
	{
		super("Capture the Flag", "CTF", 2);
	}
	
	@Override
	public void roundStart()
	{
	}
	
	@Override
	public void roundEnd()
	{
	}
	
	@Override
	public void roundCleanup()
	{
		
	}
	
	@Override
	public void tick()
	{
		
	}
	
	@Override
	public void playerQuit(ServerPlayer player)
	{
	
	}
	
	@Override
	public boolean playerCanAttack(ServerPlayer attacker, Team attackerTeam, ServerPlayer victim, Team victimTeam)
	{
		return attackerTeam != victimTeam || friendlyFire;
	}
	
	@Override
	public boolean playerAttacked(ServerPlayer player, DamageSource source)
	{
		if(getPlayerData(player) == null || getPlayerData(player).team == null)
			return false;
		ServerPlayer attacker = getPlayerFromDamageSource(source);
		if(attacker != null)
		{
			if(getPlayerData(attacker) == null || getPlayerData(attacker).team == null)
				return false;
			//Check for friendly fire
			if(getPlayerData(player).team == getPlayerData(attacker).team)
				return friendlyFire;
		}
		//Players may not attack spectators
		if(getPlayerData(player).team == Team.spectators)
			return false;
		return true;
	}
	
	@Override
	public void playerKilled(ServerPlayer player, DamageSource source)
	{
		ServerPlayer attacker = getPlayerFromDamageSource(source);
		if(attacker != null)
		{
			//Killed self. Lose a point
			if(attacker == player)
				getPlayerData(player).score--;
				//Killed someone else. Get points
			else
			{
				getPlayerData(attacker).score++;
				getPlayerData(attacker).kills++;
			}
		}
		else
		{
			getPlayerData(player).score--;
		}
		getPlayerData(player).deaths++;
		
		if(player.getPassengers().get(0) instanceof EntityFlag)
		{
			EntityFlag flag = (EntityFlag)player.getPassengers().get(0);
			
			Team flagTeam = teamsManager.getTeam(flag.getBase().getOwnerID());
			flag.stopRiding();
			TeamsManager.messageAll("\u00a7f" + player.getName() + " dropped the \u00a7" + flagTeam.textColour + flagTeam.name + "\u00a7f flag");
		}
	}
	
	@Override
	public void baseAttacked(ITeamBase base, DamageSource source)
	{
	
	}
	
	@Override
	public void objectAttacked(ITeamObject object, DamageSource source)
	{
	
	}
	
	@Override
	public void baseClickedByPlayer(ITeamBase base, ServerPlayer player)
	{
		if(base instanceof EntityFlagpole && ((EntityFlag)base.getFlag()).isHome)
			objectClickedByPlayer(base.getFlag(), player);
	}
	
	@Override
	public void objectClickedByPlayer(ITeamObject object, ServerPlayer player)
	{
		if(teamsManager.currentRound == null)
			return;
		if(object instanceof EntityFlag)
		{
			EntityFlag flag = (EntityFlag)object;
			//Flag belongs to some team other than spectators
			if(flag.getBase().getOwnerID() > 1)
			{
				//Get the player's team and teamID
				Team playerTeam = getPlayerData(player).team;
				PlayerData playerData = getPlayerData(player);
				Team flagTeam = teamsManager.getTeam(flag.getBase().getOwnerID());
				
				//Make sure they are in the game and on the right map
				if(playerTeam != null && playerTeam != Team.spectators && flag.getBase().getMap() == teamsManager.currentRound.map)
				{
					//If the player is clicking their own flag
					if(playerTeam == flagTeam)
					{
						//They found it on the floor and are returning it
						if(flag.getVehicle() == null && !flag.isHome)
						{
							flag.reset();
							playerData.score += 2;
							TeamsManager.messageAll("\u00a7f" + player.getName() + " returned the \u00a7" + flagTeam.textColour + flagTeam.name + "\u00a7f flag");
						}
						
						//TODO : Move to be a proximity thing?
						//They have another flag in hand
						else if(player.getPassengers().get(0) instanceof EntityFlag)
						{
							//Get the other flag and its team
							EntityFlag otherFlag = (EntityFlag)player.getPassengers().get(0);
							Team otherFlagTeam = teamsManager.getTeam(otherFlag.getBase().getOwnerID());
							
							if(otherFlagTeam != null && otherFlagTeam != Team.spectators && otherFlagTeam != flagTeam && flag.isHome)
							{
								playerTeam.score++;
								playerData.score += 10;
								otherFlag.reset();
								TeamsManager.messageAll("\u00a7f" + player.getName() + " captured the \u00a7" + otherFlagTeam.textColour + otherFlagTeam.name + "\u00a7f flag");
							}
						}
					}
					//If the player is clicking the other team flag
					else
					{
						if(flag.getVehicle() == player)
						{
							flag.stopRiding();
							TeamsManager.messageAll("\u00a7f" + player.getName() + " dropped the \u00a7" + flagTeam.textColour + flagTeam.name + "\u00a7f flag");
						}
						else if(flag.getVehicle() == null)
						{
							if(flag.isHome)
								playerData.score += 3;
							flag.startRiding(player);
							TeamsManager.messageAll("\u00a7f" + player.getName() + " picked up the \u00a7" + flagTeam.textColour + flagTeam.name + "\u00a7f flag");
							flag.isHome = false;
						}
						
					}
				}
			}
		}
	}
	
	@Override
	public Vec3 getSpawnPoint(ServerPlayer player)
	{
		if(teamsManager.currentRound == null)
			return null;
		PlayerData data = getPlayerData(player);
		List<BlockPos> validSpawnPoints = new ArrayList<>();
		if(data.newTeam == null)
			return null;
		
		teamsManager.currentRound.map.getValidSpawnPoints(teamsManager.currentRound.getTeamID(data.newTeam), validSpawnPoints);
		
		if(validSpawnPoints.size() > 0)
		{
			BlockPos spawnPoint = validSpawnPoints.get(rand.nextInt(validSpawnPoints.size()));
			return new Vec3(spawnPoint.getX() + 0.5D, spawnPoint.getY(), spawnPoint.getZ() + 0.5D);
		}
		
		return null;
	}
	
	@Override
	public void playerRespawned(ServerPlayer player)
	{
		
	}
	
	@Override
	public boolean setVariable(String variable, String value)
	{
		if(variable.toLowerCase().equals("friendlyfire"))
		{
			friendlyFire = Boolean.parseBoolean(value);
			return true;
		}
		if(variable.toLowerCase().equals("autobalance"))
		{
			autoBalance = Boolean.parseBoolean(value);
			return true;
		}
		if(variable.toLowerCase().equals("flagtime"))
		{
			flagReturnTime = Integer.parseInt(value);
			return true;
		}
		return false;
	}
	
	@Override
	public void readFromNBT(CompoundTag tags)
	{
		friendlyFire = tags.getBooleanOr("CTFFriendlyFire", false);
		autoBalance = tags.getBooleanOr("CTFAutoBalance", false);
		flagReturnTime = tags.getIntOr("CTFFlagTime", 0);
	}
	
	@Override
	public void saveToNBT(CompoundTag tags)
	{
		tags.putBoolean("CTFFriendlyFire", friendlyFire);
		tags.putBoolean("CTFAutoBalance", autoBalance);
		tags.putInt("CTFFlagTime", flagReturnTime);
	}
	
	@Override
	public boolean sortScoreboardByTeam()
	{
		return true;
	}
	
	@Override
	public boolean teamHasWon(Team team)
	{
		return teamsManager.currentRound != null && team.score == teamsManager.currentRound.scoreLimit;
	}
}
