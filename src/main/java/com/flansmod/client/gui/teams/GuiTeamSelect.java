package com.flansmod.client.gui.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.flansmod.common.FlansMod;
import com.flansmod.common.network.PacketTeamSelect;
import com.flansmod.common.teams.PlayerClass;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;

public class GuiTeamSelect extends Screen
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/teams.png");
	
	private boolean classMenu;
	//This is static so that players may switch teams whenever they wish. 
	//This is updated because the server forces players to pick teams when the teams change
	public static Team[] teamChoices;
	private PlayerClass[] classChoices;
	
	private int guiHeight;
	
	//For changing team when you want to, as opposed to when the server forces you to.
	public GuiTeamSelect()
	{
		super(Component.literal(""));
		if(teamChoices == null)
		{
			Minecraft.getInstance().setScreen(null);
			return;
		}
		classMenu = false;
		guiHeight = 29 + 24 * teamChoices.length;
	}
	
	public GuiTeamSelect(Team[] teams)
	{
		super(Component.literal(""));
		classMenu = false;
		teamChoices = teams;
		guiHeight = 29 + 24 * teams.length;
	}
	
	public GuiTeamSelect(PlayerClass[] classes)
	{
		super(Component.literal(""));
		classMenu = true;
		classChoices = classes;
		guiHeight = 29 + 24 * classes.length;
	}
	
	@Override
	public void init()
	{
		super.init();
		if(classMenu)
		{
			for(int i = 0; i < classChoices.length; i++)
			{
				final int choice = i;
				if(classChoices[i] != null)
					addRenderableWidget(Button.builder(Component.literal(classChoices[i].name), b -> actionPerformed(choice)).bounds(width / 2 - 128 + 9, height / 2 - guiHeight / 2 + 24 + 24 * i, 73, 20).build());
			}
		}
		else
		{
			if(teamChoices == null)
			{
				Minecraft.getInstance().setScreen(null);
				return;
			}
			for(int i = 0; i < teamChoices.length; i++)
			{
				final int choice = i;
				if(teamChoices[i] != null)
					addRenderableWidget(Button.builder(Component.literal("\u00a7" + teamChoices[i].textColour + teamChoices[i].name), b -> actionPerformed(choice)).bounds(width / 2 - 128 + 10, height / 2 - guiHeight / 2 + 24 + 24 * i, 236, 20).build());
				else
					addRenderableWidget(Button.builder(Component.literal("No Team / Builder"), b -> actionPerformed(choice)).bounds(width / 2 - 128 + 10, height / 2 - guiHeight / 2 + 24 + 24 * i, 236, 20).build());
			}
		}
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		//TODO : Draw the inventory BG and slots for the class menu
		extractMenuBackground(extractor);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 128, height / 2 - guiHeight / 2, 0F, 0F, 256, 22, 256, 256);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 128, height / 2 + guiHeight / 2 - 6, 0F, 73F, 256, 7, 256, 256);
		if(classMenu)
		{
			for(int n = 0; n < classChoices.length; n++)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 128, height / 2 - guiHeight / 2 + 22 + 24 * n, 0F, 23F, 256, 24, 256, 256);
			}
		}
		else
		{
			for(int n = 0; n < teamChoices.length; n++)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 128, height / 2 - guiHeight / 2 + 22 + 24 * n, 0F, 48F, 256, 24, 256, 256);
			}
		}
		extractor.text(font, classMenu ? "Choose a Class" : "Choose a Team", width / 2 - 120, height / 2 - guiHeight / 2 + 8, 0xffffff, true);
		if(classMenu)
		{
			for(int n = 0; n < classChoices.length; n++)
			{
				for(int m = 0; m < classChoices[n].startingItems.size(); m++)
				{
					drawSlotInventory(extractor, classChoices[n].startingItems.get(m), width / 2 - 128 + 85 + 18 * m, height / 2 - guiHeight / 2 + 26 + 24 * n);
				}
			}
		}
	}
	
	private void actionPerformed(int id)
	{
		if(classMenu)
		{
			FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(classChoices[id].shortName, true));
			Minecraft.getInstance().setScreen(null);
		}
		else
		{
			TeamsManager.getInstance().SelectTeam(teamChoices[id]);
			
		}
	}
	
	private void drawSlotInventory(GuiGraphicsExtractor extractor, ItemStack itemstack, int i, int j)
	{
		if(itemstack == null || itemstack.isEmpty())
			return;
		extractor.item(itemstack, i, j);
		extractor.itemDecorations(font, itemstack, i, j);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		if(event.key() == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(event))
		{
			Minecraft.getInstance().setScreen(null);
			if(classMenu)
			{
				if(classChoices != null && classChoices.length > 0)
					FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(classChoices[0].shortName, true));
			}
			else FlansMod.getPacketHandler().sendToServer(new PacketTeamSelect(Team.spectators.shortName, false));
		}
		return true;
	}
	
	@Override
	public void onClose()
	{
	
	}
	
}
