package com.flansmod.client.gui.teams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerLoadout;
import com.flansmod.common.teams.PlayerRankData;
import com.flansmod.common.types.EnumPaintjobRarity;

public class GuiTeamsBase extends Screen
{
	/**
	 * Gui origin
	 */
	protected int guiOriginX, guiOriginY;
	
	protected Minecraft mc;
	protected Player player;
	
	public GuiTeamsBase()
	{
		super(Component.literal(""));
		player = Minecraft.getInstance().player;
	}
	
	@Override
	public void init()
	{
		super.init();
	}
	
	private static final Identifier loudoutBoxes = Identifier.fromNamespaceAndPath("flansmod", "gui/landingpage.png");
	private static final Identifier ranks = Identifier.fromNamespaceAndPath("flansmod", "gui/ranks.png");
	private static final Identifier loadoutEditor = Identifier.fromNamespaceAndPath("flansmod", "gui/loadouteditor.png");
	
	protected void DrawLoadoutPanel(GuiGraphicsExtractor extractor, LoadoutPool pool, PlayerRankData data, int i, int j, int n)
	{
		int textureX = 512;
		int textureY = 256;
		
		if(data.currentLevel >= pool.slotUnlockLevels[n])
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, loudoutBoxes, i, j, 7 + 49 * n, 28, 46, 111, textureX, textureY);
			
			PlayerLoadout loadout = data.loadouts[n];
			if(loadout != null)
			{
				DrawGun(extractor, loadout.slots[0], i + 20, j + 28, 16f);
				DrawGun(extractor, loadout.slots[1], i + 20, j + 46, 16f);
				
				drawSlotInventory(extractor, loadout.slots[0], i + 6, j + 54);
				drawSlotInventory(extractor, loadout.slots[1], i + 24, j + 54);
				drawSlotInventory(extractor, loadout.slots[2], i + 6, j + 72);
				drawSlotInventory(extractor, loadout.slots[3], i + 24, j + 72);
			}
		}
		else
		{
			
			extractor.blit(RenderPipelines.GUI_TEXTURED, loudoutBoxes, i, j, 259, 28, 46, 111, textureX, textureY);
			extractor.centeredText(font, "Unlocks", i + 23, j + 23, 0xffffff);
			extractor.centeredText(font, "at " + pool.slotUnlockLevels[n], i + 23, j + 40, 0xffffff);
		}
		
		extractor.centeredText(font, "Slot " + (n + 1), i + 23, j + 5, 0xffffff);
	}
	
	protected void DrawRarityBackground(GuiGraphicsExtractor extractor, EnumPaintjobRarity rarity, int i, int j)
	{
		int textureX = 512;
		int textureY = 256;
		
		if(rarity != EnumPaintjobRarity.UNKNOWN)
		{
			int x = 0, y = 71;
			switch(rarity)
			{
				case COMMON: x = 331;
					break;
				case UNCOMMON: x = 349;
					break;
				case RARE: x = 367;
					break;
				case LEGENDARY:
				{
					x = 385;
					break;
				}
				default: break;
			}
			if(x > 0)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, loadoutEditor, i, j, x, y, 16, 16, textureX, textureY);
			}
		}
	}
	
	protected void DrawGun(GuiGraphicsExtractor extractor, ItemStack stack, int x, int y, float scale)
	{
		drawSlotInventory(extractor, stack, x, y);
	}
	
	protected void DrawRankIcon(GuiGraphicsExtractor extractor, int rank, int prestige, int x, int y, boolean doubleSize)
	{
		if(doubleSize)
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, ranks, guiOriginX + x, guiOriginY + y, rank * 32, prestige * 32, 32, 32, 1024, 512);
		}
		else
			extractor.blit(RenderPipelines.GUI_TEXTURED, ranks, guiOriginX + x, guiOriginY + y, rank * 16, prestige * 16, 16, 16, 512, 256);
	}
	
	/**
	 * Item stack renderering method
	 */
	protected void drawSlotInventory(GuiGraphicsExtractor extractor, ItemStack itemstack, int i, int j)
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
		if(event.key() == GLFW.GLFW_KEY_ESCAPE)
		{
			if(AllowEscape())
			{
				return super.keyPressed(event);
			}
			return true;
		}
		return super.keyPressed(event);
	}
	
	protected boolean AllowEscape()
	{
		return false;
	}
}
