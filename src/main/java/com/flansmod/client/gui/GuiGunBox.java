package com.flansmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.boxes.ContainerGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.guns.boxes.GunBoxType.GunBoxEntry;
import com.flansmod.common.guns.boxes.GunBoxType.GunBoxEntryTopLevel;
import com.flansmod.common.guns.boxes.GunBoxType.GunBoxPage;
import com.flansmod.common.network.PacketBuyWeapon;
import com.flansmod.common.types.InfoType;

public class GuiGunBox extends AbstractContainerScreen<ContainerGunBox>
{
	private static final int numCategories = 4;
	/**
	 * Texture location
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/weaponboxnew.png");
	/**
	 * Texture sizes
	 */
	private final int textureX = 512, textureY = 256;
	private Inventory inventory;
	private GunBoxType type;
	private int pageScroller;
	private GunBoxPage currentPage;
	private GunBoxEntryTopLevel currentEntry;
	private GunBoxEntry currentSubEntry;
	private int guiOriginX;
	private int guiOriginY;
	private int scroll;
	private Button craftLeft, craftRight, categoryLeft, categoryRight;
	private Button[] categories = new Button[numCategories];
	
	public GuiGunBox(Inventory inventory, GunBoxType type)
	{
		super(new ContainerGunBox(inventory), inventory, Component.literal(""), 256, 256);
		this.inventory = inventory;
		this.type = type;
		pageScroller = 0;
		
		currentPage = type.pages.get(0);
	}
	
