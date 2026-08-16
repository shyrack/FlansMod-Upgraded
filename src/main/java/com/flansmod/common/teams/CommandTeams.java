package com.flansmod.common.teams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import com.flansmod.common.FlansMod;

public class CommandTeams
{
	public static TeamsManager teamsManager = TeamsManager.getInstance();
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("teams")
			.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
			.executes(ctx -> execute(ctx.getSource(), new String[0]))
			.then(Commands.argument("args", StringArgumentType.greedyString())
				.executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "args").split(" ")))));
	}
	
	public static int execute(CommandSourceStack sender, String[] split)
	{
		if(teamsManager == null)
		{
			sender.sendSystemMessage(Component.literal("Teams mod is broken. You will need to look at the server side logs to see what's wrong"));
			return 0;
		}
		if(split == null || split.length == 0 || split[0].equals("help") || split[0].equals("?"))
		{
			if(split.length == 2)
				sendHelpInformation(sender, Integer.parseInt(split[1]));
			else sendHelpInformation(sender, 1);
			return 0;
		}
		//On / off
		if(split[0].equals("off"))
		{
			teamsManager.currentRound = null;
			teamsManager.enabled = false;
			TeamsManager.messageAll("Flan's Teams Mod disabled");
			return 0;
		}
		if(split[0].equals("on"))
		{
			teamsManager.enabled = true;
			TeamsManager.messageAll("Flan's Teams Mod enabled");
			return 0;
		}
		if(!teamsManager.enabled)
		{
			sender.sendSystemMessage(Component.literal("Teams mod is disabled. Try /teams on"));
			return 0;
		}
		if(split[0].equals("survival"))
		{
			teamsManager.explosions = true;
			teamsManager.driveablesBreakBlocks = true;
			teamsManager.bombsEnabled = true;
			teamsManager.bulletsEnabled = true;
			teamsManager.forceAdventureMode = false;
			teamsManager.overrideHunger = false;
			teamsManager.canBreakGuns = true;
			teamsManager.canBreakGlass = true;
			teamsManager.armourDrops = true;
			teamsManager.weaponDrops = 1;
			teamsManager.vehiclesNeedFuel = true;
			teamsManager.mgLife = teamsManager.planeLife = teamsManager.vehicleLife = teamsManager.aaLife = teamsManager.mechaLove = 0;
			teamsManager.messageAll("Flan's Mod switching to survival presets");
			return 0;
		}
		if(split[0].equals("arena"))
		{
			teamsManager.explosions = false;
			teamsManager.driveablesBreakBlocks = false;
			teamsManager.bombsEnabled = true;
			teamsManager.bulletsEnabled = true;
			teamsManager.forceAdventureMode = true;
			teamsManager.overrideHunger = true;
			teamsManager.canBreakGuns = true;
			teamsManager.canBreakGlass = false;
			teamsManager.armourDrops = false;
			teamsManager.weaponDrops = 2;
			teamsManager.vehiclesNeedFuel = false;
			teamsManager.mgLife = teamsManager.planeLife = teamsManager.vehicleLife = teamsManager.aaLife = teamsManager.mechaLove = 120;
			TeamsManager.messageAll("Flan's Mod switching to arena mode presets");
			return 0;
		}
		if(split[0].equals("motd"))
		{
			teamsManager.motd = "";
			for(int i = 0; i < split.length - 1; i++)
			{
				teamsManager.motd += split[i + 1];
				if(i != split.length - 2)
				{
					teamsManager.motd += " ";
				}
			}
			sender.sendSystemMessage(Component.literal("Server message of the day is now:"));
			sender.sendSystemMessage(Component.literal(teamsManager.motd));
			return 0;
		}
		if(split[0].equals("listGametypes"))
		{
			sender.sendSystemMessage(Component.literal("\u00a72Showing all avaliable gametypes"));
			sender.sendSystemMessage(Component.translatable("\u00a72To pick a gametype, use \"/teams setGametype <gametype>\" with the name in brackets"));
			for(Gametype gametype : Gametype.gametypes.values())
			{
				sender.sendSystemMessage(Component.literal("\u00a7f" + gametype.name + " (" + gametype.shortName + ")"));
			}
			return 0;
		}
		/*
		No longer used
		if(split[0].equals("setGametype"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.translatable("\u00a74To set the gametype, use \"/teams setGametype <gametype>\" with a valid gametype."));
				return 0;
			}
			if(split[1].toLowerCase().equals("none"))
			{
				if(teamsManager.currentGametype != null)
					teamsManager.currentGametype.stopGametype();
				teamsManager.currentGametype = null;
				for(PlayerData data : PlayerHandler.serverSideData.values())
				{
					if(data != null)
						data.team = null;
				}
				return 0;
			}
			Gametype gametype = Gametype.getGametype(split[1]);
			if(gametype == null)
			{
				sender.sendSystemMessage(Component.literal("\u00a74Invalid gametype. To see gametypes available type \"/teams listGametypes\""));
				return 0;
			}
			if(teamsManager.currentGametype != null)
			{
				teamsManager.currentGametype.stopGametype();
			}
			teamsManager.currentGametype = gametype;

			TeamsManager.messageAll("\u00a72" + sender.getCommandSenderName() + "\u00a7f changed the gametype to \u00a72" + gametype.name);
			if(teamsManager.teams != null && gametype.numTeamsRequired == teamsManager.teams.length)
			{
				TeamsManager.messageAll("\u00a7fTeams will remain the same unless altered by an op.");
			}
			else
			{
				teamsManager.teams = new Team[gametype.numTeamsRequired];
				TeamsManager.messageAll("\u00a7fTeams must be reassigned for this gametype. Please wait for an op to do so.");
			}
			gametype.initGametype();
			return 0;
		}*/
		if(split[0].equals("listMaps"))
		{
			if(teamsManager.maps == null)
			{
				sender.sendSystemMessage(Component.literal("The map list is null"));
				return 0;
			}
			sender.sendSystemMessage(Component.literal("\u00a72Listing maps"));
			for(TeamsMap map : teamsManager.maps.values())
			{
				sender.sendSystemMessage(Component.literal((teamsManager.currentRound != null && map == teamsManager.currentRound.map ? "\u00a74" : "") + map.name + " (" + map.shortName + ")"));
			}
			return 0;
		}
		if(split[0].equals("addMap"))
		{
			if(split.length < 3)
			{
				sender.sendSystemMessage(Component.literal("You need to specify a map name"));
				return 0;
			}
			String shortName = split[1];
			String name = split[2];
			for(int i = 3; i < split.length; i++)
			{
				name += " " + split[i];
			}
			teamsManager.maps.put(shortName, new TeamsMap(sender.getLevel(), shortName, name));
			sender.sendSystemMessage(Component.literal("Added new map : " + name + " (" + shortName + ")"));
			return 0;
		}
		if(split[0].equals("removeMap"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("You need to specify a map's short name"));
				return 0;
			}
			if(teamsManager.maps.containsKey(split[1]))
			{
				teamsManager.maps.remove(split[1]);
				sender.sendSystemMessage(Component.literal("Removed map " + split[1]));
			}
			else
			{
				sender.sendSystemMessage(Component.literal("Map (" + split[1] + ") not found"));
			}
			
			return 0;
		}
		if(split[0].equals("setRound"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("You need to specify the round index (see /teams listRounds)"));
				return 0;
			}
			TeamsRound round = teamsManager.rounds.get(Integer.parseInt(split[1]));
			if(round != null)
			{
				teamsManager.nextRound = round;
				TeamsManager.messageAll("\u00a72Next round will be " + round.gametype.shortName + " in " + round.map.name);
			}
			return 0;
		}
		/*
		if(split[0].equals("listTeams"))
		{
			if(teamsManager.currentGametype == null || teamsManager.teams == null)
			{
				sender.sendSystemMessage(Component.literal("\u00a74The gametype is not yet set. Set it by \"/teams setGametype <gametype>\""));
				return 0;
			}
			sender.sendSystemMessage(Component.literal("\u00a72Showing currently in use teams"));
			for(int i = 0; i < teamsManager.teams.length; i++)
			{
				Team team = teamsManager.teams[i];
				if(team == null)
					sender.sendSystemMessage(Component.literal("\u00a7f" + i + " : No team"));
				else
					sender.sendSystemMessage(Component.literal("\u00a7" + team.textColour + i + " : " + team.name + " (" + team.shortName + ")"));
			}
			return 0;
		}
		*/
		if(split[0].equals("listTeams") || split[0].equals("listAllTeams"))
		{
			if(Team.teams.isEmpty())
			{
				sender.sendSystemMessage(Component.literal("\u00a74No teams available. You need a content pack that has some teams with it"));
				return 0;
			}
			sender.sendSystemMessage(Component.literal("\u00a72Showing all avaliable teams"));
			sender.sendSystemMessage(Component.translatable("\u00a72To pick these teams, use /teams setTeams <team1> <team2> with the names in brackets"));
			for(Team team : Team.teams)
			{
				sender.sendSystemMessage(Component.literal("\u00a7" + team.textColour + team.name + " (" + team.shortName + ")"));
			}
			return 0;
		}
		/*
		 * No longer used
		if(split[0].equals("setTeams"))
		{
			if(teamsManager.currentGametype == null || teamsManager.teams == null)
			{
				sender.sendSystemMessage(Component.literal("\u00a74No gametype selected. Please select the gametype with the setGametype command"));
				return 0;
			}
			if(split.length - 1 != teamsManager.teams.length)
			{
				sender.sendSystemMessage(Component.literal("\u00a74Wrong number of teams given. This gametype requires " + teamsManager.teams.length + " teams to work"));
				return 0;
			}
			Team[] teams = new Team[teamsManager.teams.length];
			String teamList = "";
			for(int i = 0; i < split.length - 1; i++)
			{
				Team team = Team.getTeam(split[i + 1]);
				if(team == null)
				{
					sender.sendSystemMessage(Component.literal("\u00a74" + split[i + 1] + " is not a valid team"));
					return 0;
				}
				for(int j = 0; j < i; j++)
				{
					if(team == teams[j])
					{
						sender.sendSystemMessage(Component.literal("\u00a74You may not add " + split[i + 1] + " twice"));
						return 0;
					}
				}
				teams[i] = team;
				teamList += (i == 0 ? "" : (i == split.length - 2 ? " and " : ", ")) + "\u00a7" + team.textColour + team.name + "\u00a7f";
			}
			teamsManager.teams = teams;
			teamsManager.currentGametype.teamsSet();
			TeamsManager.messageAll("\u00a72" + sender.getCommandSenderName() + "\u00a7f changed the teams to be " + teamList);
			return 0;
		}
		*/
		if(split[0].equals("getSticks") || split[0].equals("getOpSticks") || split[0].equals("getOpKit"))
		{
			ServerPlayer player = sender.getEntity() instanceof ServerPlayer ? (ServerPlayer)sender.getEntity() : null;
			if(player != null)
			{
				player.getInventory().add(new ItemStack(FlansMod.opStick));
				player.getInventory().add(new ItemStack(FlansMod.opStick));
				player.getInventory().add(new ItemStack(FlansMod.opStick));
				player.getInventory().add(new ItemStack(FlansMod.opStick));
				sender.sendSystemMessage(Component.literal("\u00a72Enjoy your op sticks."));
				sender.sendSystemMessage(Component.literal("\u00a77The Stick of Connecting connects objects (spawners, banners etc) to bases (flagpoles etc)"));
				sender.sendSystemMessage(Component.literal("\u00a77The Stick of Ownership sets the team that currently owns a base"));
				sender.sendSystemMessage(Component.literal("\u00a77The Stick of Mapping sets the map that a base is currently associated with"));
				sender.sendSystemMessage(Component.literal("\u00a77The Stick of Destruction deletes bases and team objects"));
			}
			return 0;
		}
		if(split[0].toLowerCase().equals("autobalance"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.autoBalance = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Autobalance is now " + (TeamsManager.autoBalance ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("useRotation"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.voting = !Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Voting is now " + (TeamsManager.voting ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("voting"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.voting = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Voting is now " + (TeamsManager.voting ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("listRounds") || split[0].equals("listRotation"))
		{
			sender.sendSystemMessage(Component.literal("\u00a72Current Round List"));
			for(int i = 0; i < TeamsManager.getInstance().rounds.size(); i++)
			{
				TeamsRound entry = TeamsManager.getInstance().rounds.get(i);
				if(entry.map == null)
				{
					sender.sendSystemMessage(Component.literal("Round had null map"));
					return 0;
				}
				if(entry.gametype == null)
				{
					sender.sendSystemMessage(Component.literal("Round had null gametype"));
					return 0;
				}
				String s = i + ". " + entry.map.shortName + ", " + entry.gametype.shortName;
				if(entry == TeamsManager.getInstance().currentRound)
				{
					s = "\u00a74" + s;
				}
				for(int j = 0; j < entry.teams.length; j++)
				{
					s += ", " + entry.teams[j].shortName;
				}
				s += ", " + entry.timeLimit;
				s += ", " + entry.scoreLimit;
				s += ", Pop : " + (int)(entry.popularity * 100F) + "%";
				sender.sendSystemMessage(Component.literal(s));
			}
			return 0;
		}
		if(split[0].equals("removeRound") || split[0].equals("removeMapFromRotation") || split[0].equals("removeFromRotation") || split[0].equals("removeRotation"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <ID>"));
				return 0;
			}
			int map = Integer.parseInt(split[1]);
			sender.sendSystemMessage(Component.literal("Removed map " + map + " (" + TeamsManager.getInstance().rounds.get(map).map.shortName + ") from rotation"));
			TeamsManager.getInstance().rounds.remove(map);
			return 0;
		}
		if(split[0].equals("addMapToRotation") || split[0].equals("addToRotation") || split[0].equals("addRotation") || split[0].equals("addRound"))
		{
			if(split.length < 7)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <Map> <Gametype> <Team1> <Team2> ... <TimeLimit> <ScoreLimit>"));
				return 0;
			}
			TeamsMap map = TeamsManager.getInstance().maps.get(split[1]);
			if(map == null)
			{
				sender.sendSystemMessage(Component.literal("Could not find map : " + split[1]));
				return 0;
			}
			Gametype gametype = Gametype.getGametype(split[2]);
			if(gametype == null)
			{
				sender.sendSystemMessage(Component.literal("Could not find gametype : " + split[2]));
				return 0;
			}
			if(split.length != 5 + gametype.numTeamsRequired)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <Map> <Gametype> <Team1> <Team2> ... <ScoreLimit> <TimeLimit>"));
				return 0;
			}
			Team[] teams = new Team[gametype.numTeamsRequired];
			for(int i = 0; i < teams.length; i++)
			{
				teams[i] = Team.getTeam(split[3 + i]);
			}
			sender.sendSystemMessage(Component.literal("Added map (" + map.shortName + ") to rotation"));
			TeamsManager.getInstance().rounds.add(new TeamsRound(map, gametype, teams, Integer.parseInt(split[3 + gametype.numTeamsRequired]), Integer.parseInt(split[4 + gametype.numTeamsRequired])));
			return 0;
		}
		if(split[0].equals("start") || split[0].equals("begin"))
		{
			teamsManager.start();
			sender.sendSystemMessage(Component.literal("Started teams map rotation"));
			return 0;
		}
		if(split[0].equals("nextMap") || split[0].equals("next") || split[0].equals("nextRound"))
		{
			teamsManager.roundTimeLeft = 1;
			return 0;
		}
		/*
		 * Ignore
		if(split[0].equals("goToMap"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <ID>"));	
				return 0;
			}
			int prevRotation = Integer.parseInt(split[1]) - 1;
			if(prevRotation == -1)
				prevRotation = teamsManager.rotation.size() - 1;
			teamsManager.currentRotationEntry = prevRotation;
			teamsManager.switchToNextGametype();
			return 0;
		}
		*/
		if(split[0].equals("forceAdventure") || split[0].equals("forceAdventureMode"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.forceAdventureMode = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Adventure mode will " + (TeamsManager.forceAdventureMode ? "now" : "no longer") + " be forced"));
			return 0;
		}
		if(split[0].equals("overrideHunger") || split[0].equals("noHunger"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.overrideHunger = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Players will " + (TeamsManager.overrideHunger ? "no longer" : "now") + " get hungry during rounds"));
			return 0;
		}
		if(split[0].equals("explosions"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.explosions = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Expolsions are now " + (TeamsManager.explosions ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("bombs") || split[0].equals("allowBombs"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.bombsEnabled = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Bombs are now " + (TeamsManager.bombsEnabled ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("bullets") || split[0].equals("bulletsEnabled"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.bulletsEnabled = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Bullets are now " + (TeamsManager.bulletsEnabled ? "enabled" : "disabled")));
			return 0;
		}
		if(split[0].equals("canBreakGuns"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.canBreakGuns = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("AAGuns and MGs can " + (TeamsManager.canBreakGuns ? "now" : "no longer") + " be broken"));
			return 0;
		}
		if(split[0].equals("canBreakGlass"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.canBreakGlass = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Glass and glowstone can " + (TeamsManager.canBreakGlass ? "now" : "no longer") + " be broken"));
			return 0;
		}
		if(split[0].equals("armourDrops") || split[0].equals("armorDrops"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.armourDrops = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Armour will " + (TeamsManager.armourDrops ? "now" : "no longer") + " be dropped"));
			return 0;
		}
		if(split[0].equals("weaponDrops"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <on/off/smart>"));
				return 0;
			}
			if(split[1].toLowerCase().equals("on"))
			{
				TeamsManager.weaponDrops = 1;
				sender.sendSystemMessage(Component.literal("Weapons will be dropped normally"));
			}
			else if(split[1].toLowerCase().equals("off"))
			{
				TeamsManager.weaponDrops = 0;
				sender.sendSystemMessage(Component.literal("Weapons will be not be dropped"));
			}
			else if(split[1].toLowerCase().equals("smart"))
			{
				TeamsManager.weaponDrops = 2;
				sender.sendSystemMessage(Component.literal("Smart drops enabled"));
			}
			return 0;
		}
		if(split[0].equals("fuelNeeded"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.vehiclesNeedFuel = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Vehicles will " + (TeamsManager.vehiclesNeedFuel ? "now" : "no longer") + " require fuel"));
			return 0;
		}
		if(split[0].equals("mgLife"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.mgLife = Integer.parseInt(split[1]);
			if(TeamsManager.mgLife > 0)
				sender.sendSystemMessage(Component.literal("MGs will despawn after " + TeamsManager.mgLife + " seconds"));
			else sender.sendSystemMessage(Component.literal("MGs will not despawn"));
			return 0;
		}
		if(split[0].equals("planeLife"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.planeLife = Integer.parseInt(split[1]);
			if(TeamsManager.planeLife > 0)
				sender.sendSystemMessage(Component.literal("Planes will despawn after " + TeamsManager.planeLife + " seconds"));
			else sender.sendSystemMessage(Component.literal("Planes will not despawn"));
			return 0;
		}
		if(split[0].equals("vehicleLife"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.vehicleLife = Integer.parseInt(split[1]);
			if(TeamsManager.vehicleLife > 0)
				sender.sendSystemMessage(Component.literal("Vehicles will despawn after " + TeamsManager.vehicleLife + " seconds"));
			else sender.sendSystemMessage(Component.literal("Vehicles will not despawn"));
			return 0;
		}
		if(split[0].equals("mechaLife"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.mechaLove = Integer.parseInt(split[1]);
			if(TeamsManager.mechaLove > 0)
				sender.sendSystemMessage(Component.literal("Mechas will despawn after " + TeamsManager.mechaLove + " seconds"));
			else sender.sendSystemMessage(Component.literal("Mechas will not despawn"));
			return 0;
		}
		if(split[0].equals("aaLife"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.aaLife = Integer.parseInt(split[1]);
			if(TeamsManager.aaLife > 0)
				sender.sendSystemMessage(Component.literal("AA Guns will despawn after " + TeamsManager.aaLife + " seconds"));
			else sender.sendSystemMessage(Component.literal("AA Guns will not despawn"));
			return 0;
		}
		if(split[0].equals("vehiclesBreakBlocks"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <true/false>"));
				return 0;
			}
			TeamsManager.driveablesBreakBlocks = Boolean.parseBoolean(split[1]);
			sender.sendSystemMessage(Component.literal("Vehicles will " + (TeamsManager.driveablesBreakBlocks ? "now" : "no longer") + " break blocks"));
			return 0;
		}
		if(split[0].equals("scoreDisplayTime"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.scoreDisplayTime = Integer.parseInt(split[1]) * 20;
			sender.sendSystemMessage(Component.literal("Score summary menu will appear for " + TeamsManager.scoreDisplayTime / 20 + " seconds"));
			return 0;
		}
		if(split[0].equals("rankUpdateTime"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.rankUpdateTime = Integer.parseInt(split[1]) * 20;
			sender.sendSystemMessage(Component.literal("Rank update menu will appear for " + TeamsManager.rankUpdateTime / 20 + " seconds"));
			return 0;
		}
		if(split[0].equals("votingTime"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.votingTime = Integer.parseInt(split[1]) * 20;
			sender.sendSystemMessage(Component.literal("Voting menu will appear for " + TeamsManager.votingTime / 20 + " seconds"));
			return 0;
		}
		if(split[0].toLowerCase().equals("autobalancetime"))
		{
			if(split.length != 2)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams " + split[0] + " <time>"));
				return 0;
			}
			TeamsManager.autoBalanceInterval = Integer.parseInt(split[1]) * 20;
			sender.sendSystemMessage(Component.literal("Autobalance will now occur every " + TeamsManager.autoBalanceInterval / 20 + " seconds"));
			return 0;
		}
		if(split[0].equals("setVariable"))
		{
			if(TeamsManager.getInstance().currentRound == null)
			{
				sender.sendSystemMessage(Component.literal("There is no gametype to set variables for"));
				return 0;
			}
			if(split.length != 3)
			{
				sender.sendSystemMessage(Component.literal("Incorrect Usage : Should be /teams setVariable <variable> <value>"));
				return 0;
			}
			if(TeamsManager.getInstance().currentRound.gametype.setVariable(split[1], split[2]))
				sender.sendSystemMessage(Component.literal("Set variable " + split[1] + " in gametype " + TeamsManager.getInstance().currentRound.gametype.shortName + " to " + split[2]));
			else
				sender.sendSystemMessage(Component.literal("Variable " + split[1] + " did not exist in gametype " + TeamsManager.getInstance().currentRound.gametype.shortName));
			return 0;
		}
		if(split[0].toLowerCase().equals("setloadoutpool"))
		{
			LoadoutPool pool = LoadoutPool.GetPool(split[1]);
			if(pool != null)
			{
				TeamsManagerRanked.GetInstance().currentPool = pool;
				sender.sendSystemMessage(Component.literal("Loadout pool set to " + split[1]));
			}
			else
			{
				sender.sendSystemMessage(Component.literal("No such loadout pool"));
			}
			
			return 0;
		}
		if(split[0].toLowerCase().equals("go"))
		{
			TeamsManagerRanked.GetInstance().currentPool = LoadoutPool.GetPool("modernLoadout");
			teamsManager.start();
			return 0;
		}
		if(split[0].toLowerCase().equals("xp"))
		{
			sender.sendSystemMessage(Component.literal("Awarded " + Integer.parseInt(split[1]) + " XP"));
			if(sender.getEntity() instanceof ServerPlayer)
				TeamsManagerRanked.AwardXP((ServerPlayer)sender.getEntity(), Integer.parseInt(split[1]));
			return 0;
		}
		if(split[0].toLowerCase().equals("resetrank"))
		{
			sender.sendSystemMessage(Component.literal("Reset your rank"));
			if(sender.getEntity() instanceof ServerPlayer)
				TeamsManagerRanked.ResetRank((ServerPlayer)sender.getEntity());
			return 0;
		}
		if(split[0].toLowerCase().equals("giverewardbox"))
		{
			String name = split[1];
			RewardBox box = RewardBox.GetRewardBox(split[2]);
			if(box == null)
			{
				sender.sendSystemMessage(Component.literal("Invalid box"));
				return 0;
			}
			
			ServerPlayer profile = FlansMod.serverInstance.getPlayerList().getPlayerByName(name);
			if(profile != null)
			{
				RewardBoxInstance instance = RewardBoxInstance.CreateCheatReward(box, name);
				PlayerRankData data = TeamsManagerRanked.GetRankData(profile.getUUID());
				if(data != null)
				{
					data.AddRewardBoxInstance(instance);
				}
			}
			return 0;
		}
		if(split[0].toLowerCase().equals("xpmultiplier"))
		{
			float target = Float.parseFloat(split[1]);
			if(target < 0.5f || target > 2.0f)
			{
				sender.sendSystemMessage(Component.literal("Not going to allow that for now. Keep it within 0.5 to 2.0"));
			}
			else
			{
				sender.sendSystemMessage(Component.literal("XP multiplier is now " + target));
				TeamsManagerRanked.GetInstance().XPMultiplier = target;
			}
			return 0;
		}
		
		sender.sendSystemMessage(Component.literal(split[0] + " is not a valid teams command. Try /teams help"));
		return 0;
	}
	
	public static void sendHelpInformation(CommandSourceStack sender, int page)
	{
		if(page > 3 || page < 1)
		{
			Component text = Component.literal("Invalid help page, should be in the range (1-3)").withStyle(ChatFormatting.RED);
			sender.sendSystemMessage(text);
			return;
		}
		
		sender.sendSystemMessage(Component.literal("\u00a72Listing teams commands \u00a7f[Page " + page + " of 3]"));
		switch(page)
		{
			case 1:
			{
				sender.sendSystemMessage(Component.literal("/teams help [page]"));
				sender.sendSystemMessage(Component.literal("/teams off"));
				sender.sendSystemMessage(Component.literal("/teams arena"));
				sender.sendSystemMessage(Component.literal("/teams survival"));
				sender.sendSystemMessage(Component.literal("/teams getSticks"));
				sender.sendSystemMessage(Component.literal("/teams listGametypes"));
				//sender.sendSystemMessage(Component.literal("/teams setGametype <name>"));
				//sender.sendSystemMessage(Component.literal("/teams listAllTeams"));
				sender.sendSystemMessage(Component.literal("/teams listTeams"));
				//sender.sendSystemMessage(Component.literal("/teams setTeams <teamName1> <teamName2>"));
				sender.sendSystemMessage(Component.literal("/teams addMap <shortName> <longName>"));
				sender.sendSystemMessage(Component.literal("/teams listMaps"));
				sender.sendSystemMessage(Component.literal("/teams removeMap <shortName>"));
				break;
			}
			case 2:
			{
				
				//sender.sendSystemMessage(Component.literal("/teams setMap <shortName>"));
				sender.sendSystemMessage(Component.literal("/teams useRotation <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams voting <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams addRound <map> <gametype> <team1> <team2> <TimeLimit> <ScoreLimit>"));
				sender.sendSystemMessage(Component.literal("/teams listRounds"));
				sender.sendSystemMessage(Component.literal("/teams removeRound <ID>"));
				sender.sendSystemMessage(Component.literal("/teams nextMap"));
				//sender.sendSystemMessage(Component.literal("/teams goToMap <ID>"));
				sender.sendSystemMessage(Component.literal("/teams votingTime <time>"));
				sender.sendSystemMessage(Component.literal("/teams scoreDisplayTime <time>"));
				break;
			}
			case 3:
			{
				sender.sendSystemMessage(Component.literal("/teams setVariable <variable> <value>"));
				sender.sendSystemMessage(Component.literal("/teams forceAdventure <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams overrideHunger <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams explosions <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams canBreakGuns <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams canBreakGlass <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams armourDrops <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams weaponDrops <off / on / smart>"));
				sender.sendSystemMessage(Component.literal("/teams fuelNeeded <true / false>"));
				sender.sendSystemMessage(Component.literal("/teams mgLife <time>"));
				sender.sendSystemMessage(Component.literal("/teams planeLife <time>"));
				sender.sendSystemMessage(Component.literal("/teams vehicleLife <time>"));
				sender.sendSystemMessage(Component.literal("/teams aaLife <time>"));
				
				sender.sendSystemMessage(Component.literal("/teams vehiclesBreakBlocks <true / false>"));
				break;
			}
		}
	}
	
	public static ServerPlayer getPlayer(String name)
	{
		return FlansMod.serverInstance.getPlayerList().getPlayerByName(name);
	}
}
