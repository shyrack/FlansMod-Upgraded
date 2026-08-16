package com.flansmod.client.gui;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import com.flansmod.api.IControllable;
import com.flansmod.client.FlansModClient;
import com.flansmod.client.handlers.KeyInputHandler;
import com.flansmod.common.FlansMod;

public class GuiDriveableController extends Screen
{
	private IControllable plane;
	private boolean leftMouseHeld;
	private boolean rightMouseHeld;
	private double lastMouseX, lastMouseY;
	
	public GuiDriveableController(IControllable thePlane)
	{
		super(Component.literal(""));
		plane = thePlane;
	}
	
	@Override
	public void init()
	{
		if(Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK)
			Minecraft.getInstance().setCameraEntity((plane.getCamera() == null ? Minecraft.getInstance().player : plane.getCamera()));
	}
	
	@Override
	public void onClose()
	{
		Minecraft.getInstance().mouseHandler.releaseMouse();
		Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
	}
	
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		int i = event.key();
		
		if(i == GLFW.GLFW_KEY_F2)
		{
			Minecraft mc = Minecraft.getInstance();
			mc.options.setCameraType(mc.options.getCameraType().cycle());
			if(mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK)
				mc.setCameraEntity((plane.getCamera() == null ? mc.player : plane.getCamera()));
			else mc.setCameraEntity(mc.player);
		}
		if(KeyInputHandler.debugKey.matches(event))
		{
			FlansMod.DEBUG = !FlansMod.DEBUG;
		}
		if(KeyInputHandler.reloadModelsKey.matches(event))
		{
			FlansModClient.reloadModels(false);
		}
		return true;
	}
	
	@Override
	public void tick()
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK)
			mc.setCameraEntity((plane.getCamera() == null ? mc.player : plane.getCamera()));
		else mc.setCameraEntity(mc.player);
		
		Player player = (Player)plane.getControllingEntity();
		if(player != mc.player)
		{
			mc.setScreen(null);
			return;
		}
		if(!mc.mouseHandler.isMouseGrabbed())
		{
			mc.mouseHandler.grabMouse();
		}
		
		//Right mouse. Fires shells, drops bombs. Is not a holding thing
		if(mc.mouseHandler.isRightPressed())
			plane.pressKey(8, player, true);
		
		if(!leftMouseHeld && mc.mouseHandler.isLeftPressed()) //Left mouse, for MGs. Is a holding thing
		{
			leftMouseHeld = true;
			plane.updateKeyHeldState(9, true);
		}
		if(leftMouseHeld && !mc.mouseHandler.isLeftPressed())
		{
			leftMouseHeld = false;
			plane.updateKeyHeldState(9, false);
		}
		if(!rightMouseHeld && mc.mouseHandler.isRightPressed()) //Right mouse
		{
			rightMouseHeld = true;
			plane.updateKeyHeldState(8, true);
		}
		if(rightMouseHeld && !mc.mouseHandler.isRightPressed())
		{
			rightMouseHeld = false;
			plane.updateKeyHeldState(8, false);
		}
		
		if(plane != null && !plane.isDead() && plane.getControllingEntity() != null && plane.getControllingEntity() instanceof Player)
		{
			if(mc.options.keyUp.isDown())
			{
				plane.pressKey(0, player, true);
			}
			if(mc.options.keyDown.isDown())
			{
				plane.pressKey(1, player, true);
			}
			if(mc.options.keyLeft.isDown())
			{
				plane.pressKey(2, player, true);
			}
			if(mc.options.keyRight.isDown())
			{
				plane.pressKey(3, player, true);
			}
			if(mc.options.keyJump.isDown())
			{
				plane.pressKey(4, player, true);
			}
			if(KeyInputHandler.downKey.isDown())
			{
				plane.pressKey(5, player, true);
			}
			if(mc.options.keyShift.isDown())
			{
				plane.pressKey(6, player, true);
			}
			if(KeyInputHandler.vehicleMenuKey.isDown())
			{
				plane.pressKey(7, player, true);
			}
			if(KeyInputHandler.bombKey.isDown())
			{
				plane.pressKey(8, player, true);
			}
			if(KeyInputHandler.gunKey.isDown())
			{
				plane.pressKey(9, player, true);
			}
			if(KeyInputHandler.controlSwitchKey.isDown())
			{
				plane.pressKey(10, player, true);
			}
			if(KeyInputHandler.leftRollKey.isDown())
			{
				plane.pressKey(11, player, true);
			}
			if(KeyInputHandler.rightRollKey.isDown())
			{
				plane.pressKey(12, player, true);
			}
			if(KeyInputHandler.gearKey.isDown())
			{
				plane.pressKey(13, player, true);
			}
			if(KeyInputHandler.doorKey.isDown())
			{
				plane.pressKey(14, player, true);
			}
			if(KeyInputHandler.modeKey.isDown())
			{
				plane.pressKey(15, player, true);
			}
			
		}
		else
		{
			mc.setScreen(null);
		}
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY)
	{
		Minecraft mc = Minecraft.getInstance();
		float scale = mc.getWindow().getGuiScale();
		int dx = (int)Math.round((mouseX - lastMouseX) * scale);
		int dy = (int)Math.round((mouseY - lastMouseY) * scale);
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		
		if(plane != null)
		{
			plane.onMouseMoved(dx, dy);
		}
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