	@Override
	public void containerTick()
	{
		super.containerTick();
		scroll++;
		
		if(craftLeft != null && craftRight != null)
		{
			craftLeft.visible = currentEntry != null;
			craftRight.visible = currentSubEntry != null;

			
			if(currentEntry != null)
			{
				craftLeft.active = currentEntry.canCraft(inventory, false);
			}
			
			if(currentSubEntry != null)
			{
				craftRight.active = currentSubEntry.canCraft(inventory, false);
			}
		}
		

	}

	
	@Override
	public void init()
	{
		super.init();
		
		craftLeft = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> actionPerformed(0)).bounds(width / 2 - 119, height / 2 + 15, 87, 20).build());
		craftLeft.visible = false;
		
		craftRight = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> actionPerformed(1)).bounds(width / 2 + 33, height / 2 + 15, 87, 20).build());
		craftRight.visible = false;

		categoryLeft = addRenderableWidget(Button.builder(Component.literal("<"), b -> actionPerformed(2)).bounds(width / 2 - 119, height / 2 - 122, 20, 20).build());
		categoryLeft.active = false;
		
		categoryRight = addRenderableWidget(Button.builder(Component.literal(">"), b -> actionPerformed(3)).bounds(width / 2 + 99, height / 2 - 122, 20, 20).build());
		categoryRight.active = type.pages.size() > (pageScroller + 1) * numCategories;
		
		for(int i = 0; i < numCategories; i++)
		{
			final int category = i;
			if(pageScroller * numCategories + i < type.pages.size())
			{
				categories[i] = addRenderableWidget(Button.builder(Component.literal(type.pages.get(pageScroller * numCategories + i).name), b -> actionPerformed(4 + category))
						.bounds(width / 2 - numCategories * 30 + i * 60, height / 2 - 100, 60, 20).build());
			}
			else
			{
				categories[i] = addRenderableWidget(Button.builder(Component.literal("NONE"), b -> actionPerformed(4 + category))
						.bounds(width / 2 - numCategories * 30 + i * 60, height / 2 - 100, 60, 20).build());
				categories[i].visible = false;
			}
		}

	}
	
	private void actionPerformed(int id)
	{
		if(categories == null)
		{
			return;
		}
		switch(id)
		{
			case 0: //Left
				FlansMod.getPacketHandler().sendToServer(new PacketBuyWeapon(type, currentEntry.type));
				break;
			case 1: //Right
				FlansMod.getPacketHandler().sendToServer(new PacketBuyWeapon(type, currentSubEntry.type));
				break;
			case 2: //Left
				if(pageScroller > 0)
					pageScroller--;
				break;
			case 3: //Right
				if(type.pages.size() > (pageScroller + 1) * numCategories)
					pageScroller++;
				break;
			default:
				currentPage = type.pages.get(pageScroller * numCategories + id - 4);
				currentEntry = currentPage.entries.size() == 0 ? null : currentPage.entries.get(0);
				currentSubEntry = currentEntry == null ? null : (currentEntry.childEntries.size() == 0 ? null : currentEntry.childEntries.get(0));

		}
		
		categoryLeft.active = pageScroller > 0;
		categoryRight.active = type.pages.size() > (pageScroller + 1) * numCategories;
		
		for(int i = 0; i < numCategories; i++)
		{
			if(pageScroller * numCategories + i < type.pages.size())
			{
				categories[i].visible = true;
				categories[i].setMessage(Component.literal(type.pages.get(pageScroller * numCategories + i).name));
			}
			else categories[i].visible = false;
		}
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		int originX = guiOriginX = leftPos;
		int originY = guiOriginY = topPos;
		
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX, originY, 0F, 0F, imageWidth, imageHeight, textureX, textureY);
		
		if(currentPage != null)
		{
			if(currentEntry != null)
			{
				int currentEntryIndex = currentPage.entries.indexOf(currentEntry);
				
				//EntityRenderer sub entry selection boxes
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 130, originY + 54, 290F, 4F, 24, 112, textureX, textureY);
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 95, originY + 57 + currentEntryIndex * 22, 318F, 28F, 38, 18, textureX, textureY);
				
				//Loop twice for bg texture and item
				for(int i = 0; i < 5; i++)
				{
					if(i >= currentEntry.childEntries.size())
						break;
					GunBoxEntry subEntry = currentEntry.childEntries.get(i);
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 133, originY + 57 + i * 22, 319F, 8F, 18, 18, textureX, textureY);
				}
				
				if(currentSubEntry != null)
				{
					int currentSubEntryIndex = currentEntry.childEntries.indexOf(currentSubEntry);
					// EntityRenderer right panel thing
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 132, originY + 55 + currentSubEntryIndex * 22, 327F, 48F, 29, 22, textureX, textureY);
					
					
					//EntityRenderer right panel bg
					renderPanelBackground(extractor, currentSubEntry, originX + 161, originY + 57);
				}
				
				//EntityRenderer left panel for bg 
				renderPanelBackground(extractor, currentEntry, originX + 8, originY + 57);

				//EntityRenderer left panel detail
				renderPanelForeground(extractor, currentEntry, originX + 8, originY + 57);

				if(currentSubEntry != null)
				{
					//EntityRenderer right panel detail
					renderPanelForeground(extractor, currentSubEntry, originX + 161, originY + 57);
				}
				
				
				for(int i = 0; i < 5; i++)
				{
					if(i >= currentEntry.childEntries.size())
						break;
					GunBoxEntry subEntry = currentEntry.childEntries.get(i);
					renderInfoType(extractor, subEntry.type, originX + 134, originY + 58 + i * 22);
				}
			}
			
			//EntityRenderer options
			for(int i = 0; i < 5; i++)
			{
				if(i >= currentPage.entries.size())
					break;
				GunBoxEntryTopLevel entry = currentPage.entries.get(i);

				renderInfoType(extractor, entry.type, originX + 106, originY + 58 + i * 22);
			}
		}
		int stringWidth = font.width(type.name);
		extractor.text(font, type.name, originX + imageWidth / 2 - stringWidth / 2, originY + 8, 0x00000000);
		extractor.text(font, type.name, originX + imageWidth / 2 - stringWidth / 2 + 1, originY + 7, 0xffffffff);
	}
	
	private void renderInfoType(GuiGraphicsExtractor extractor, InfoType type, int x, int y)
	{
		if(type == null)
		{
			//FlansMod.log.warn("Null type when rendering!");
			return;
		}
		drawSlotInventory(extractor, new ItemStack(type.item), x, y);
	}
	
	private void renderPanelBackground(GuiGraphicsExtractor extractor, GunBoxEntry entry, int x, int y)
	{
		int numParts = entry.requiredParts.size();
		
		int numPartsOnLine1 = Math.min(numParts, 4);
		int numPartsOnLine2 = numParts > 4 ? Math.min(numParts - 4, 4) : 0;
		
		for(int i = 0; i < numPartsOnLine1; i++)
		{
			if(entry.haveEnoughOf(inventory, entry.requiredParts.get(i)))
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, x + 5 + 20 * i, y + 64, 294F, 142F, 18, 18, textureX, textureY);
			else 
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, x + 5 + 20 * i, y + 64, 276F, 142F, 18, 18, textureX, textureY);
		}
		
		//if(numPartsOnLine1 > 0)
		//	drawModalRectWithCustomSizedTexture(x + 5, y + 44, 276, 122, 18 + 20 * (numPartsOnLine1 - 1), 18, textureX, textureY);
		//if(numPartsOnLine2 > 0)
		//	drawModalRectWithCustomSizedTexture(x + 5, y + 64, 276, 122, 18 + 20 * (numPartsOnLine2 - 1), 18, textureX, textureY);
	}
	
	private void renderPanelForeground(GuiGraphicsExtractor extractor, GunBoxEntry entry, int x, int y)
	{
		if(entry == null || entry.type == null)
		{
			return;
		}
		
		String bufferLine = "";
		String bufferLine2 = "";
		String bufferArray[] = entry.type.name.split(" ");
		
		for(String aBufferArray : bufferArray)
		{
			if((bufferLine.length() + aBufferArray.length()) <= 16)
				bufferLine += aBufferArray + " ";
			else
				bufferLine2 += aBufferArray + " ";
		}

		extractor.text(font, bufferLine, x + 5, y + 5, 0x00000000);
		extractor.text(font, bufferLine2, x + 5, y + 15, 0x00000000);
		
		if(entry.type instanceof GunType)
		{
			GunType gun = (GunType)entry.type;
			
			extractor.text(font, "Damage: ", x + 5, y + 25, 0x00000000);
			String tempString = "" + gun.damage;
			extractor.text(font, tempString, x + 85 - font.width(tempString), y + 25, 0x00000000);
			
			extractor.text(font, "Spread: ", x + 5, y + 35, 0x00000000);
			tempString = "" + gun.bulletSpread;
			extractor.text(font, tempString, x + 85 - font.width(tempString), y + 35, 0x00000000);

			if(gun.shootDelay > 0)
			{
				extractor.text(font, "RoF: ", x + 5, y + 45, 0x00000000);
				tempString = String.format("%.0f RPM", 60f * 20f / gun.shootDelay);
				extractor.text(font, tempString, x + 85 - font.width(tempString), y + 45, 0x00000000);
			}
		}
		else if(entry.type instanceof ShootableType)
		{
			ShootableType gun = (ShootableType)entry.type;
			
			extractor.text(font, "No. Rounds: ", x + 5, y + 25, 0x00000000);
			String tempString = "" + gun.roundsPerItem;
			extractor.text(font, tempString, x + 85 - font.width(tempString), y + 25, 0x00000000);
			
			if(gun.numBullets > 1)
			{
				extractor.text(font, "Pellets: ", x + 5, y + 35, 0x00000000);
				tempString = "" + gun.numBullets;
				extractor.text(font, tempString, x + 85 - font.width(tempString), y + 35, 0x00000000);
			}
			else if(gun.fireRadius > 0f)
			{
				extractor.text(font, "Creates Fire", x + 5, y + 35, 0x00000000);
			}			
			else if(gun.explosionRadius > 0f)
			{
				extractor.text(font, "Explosion: ", x + 5, y + 35, 0x00000000);
				tempString = "" + gun.explosionRadius;
				extractor.text(font, tempString, x + 85 - font.width(tempString), y + 35, 0x00000000);
			}
			
			if(gun.damageVsDriveable > 1.0f)
			{
				extractor.text(font, "Anti-Tank: ", x + 5, y + 45, 0x00000000);
				tempString = String.format("x%.0f", gun.damageVsDriveable);
				extractor.text(font, tempString, x + 85 - font.width(tempString), y + 45, 0x00000000);
			}
			else if(gun.damageVsLiving > 1.0f)
			{
				extractor.text(font, "Anti-Person: ", x + 5, y + 45, 0x00000000);
				tempString = String.format("x%.0f", gun.damageVsLiving);
				extractor.text(font, tempString, x + 85 - font.width(tempString), y + 45, 0x00000000);
			}
		}
		
		extractor.text(font, "Cost", x + 5, y + 55, 0x00000000);
		
		int numParts = entry.requiredParts.size();
		
		int numPartsOnLine1 = Math.min(numParts, 4);
		int numPartsOnLine2 = numParts > 4 ? Math.min(numParts - 4, 4) : 0;
		
		for(int i = 0; i < numPartsOnLine1; i++)
		{
			drawSlotInventory(extractor, entry.requiredParts.get(i), x + 6 + 20 * i, y + 65);
		}
		//for(int i = 0; i < numPartsOnLine2; i++)
		//{
		//	drawSlotInventory(entry.requiredParts.get(i + 4), x + 6 + 20 * i, y + 65);
		//}
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
		int m = (int)event.x() - guiOriginX;
		int n = (int)event.y() - guiOriginY;
		int k = event.button();
		if(k == 0 || k == 1)
		{
			if(currentPage != null)
			{
				for(int e = 0; e < 5; e++)
				{
					if(e < currentPage.entries.size() && 105 < m && m < 123 && 57 + e * 22 < n && n < 76 + e * 22)
					{
						currentEntry = currentPage.entries.get(e);
						currentSubEntry = currentEntry.childEntries.size() > 0 ? currentEntry.childEntries.get(0) : null;
					}
				}
			}
			
			if(currentEntry != null)
			{
				for(int e = 0; e < 5; e++)
				{
					if(e < currentEntry.childEntries.size() && 133 < m && m < 151 && 57 + e * 22 < n && n < 76 + e * 22)
					{
						currentSubEntry = currentEntry.childEntries.get(e);
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
