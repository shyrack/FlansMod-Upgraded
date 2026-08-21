package com.flansmod.client.gui;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.network.PacketDriveableGUI;

public class GuiDriveableRepair extends Screen
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/repair.png");
	
	/**
	 * The player using this GUI
	 */
	private Player driver;
	/**
	 * The driveable (s)he is driving
	 */
	private EntityDriveable driving;
	/**
	 * The list of parts that are actually damageable
	 */
	private ArrayList<DriveablePart> partsToDraw = new ArrayList<>();
	
	/**
	 * The list of repair buttons
	 */
	private ArrayList<Button> buttonList = new ArrayList<>();
	/**
	 * Gui origin
	 */
	private int guiOriginX, guiOriginY;
	
	public GuiDriveableRepair(Player player)
	{
		super(Component.literal(""));
		driver = player;
		driving = ((EntitySeat)player.getVehicle()).driveable;
		for(DriveablePart part : driving.getDriveableData().parts.values())
		{
			//Check to see if the part is actually damageable
			if(part.maxHealth > 0)
			{
				//Add it to the list of parts to draw
				partsToDraw.add(part);
			}
		}
	}
	
	@Override
	public void init()
	{
		super.init();
		buttonList.clear();
		for(int i = 0; i < partsToDraw.size(); i++)
		{
			final int index = i;
			buttonList.add(addRenderableWidget(Button.builder(Component.literal("Repair"), b -> FlansMod.proxy.repairDriveable(driver, driving, partsToDraw.get(index))).bounds(0, 0, 55, 20).build()));
		}
	}

	private void updateButtons()
	{
		int y = 43;
		for(int i = 0; i < partsToDraw.size(); i++)
		{
			DriveablePart part = partsToDraw.get(i);
			Button button = buttonList.get(i);
			button.visible = part.health <= 0;
			button.setX(guiOriginX + 9);
			button.setY(guiOriginY + y);
			y += part.health <= 0 ? 40 : 20;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		int guiWidth = 202;
		//Work out the guiHeight by adding what is necessary for each part
		int guiHeight = 31;
		for(DriveablePart part : partsToDraw)
		{
			//Add to the GUI height depending on whether we need a repair button or not
			guiHeight += part.health <= 0 ? 40 : 20;
		}
		//Update the buttons
		updateButtons();

		//Standard GUI render stuff
		extractMenuBackground(extractor);
		int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		
		//Calculate the gui origin
		guiOriginX = w / 2 - guiWidth / 2;
		guiOriginY = h / 2 - guiHeight / 2;
		
		//EntityRenderer the header
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, 202, 23, 256, 256);
		//EntityRenderer the footer
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY + guiHeight - 8, 0F, 65F, 202, 8, 256, 256);
		//EntityRenderer the title
		extractor.text(font, driving.getDriveableType().name + " - Repair", guiOriginX + 7, guiOriginY + 7, 0xffffff);
		
		//EntityRenderer each part
		//Where to start rendering from. Updated with each part
		int y = 23;
		for(DriveablePart part : partsToDraw)
		{
			boolean broken = part.health <= 0;
			//EntityRenderer the background for this section
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY + y, 0F, 24F, 202, broken ? 40 : 20, 256, 256);
			
			//EntityRenderer the damage bar
			float percentHealth = (float)part.health / (float)part.maxHealth;
			int barColor = 0xFF000000 | ((int)((1F - percentHealth) * 255F) << 16) | ((int)(percentHealth * 255F) << 8);
			extractor.fill(guiOriginX + 121, guiOriginY + y + 2, guiOriginX + 121 + (int)(70 * percentHealth), guiOriginY + y + 18, barColor);
			
			//Write the part name and percent health
			extractor.text(font, part.type.getName(), guiOriginX + 10, guiOriginY + y + 6, 0xffffff);
			extractor.centeredText(font, (int)(percentHealth * 100F) + "%", guiOriginX + 158, guiOriginY + y + 6, 0xffffff);
			
			//If the part is damaged, draw the parts required to fix it
			if(broken)
			{
				//Create a temporary copy of the player inventory in order to work out whether the player has each of the itemstacks required
				Inventory temporaryInventory = new Inventory(driver, new EntityEquipment());
				temporaryInventory.replaceWith(driver.getInventory());
				
				ArrayList<ItemStack> stacksNeeded = driving.getDriveableType().getItemsRequired(part, driving.getDriveableData().engine);
				//Draw the stacks that should be in each slot
				for(int n = 0; n < 7; n++)
				{
					//If there are more than 7 stacks, loop over them
					int stackNum = n + (FlansMod.ticker / 60) % Math.max(1, stacksNeeded.size() - 6);
					//If this is a valid stack
					if(stackNum < stacksNeeded.size())
					{
						//Get the item stack we need
						ItemStack stackNeeded = stacksNeeded.get(stackNum);
						//The total amount of items found that match this recipe stack
						int totalAmountFound = 0;
						//Iterate over the temporary inventory
						for(int m = 0; m < temporaryInventory.getContainerSize(); m++)
						{
							//Get the stack in each slot
							ItemStack stackInSlot = temporaryInventory.getItem(m).copy();
							//If the stack is what we want
							if(stackInSlot.getItem() == stackNeeded.getItem() && stackInSlot.getDamageValue() == stackNeeded.getDamageValue())
							{
								//Work out the amount to take from the stack
								int amountFound = Math.min(stackInSlot.getCount(), stackNeeded.getCount() - totalAmountFound);
								//Take it
								stackInSlot.setCount(stackInSlot.getCount() - amountFound);
								//Check for empty stacks
								if(stackInSlot.getCount() <= 0)
									stackInSlot = ItemStack.EMPTY.copy();
								//Put the modified stack back in the inventory
								temporaryInventory.setItem(m, stackInSlot);
								//Increase the amount found counter
								totalAmountFound += amountFound;
								//If we have enough, stop looking
								if(totalAmountFound == stackNeeded.getCount())
									break;
							}
						}
						//If we did not find enough in the inventory
						if(totalAmountFound < stackNeeded.getCount())
						{
							extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX + 67 + 18 * n, guiOriginY + y + 22, 202F, 0F, 16, 16, 256, 256);
						}
						drawSlotInventory(extractor, stacksNeeded.get(stackNum), guiOriginX + 67 + 18 * n, guiOriginY + y + 22);
					}
				}
			}
			
			//Increase the render y value for the next part
			y += broken ? 40 : 20;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int m = (int)event.x() - guiOriginX;
		int n = (int)event.y() - guiOriginY;
		if(m > 185 && m < 195 && n > 5 && n < 15)
			if(driving instanceof EntityMecha)
			{
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MECHA));
			}
			else
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MENU));
		return true;
	}

	/**
	 * Item stack renderering method
	 */
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
}
