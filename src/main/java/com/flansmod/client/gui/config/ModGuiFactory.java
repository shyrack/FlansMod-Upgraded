package com.flansmod.client.gui.config;

import net.minecraft.client.gui.screens.Screen;

/**
 * Formerly the Forge ModGuiFactory entry point. Fabric has no equivalent
 * registration; the config screen is exposed through this static helper so
 * that mod menu integrations can open it.
 */
public class ModGuiFactory
{
	public static Screen createConfigGui(Screen parentScreen)
	{
		return new ModGuiConfig(parentScreen);
	}
}
