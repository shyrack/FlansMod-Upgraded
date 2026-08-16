package com.flansmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourBoxType.ArmourBoxEntry;

public class GuiArmourBox extends Screen
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/armourbox.png");
	private Inventory inventory;
	private ArmourBoxType type;
	private int page;
	private int guiOriginX;
	private int guiOriginY;
	private int scroll;
	
	public GuiArmourBox(Inventory playerinventory, ArmourBoxType type)
	{
		super(Component.literal(""));
		inventory = playerinventory;
		this.type = type;
		page = 0;
	}

	@Override
	public void tick()
	{
		super.tick();
		scroll++;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		extractMenuBackground(extractor);
		int k = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int l = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		Font fontrenderer = font;
		int m = guiOriginX = k / 2 - 88;
		int n = guiOriginY = l / 2 - 91;
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m, n, 0F, 0F, 176, 182, 256, 256);
		
		extractor.centeredText(fontrenderer, Component.literal(type.name), k / 2, n + 5, 0xffffff);
		
		// Grey out buttons when they are unavaliable
		if(page == 0)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m + 77, n + 87, 176F, 0F, 10, 10, 256, 256);
		if(page >= type.pages.size() - 1)
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, m + 89, n + 87, 186F, 0F, 10, 10, 256, 256);

		// Fill the gun panels with guns
		drawRecipe(extractor, fontrenderer, m, n, page);
		// Draw the inventory slots (not real slots)
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				drawSlotInventory(extractor, inventory.getItem(col + (row + 1) * 9), m + 8 + col * 18, n + 100 + row * 18);
			}
		}
		for(int col = 0; col < 9; col++)
		{
			drawSlotInventory(extractor, inventory.getItem(col), m + 8 + col * 18, n + 158);
		}
	}
	
	private void drawRecipe(GuiGraphicsExtractor extractor, Font fontrenderer, int m, int n, int q)
	{
		ArmourBoxEntry page = type.pages.get(q);
		if(page != null)
		{
			//Iterate over x
			for(int i = 0; i < 2; i++)
			{
				//Iterate over y
				for(int j = 0; j < 2; j++)
				{
					if(page.armours[i * 2 + j] != null)
					{
						drawSlotInventory(extractor, new ItemStack(page.armours[i * 2 + j].item), m + 9 + 83 * i, n + 44 + 22 * j);
						int numParts = page.requiredStacks[i * 2 + j].size();
						//Find which 3 parts to render
						int startPart = 0;
						if(numParts >= 4)
						{
							startPart = (scroll / 40) % (numParts - 2);
						}
						
						for(int p = 0; p < (numParts < 3 ? numParts : 3); p++)
						{
							drawSlotInventory(extractor, page.requiredStacks[i * 2 + j].get(startPart + p), m + 30 + p * 19 + 83 * i, n + 44 + 22 * j);
						}
					}
				}
			}

			//Draw the armour name at the top
			extractor.centeredText(fontrenderer, Component.literal(page.name), m + 87, n + 25, 0xffffff);
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
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int i = (int)event.x();
		int j = (int)event.y();
		int k = event.button();
		int m = i - guiOriginX;
		int n = j - guiOriginY;
		if(k == 0 || k == 1)
		{
			// Back button
			if(m > 77 && m < 87 && n > 87 && n < 97)
			{
				if(page > 0)
					page--;
			}

			// Forwards button
			if(m > 89 && m < 99 && n > 87 && n < 97)
			{
				if(page < type.pages.size() - 1)
					page++;
			}

			// Gun 1
			//Iterate over x
			for(int x = 0; x < 2; x++)
			{
				//Iterate over y
				for(int y = 0; y < 2; y++)
				{
					if(type.pages.get(page).armours[x * 2 + y] != null && m > 7 + 83 * x && m < 27 + 83 * x && n > 42 + 22 * y && n < 62 + 22 * y)
					{
						type.block.buyArmour(type.pages.get(page).shortName, x * 2 + y, inventory);
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		if(event.key() == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(event))
		{
			Minecraft.getInstance().setScreen(null);
		}
		return true;
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

}
