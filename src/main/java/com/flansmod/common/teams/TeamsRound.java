package com.flansmod.common.teams;

import java.util.Optional;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

/**
 * This represents a round in the teams mod
 * It designates the map, gametype and teams to be played that round
 * A list of valid rounds is kept by the TeamsManager and then either
 * players vote on which rounds to play or there is a rotation
 */
public class TeamsRound implements Comparable<TeamsRound>
{
	public Gametype gametype;
	public TeamsMap map;
	/**
	 * The teams available. This does not include spectators
	 */
	public Team[] teams;
	/**
	 * The round length in minutes
	 */
	public int timeLimit;
	/**
	 * The round score limit
	 */
	public int scoreLimit;
	/**
	 * 0 is almost never picked, 1 is always always picked. Used to pick vote options
	 */
	public float popularity;
	/**
	 * Number of rounds since it was offered as an option in the vote. Used to pick vote options
	 */
	public int roundsSincePlayed;
	
	public TeamsRound(TeamsMap map2, Gametype gametype2, Team[] teams2, int timeLimit, int scoreLimit)
	{
		map = map2;
		gametype = gametype2;
		teams = teams2;
		this.timeLimit = timeLimit;
		this.scoreLimit = scoreLimit;
		popularity = 0.5F;
	}
	
	public TeamsRound(CompoundTag tags)
	{
		map = TeamsManager.getInstance().maps.get(tags.getStringOr("Map", ""));
		gametype = Gametype.getGametype(tags.getStringOr("Gametype", ""));
		timeLimit = tags.getIntOr("TimeLimit", 0);
		scoreLimit = tags.getIntOr("ScoreLimit", 0);
		
		teams = new Team[tags.getIntOr("NumTeams", 0)];
		for(int i = 0; i < teams.length; i++)
		{
			teams[i] = Team.getTeam(tags.getStringOr("Team_" + i, ""));
			if(teams[i] == null)
			{
				teams[i] = Team.teams.get(0);
			}
			
		}
		
		popularity = tags.getFloatOr("Pop", 0F);
	}
	
	public void writeToNBT(CompoundTag tags)
	{
		tags.putString("Map", map.shortName);
		tags.putString("Gametype", gametype.shortName);
		tags.putInt("TimeLimit", timeLimit);
		tags.putInt("ScoreLimit", scoreLimit);
		
		tags.putInt("NumTeams", teams.length);
		for(int i = 0; i < teams.length; i++)
		{
			tags.putString("Team_" + i, teams[i].shortName);
		}
		
		tags.putFloat("Pop", popularity);
	}
	
	public int getTeamID(Team team)
	{
		if(team == Team.spectators)
			return 1;
		if(team == teams[0])
			return 2;
		if(team == teams[1])
			return 3;
		return 0;
	}
	
	public Team getTeam(int id)
	{
		switch(id)
		{
			case 0: return null;
			case 1: return Team.spectators;
			default: return teams[id - 2];
		}
	}
	
	/**
	 * In two team gametypes, returns the opposite team
	 */
	public Team getOtherTeam(Team team)
	{
		if(team == Team.spectators || team == null || teams.length != 2)
			return team;
		if(team == teams[0])
			return teams[1];
		return teams[0];
	}
	
	public Optional<Team> getTeam(Player player)
	{
		String username = player.getName().getString();
		for(Team team : teams)
		{
			for (String name : team.members)
			{
				if (username.equals(name))
				{
					return Optional.of(team);
				}
			}
		}
		return Optional.empty();
	}
	
	public float getWeight()
	{
		return popularity * 4F + roundsSincePlayed;
	}
	
	@Override
	public int compareTo(TeamsRound o)
	{
		return Float.compare(o.getWeight(), getWeight());
	}
}
