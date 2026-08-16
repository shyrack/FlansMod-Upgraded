package com.flansmod.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.network.PacketTeamInfo;
import com.flansmod.common.network.PacketTeamInfo.PlayerScoreData;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.types.InfoType;

/**
 * HUD rendering hooks: crosshair/hit marker, scope and helmet overlays, ammo
 * readout, team scoreboard, kill messages and vehicle debug readout.
 */
public class ClientRenderHooks implements HudElement
{
	public static final Identifier hitMarker = Identifier.fromNamespaceAndPath("flansmod", "gui/hitmarker.png");
	private Minecraft mc;
	private float partialTicks;
	private static List<KillMessage> killMessages = new ArrayList<>();

	public ClientRenderHooks()
	{
		mc = Minecraft.getInstance();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("flansmod", "hud"), this);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker)
	{
		int i = extractor.guiWidth();
		int j = extractor.guiHeight();

		renderScopeOverlay(extractor, i, j);
		renderHitMarker(extractor, i, j);
		renderPlayerAmmo(extractor, i, j);
		renderTeamInfo(extractor, i, j);
		renderKillMessages(extractor, i, j);
		renderVehicleDebug(extractor);
	}

	public void update()
	{
		for(Iterator<KillMessage> it = killMessages.iterator(); it.hasNext(); )
		{
			KillMessage message = it.next();
			message.timer--;
			if(message.timer == 0)
			{
				it.remove();
			}
		}
	}

	public void setPartialTick(float partialTick)
	{
		this.partialTicks = partialTick;
	}

	public void updatePlayerView()
	{
	}

	private void renderScopeOverlay(GuiGraphicsExtractor extractor, int i, int j)
	{
		// Scopes and helmet overlays
		String overlayTexture = null;
		if(FlansModClient.currentScope != null && FlansModClient.currentScope.hasZoomOverlay()
				&& Minecraft.getInstance().screen == null && FlansModClient.zoomProgress > 0.8F)
		{
			overlayTexture = FlansModClient.currentScope.getZoomOverlay();
		}
		else if(mc.player != null)
		{
			ItemStack stack = mc.player.getItemBySlot(EquipmentSlot.HEAD);
			if(stack.getItem() instanceof ItemTeamArmour)
			{
				overlayTexture = ((ItemTeamArmour)stack.getItem()).type.overlay;
			}
		}

		if(overlayTexture != null)
		{
			Identifier texture = FlansModResourceHandler.getScope(overlayTexture);
			if(texture != null)
				extractor.blit(texture, 0, 0, i, j, 0F, 0F, 1F, 1F);
		}
	}

	private void renderHitMarker(GuiGraphicsExtractor extractor, int i, int j)
	{
		if(FlansModClient.hitMarkerTime > 0)
		{
			extractor.blit(hitMarker, i / 2 - 5, j / 2 - 5, 9, 9, 0F, 0F, 1F, 1F);
		}
	}

	private void renderPlayerAmmo(GuiGraphicsExtractor extractor, int i, int j)
	{
		// Player ammo overlay
		if(mc.player != null)
		{
			Font font = mc.font;
			int iNumHandsUsed = 0;

			for(InteractionHand hand : InteractionHand.values())
			{
				ItemStack stack = mc.player.getItemInHand(hand);
				if(stack.getItem() instanceof ItemGun)
				{
					GunType gunType = ((ItemGun)stack.getItem()).GetType();
					if(gunType.oneHanded)
						iNumHandsUsed++;
					else
						iNumHandsUsed += 2;
				}
			}

			if(iNumHandsUsed > 2)
			{
				extractor.text(font, "Too many guns, not enough hands", i / 2 - 85, j - 35, 0x000000);
				extractor.text(font, "Too many guns, not enough hands", i / 2 - 86, j - 36, 0xffffff);
			}
			else
			{
				for(InteractionHand hand : InteractionHand.values())
				{
					ItemStack stack = mc.player.getItemInHand(hand);
					if(stack.getItem() instanceof ItemGun)
					{
						GunType gunType = ((ItemGun)stack.getItem()).GetType();
						int x = 0;
						for(int n = 0; n < gunType.numAmmoItemsInGun; n++)
						{
							ItemStack bulletStack = ((ItemGun)stack.getItem()).getBulletItemStack(stack, n);
							if(bulletStack != null && !bulletStack.isEmpty()
									&& bulletStack.getDamageValue() < bulletStack.getMaxDamage())
							{
								int xPos = hand == InteractionHand.MAIN_HAND ? i / 2 + 16 + x : i / 2 - 32 - x;
								extractor.item(bulletStack, xPos, j - 65);
								String s = (bulletStack.getMaxDamage() - bulletStack.getDamageValue()) + "/" +
										bulletStack.getMaxDamage();
								if(bulletStack.getMaxDamage() == 1)
									s = "";

								xPos = hand == InteractionHand.MAIN_HAND ? i / 2 + 32 + x : i / 2 - 16 - x;
								extractor.text(font, s, xPos, j - 59, 0x000000);
								extractor.text(font, s, xPos + 1, j - 60, 0xffffff);
								x += 16 + font.width(s);
							}
						}
					}
				}
			}
		}
	}

	private void renderTeamInfo(GuiGraphicsExtractor extractor, int i, int j)
	{
		PacketTeamInfo teamInfo = FlansModClient.teamInfo;

		if(teamInfo != null && Minecraft.getInstance().player != null
				&& (teamInfo.numTeams > 0 || !teamInfo.sortedByTeam)
				&& PacketTeamInfo.getPlayerScoreData(Minecraft.getInstance().player.getName().getString()) != null)
		{
			Font font = mc.font;
			// If we are in a two team gametype, draw the team scores at the top of the screen
			if(teamInfo.numTeams == 2 && teamInfo.sortedByTeam)
			{
				if(teamInfo.teamData == null || teamInfo.teamData[0] == null || teamInfo.teamData[0].team == null ||
						teamInfo.teamData[1] == null || teamInfo.teamData[1].team == null)
				{
					FlansMod.Assert(false, "Failure in team data overlay");
					return;
				}

				// Draw the team scores
				if(teamInfo.teamData[0] != null && teamInfo.teamData[1] != null)
				{
					extractor.text(font, teamInfo.teamData[0].score + "", i / 2 - 35, 9, 0x000000);
					extractor.text(font, teamInfo.teamData[0].score + "", i / 2 - 36, 8, 0xffffff);
					extractor.text(font, teamInfo.teamData[1].score + "",
							i / 2 + 35 - font.width(teamInfo.teamData[1].score + ""), 9, 0x000000);
					extractor.text(font, teamInfo.teamData[1].score + "",
							i / 2 + 34 - font.width(teamInfo.teamData[1].score + ""), 8, 0xffffff);
				}
			}

			extractor.text(font, teamInfo.gametype + "", i / 2 + 48, 9, 0x000000);
			extractor.text(font, teamInfo.gametype + "", i / 2 + 47, 8, 0xffffff);
			extractor.text(font, teamInfo.map + "", i / 2 - 47 - font.width(teamInfo.map + ""), 9, 0x000000);
			extractor.text(font, teamInfo.map + "", i / 2 - 48 - font.width(teamInfo.map + ""), 8, 0xffffff);

			int secondsLeft = teamInfo.timeLeft / 20;
			int minutesLeft = secondsLeft / 60;
			secondsLeft = secondsLeft % 60;
			String timeLeft = minutesLeft + ":" + (secondsLeft < 10 ? "0" + secondsLeft : secondsLeft);
			extractor.text(font, timeLeft, i / 2 - font.width(timeLeft) / 2 - 1, 29, 0x000000);
			extractor.text(font, timeLeft, i / 2 - font.width(timeLeft) / 2, 30, 0xffffff);

			String playerUsername = Minecraft.getInstance().player.getName().getString();
			PlayerScoreData data = PacketTeamInfo.getPlayerScoreData(playerUsername);
			if(data != null)
			{
				extractor.text(font, data.score + "", i / 2 - 7, 1, 0x000000);
				extractor.text(font, data.kills + "", i / 2 - 7, 9, 0x000000);
				extractor.text(font, data.deaths + "", i / 2 - 7, 17, 0x000000);
			}
		}
	}

	private void renderKillMessages(GuiGraphicsExtractor extractor, int i, int j)
	{
		Font font = mc.font;
		for(KillMessage killMessage : killMessages)
		{
			String message = "\u00a7" + killMessage.killerName + (killMessage.headshot ? "         " : "     ") +
					"\u00a7" + killMessage.killedName;
			extractor.text(font, message, i - font.width(message) - 6, j - 32 - killMessage.line * 16, 0xffffff);
		}

		for(KillMessage killMessage : killMessages)
		{
			extractor.item(new ItemStack(killMessage.weapon.item), i - font.width(
					(killMessage.headshot ? "         " : "     ") + killMessage.killedName),
					j - 36 - killMessage.line * 16);
			if(killMessage.headshot)
				extractor.item(new ItemStack(FlansMod.crosshairsymbol),
						i - font.width("     " + killMessage.killedName),
						j - 36 - killMessage.line * 16);
		}
	}

	private void renderVehicleDebug(GuiGraphicsExtractor extractor)
	{
		// DEBUG vehicles
		if(mc.player != null && mc.player.getVehicle() instanceof EntitySeat)
		{
			EntityDriveable ent = ((EntitySeat)mc.player.getVehicle()).driveable;

			if(ent != null)
			{
				double dX = ent.getX() - ent.xo;
				double dY = ent.getY() - ent.yo;
				double dZ = ent.getZ() - ent.zo;

				// Convert to chunks per Minecraft hour
				float speed = (float)Math.sqrt(dX * dX + dY * dY + dZ * dZ) * 1000F / 16F;

				speed = (int)(speed * 10F) / 10F;

				extractor.text(mc.font, "Speed: " + speed + " chunks per hour", 2, 2, 0xffffff);

				if(FlansMod.DEBUG)
				{
					extractor.text(mc.font, "Throttle : " + ent.throttle, 2, 12, 0xffffff);
				}
			}
		}
	}

	public static void addKillMessage(boolean headshot, InfoType infoType, String killer, String killed)
	{
		for(KillMessage killMessage : killMessages)
		{
			killMessage.line++;
			if(killMessage.line > 10)
				killMessage.timer = 0;
		}
		killMessages.add(new KillMessage(headshot, infoType, killer, killed));
	}

	private static class KillMessage
	{
		public KillMessage(boolean head, InfoType infoType, String killer, String killed)
		{
			headshot = head;
			killerName = killer;
			killedName = killed;
			weapon = infoType;
			line = 0;
			timer = 200;

			// Get the player and see if they're still holding the gun they used to kill this player.
			// From that we can work out the paintjob
			if(Minecraft.getInstance().level != null)
			{
				for(Player o : Minecraft.getInstance().level.players())
				{
					if(o.getName().getString().equals(killer))
					{
						ItemStack stack = o.getMainHandItem();
						if(stack.getItem() instanceof com.flansmod.common.paintjob.IPaintableItem)
						{
							paint = stack.getDamageValue();
							break;
						}
					}
				}
			}
		}

		public String killerName = "";
		public String killedName = "";
		public InfoType weapon = null;
		public int paint = 0;
		public int timer = 0;
		public int line = 0;
		public boolean headshot;
	}
}
