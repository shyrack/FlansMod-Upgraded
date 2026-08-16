package com.flansmod.client.handlers;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import com.mojang.blaze3d.platform.InputConstants;

import com.flansmod.api.IControllable;
import com.flansmod.client.FlansModClient;
import com.flansmod.client.gui.teams.GuiLandingPage;
import com.flansmod.client.gui.teams.GuiTeamScores;
import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.GunAnimations.LookAtState;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.enchantments.EnchantmentModule;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.network.PacketReload;
import com.flansmod.common.network.PacketRequestDebug;

public class KeyInputHandler
{
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.MISC;

	public static KeyMapping downKey = new KeyMapping("key.pitchDown.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_CONTROL,
			CATEGORY);
	public static KeyMapping vehicleMenuKey = new KeyMapping("key.vehicleMenu.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_M,
			CATEGORY);
	public static KeyMapping bombKey = new KeyMapping("key.dropBomb.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			CATEGORY);
	public static KeyMapping gunKey = new KeyMapping("key.fireVehicleGuns.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			CATEGORY);
	public static KeyMapping controlSwitchKey = new KeyMapping("key.switchControlMode.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_C,
			CATEGORY);
	public static KeyMapping reloadKey = new KeyMapping("key.reload.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			CATEGORY);
	public static KeyMapping teamsMenuKey = new KeyMapping("key.teamsMenu.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			CATEGORY);
	public static KeyMapping teamsScoresKey = new KeyMapping("key.teamsScores.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			CATEGORY);
	public static KeyMapping leftRollKey = new KeyMapping("key.rollLeft.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_Z,
			CATEGORY);
	public static KeyMapping rightRollKey = new KeyMapping("key.rollRight.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_X,
			CATEGORY);
	public static KeyMapping gearKey = new KeyMapping("key.toggleLandingGear.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_L,
			CATEGORY);
	public static KeyMapping doorKey = new KeyMapping("key.toggleDoors.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			CATEGORY);
	public static KeyMapping modeKey = new KeyMapping("key.switchMovementMode.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_J,
			CATEGORY);
	public static KeyMapping lookAtGunKey = new KeyMapping("key.lookAtGun.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_L,
			CATEGORY);
	public static KeyMapping debugKey = new KeyMapping("key.debug.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F10,
			CATEGORY);
	public static KeyMapping reloadModelsKey = new KeyMapping("key.reloadModels.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F9,
			CATEGORY);
	public static KeyMapping toggleCameraPerspective = new KeyMapping("key.toggleCameraPerspective.desc",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F5,
			CATEGORY);
	public static KeyMapping primaryVehicleInteract = new KeyMapping("key.primaryVehicleInteract.desc",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_LEFT,
			CATEGORY);
	public static KeyMapping secondaryVehicleInteract = new KeyMapping("key.secondaryVehicleInteract.desc",
			InputConstants.Type.MOUSE,
			GLFW.GLFW_MOUSE_BUTTON_RIGHT,
			CATEGORY);

	private Minecraft mc;

	public static void init()
	{
		KeyMappingHelper.registerKeyMapping(downKey);
		KeyMappingHelper.registerKeyMapping(vehicleMenuKey);
		KeyMappingHelper.registerKeyMapping(bombKey);
		KeyMappingHelper.registerKeyMapping(gunKey);
		KeyMappingHelper.registerKeyMapping(controlSwitchKey);
		KeyMappingHelper.registerKeyMapping(reloadKey);
		KeyMappingHelper.registerKeyMapping(teamsMenuKey);
		KeyMappingHelper.registerKeyMapping(teamsScoresKey);
		KeyMappingHelper.registerKeyMapping(leftRollKey);
		KeyMappingHelper.registerKeyMapping(rightRollKey);
		KeyMappingHelper.registerKeyMapping(gearKey);
		KeyMappingHelper.registerKeyMapping(doorKey);
		KeyMappingHelper.registerKeyMapping(modeKey);
		KeyMappingHelper.registerKeyMapping(lookAtGunKey);
		KeyMappingHelper.registerKeyMapping(debugKey);
		KeyMappingHelper.registerKeyMapping(reloadModelsKey);
		KeyMappingHelper.registerKeyMapping(primaryVehicleInteract);
		KeyMappingHelper.registerKeyMapping(secondaryVehicleInteract);
	}

	KeyInputHandler()
	{
		mc = Minecraft.getInstance();
	}

	void checkTickKeys()
	{
		Player player = mc.player;
		if(player == null)
		{
			return;
		}

		Entity ridingEntity = player.getVehicle();
		if(ridingEntity instanceof IControllable)
		{
			IControllable controllable = (IControllable)ridingEntity;
			if(mc.options.keyUp.isDown())
				controllable.pressKey(0, player, false);
			if(mc.options.keyDown.isDown())
				controllable.pressKey(1, player, false);
			if(mc.options.keyLeft.isDown())
				controllable.pressKey(2, player, false);
			if(mc.options.keyRight.isDown())
				controllable.pressKey(3, player, false);
			if(mc.options.keyJump.isDown())
				controllable.pressKey(4, player, false);
			if(downKey.isDown())
				controllable.pressKey(5, player, false);
			if(secondaryVehicleInteract.isDown())
				controllable.pressKey(8, player, false);
			if(primaryVehicleInteract.isDown())
				controllable.pressKey(9, player, false);
			if(leftRollKey.isDown())
				controllable.pressKey(11, player, false);
			if(rightRollKey.isDown())
				controllable.pressKey(12, player, false);
		}
	}

	void checkEventKeys()
	{
		if(mc.screen instanceof ChatScreen || mc.screen != null)
			return;

		Player player = mc.player;

		if(teamsMenuKey.consumeClick())
		{
			mc.setScreen(new GuiLandingPage());
			return;
		}
		if(teamsScoresKey.consumeClick())
		{
			mc.setScreen(new GuiTeamScores());
			return;
		}
		if(reloadKey.consumeClick())
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			ItemStack stack = player.getMainHandItem();

			if(data.shootTimeRight <= 0.0f)
			{
				if(stack.getItem() instanceof ItemGun)
				{
					ItemGun item = (ItemGun)stack.getItem();
					GunType type = item.GetType();

					if(item.CanReload(stack, player.getInventory()))
					{
						FlansMod.getPacketHandler().sendToServer(new PacketReload(InteractionHand.MAIN_HAND, true));

						// Set player shoot delay to be the reload delay
						// Set both gun delays to avoid reloading two guns at once
						data.shootTimeRight = data.shootTimeLeft = (int)type.getReloadTime(stack);

						float reloadDelay = EnchantmentModule.ModifyReloadTime(type.reloadTime, player, player.getOffhandItem());

						GunAnimations animations = FlansModClient.getGunAnimations(player, InteractionHand.MAIN_HAND);

						int pumpDelay = type.model == null ? 0 : type.model.pumpDelayAfterReload;
						int pumpTime = type.model == null ? 1 : type.model.pumpTime;
						animations.doReload(type.reloadTime, pumpDelay, pumpTime);

						data.reloadingRight = true;
						data.burstRoundsRemainingRight = 0;
					}
				}
			}
			return;
		}
		if(lookAtGunKey.consumeClick())
		{
			FlansModClient.getGunAnimations(mc.player, InteractionHand.MAIN_HAND).lookAt = LookAtState.TILT1;
			FlansModClient.getGunAnimations(mc.player, InteractionHand.OFF_HAND).lookAt = LookAtState.TILT1;
			return;
		}
		if(debugKey.consumeClick())
		{
			if(FlansMod.DEBUG)
				FlansMod.DEBUG = false;
			else
			{
				FlansMod.packetHandler.sendToServer(new PacketRequestDebug());
			}
			return;
		}
		if(reloadModelsKey.consumeClick())
		{
			FlansModClient.reloadModels(InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT));
			return;
		}

		if(player == null)
		{
			return;
		}

		Entity ridingEntity = player.getVehicle();
		if(ridingEntity instanceof IControllable)
		{
			IControllable controllable = (IControllable)ridingEntity;
			if(mc.options.keyShift.consumeClick())
				controllable.pressKey(6, player, true);
			if(vehicleMenuKey.consumeClick())
				controllable.pressKey(7, player, true);
			if(primaryVehicleInteract.consumeClick())
				controllable.pressKey(9, player, true);
			if(secondaryVehicleInteract.consumeClick())
				controllable.pressKey(8, player, true);
			if(controlSwitchKey.consumeClick())
				controllable.pressKey(10, player, true);
			if(gearKey.consumeClick())
				controllable.pressKey(13, player, true);
			if(doorKey.consumeClick())
				controllable.pressKey(14, player, true);
			if(modeKey.consumeClick())
				controllable.pressKey(15, player, true);
			if(toggleCameraPerspective.isDown())
				controllable.pressKey(18, player, true);
		}
	}
}
