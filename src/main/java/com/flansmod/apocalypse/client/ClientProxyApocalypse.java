package com.flansmod.apocalypse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import com.flansmod.apocalypse.client.model.RenderFakePlayer;
import com.flansmod.apocalypse.client.model.RenderNukeDrop;
import com.flansmod.apocalypse.client.model.RenderPowerCube;
import com.flansmod.apocalypse.client.model.RenderSkullBoss;
import com.flansmod.apocalypse.client.model.RenderSkullDrone;
import com.flansmod.apocalypse.client.model.RenderSurvivor;
import com.flansmod.apocalypse.client.model.RenderTeleporter;
import com.flansmod.apocalypse.common.CommonProxyApocalypse;
import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.entity.EntityNukeDrop;
import com.flansmod.common.ModEntities;

public class ClientProxyApocalypse extends CommonProxyApocalypse
{
	public static int apocalypseCountdown = 0;
	
	@Override
	public void preInit()
	{
		super.preInit();
		
		EntityRendererRegistry.register(ModEntities.SURVIVOR, RenderSurvivor::new);
		EntityRendererRegistry.register(ModEntities.FAKE_PLAYER, RenderFakePlayer::new);
		EntityRendererRegistry.register(ModEntities.TELEPORTER, RenderTeleporter::new);
		EntityRendererRegistry.register(ModEntities.NUKE_DROP, RenderNukeDrop::new);
		EntityRendererRegistry.register(ModEntities.SKULL_BOSS, RenderSkullBoss::new);
		EntityRendererRegistry.register(ModEntities.SKULL_DRONE, RenderSkullDrone::new);
		
		BlockEntityRendererRegistry.register(FlansModApocalypse.POWER_CUBE_BE, RenderPowerCube::new);
		
		// TODO APOCALYPSE: 1.12.2 registered models via ModelLoader.setCustomModelResourceLocation;
		// model registration is handled by vanilla block/item model json now
		
		ClientTickEvents.END_CLIENT_TICK.register(mc ->
		{
			if(apocalypseCountdown > 0)
			{
				apocalypseCountdown--;
			}
		});
		
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, "apocalypse_overlay"), (extractor, deltaTracker) -> renderOverlay(extractor));
	}
	
	private static void renderOverlay(net.minecraft.client.gui.GuiGraphicsExtractor extractor)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null || mc.level == null)
			return;
		
		//DEBUG vehicles
		if(apocalypseCountdown > 0 && FlansModApocalypse.DEBUG)
		{
			extractor.text(mc.font, "Seconds to the apocalypse: " + (apocalypseCountdown / 20), 2, 2, 0xffffff);
		}
		
		//Draw white screen if we are being nuked
		// TODO APOCALYPSE: 1.12.2 used a RenderGameOverlayEvent + immediate mode quad; replaced with a full-screen fill
		boolean playerIsInExplosion = false;
		for(net.minecraft.world.entity.Entity nukeEntity : mc.level.getEntities(mc.player, AABB.ofSize(Vec3.ZERO, 1e9D, 1e9D, 1e9D), entity -> entity instanceof EntityNukeDrop))
		{
			EntityNukeDrop nuke = (EntityNukeDrop)nukeEntity;
			float scale = 1F - 1F / ((float)nuke.timeSinceExplosion / 5F + 1);
			scale *= 100F * scale;
			float alpha = ((float)nuke.timeSinceExplosion / (float)EntityNukeDrop.explosionLength);
			alpha = 1F - alpha * alpha;
			alpha *= 0.5F;
			if(mc.player.distanceToSqr(nuke) < scale * scale)
			{
				playerIsInExplosion = true;
				break;
			}
		}
		if(playerIsInExplosion)
		{
			extractor.fill(0, 0, extractor.guiWidth(), extractor.guiHeight(), 0x80FFFFFF);
		}
	}
	
	@Override
	public void init()
	{
		super.init();
		
		Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, "skullboss_laugh"), SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, "skullboss_laugh")));
		Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, "skullboss_spawn"), SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, "skullboss_spawn")));
	}
	
	@Override
	public void postInit()
	{
		//FlansMod.getPacketHandler().registerPacket(PacketApocalypseCountdown.class);
	}
	
	public static void updateApocalypseCountdownTimer(int i)
	{
		apocalypseCountdown = i;
	}
}
