package com.flansmod.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.util.FlansModUtil;
import com.flansmod.common.guns.ContainerGunModTable;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.network.PacketGunPaint;

public class GuiGunModTable extends AbstractContainerScreen<ContainerGunModTable>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/guntable.png");
	private Paintjob hoveringOver = null;
	private int mouseX, mouseY;
	private Inventory inventory;
	
	public GuiGunModTable(Inventory inv, Level w)
	{
		super(new ContainerGunModTable(inv, w), inv, Component.literal(""), 176, 256);
		inventory = inv;
	}
	
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		extractor.text(font, "Inventory", 8, (imageHeight - 94) + 2, 0x404040);
		extractor.text(font, "Gun Modification Table", 8, 6, 0x404040);
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);
		int xOrigin = leftPos;
		int yOrigin = topPos;
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin, yOrigin, 0F, 0F, imageWidth, imageHeight, 256, 256);
		
		ItemStack gunStack = menu.getSlot(0).getItem();
		if(gunStack != null && gunStack.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)gunStack.getItem()).GetType();
			if(gunType.allowBarrelAttachments)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 51, yOrigin + 107, 176, 122, 22, 22, 256, 256);
			}
			if(gunType.allowScopeAttachments)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 77, yOrigin + 81, 202, 96, 22, 22, 256, 256);
			}
			if(gunType.allowStockAttachments)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 103, yOrigin + 107, 228, 122, 22, 22, 256, 256);
			}
			if(gunType.allowGripAttachments)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 77, yOrigin + 133, 202, 148, 22, 22, 256, 256);
			}
			
			//EntityRenderer generic slot backgrounds
			for(int x = 0; x < 2; x++)
			{
				for(int y = 0; y < 4; y++)
				{
					if(x + y * 2 < gunType.numGenericAttachmentSlots)
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 9 + 18 * x, yOrigin + 82 + 18 * y, 178, 54, 18, 18, 256, 256);
				}
			}
			
			int numPaintjobs = gunType.paintjobs.size();
			int numRows = numPaintjobs / 2 + 1;
			
			for(int y = 0; y < numRows; y++)
			{
				for(int x = 0; x < 2; x++)
				{
					//If this row has only one paintjob, don't try and render the second one
					if(2 * y + x >= numPaintjobs)
						continue;
					
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 131 + 18 * x, yOrigin + 82 + 18 * y, 178, 54, 18, 18, 256, 256);
				}
			}
			
			for(int y = 0; y < numRows; y++)
			{
				for(int x = 0; x < 2; x++)
				{
					//If this row has only one paintjob, don't try and render the second one
					if(2 * y + x >= numPaintjobs)
						continue;
					
					Paintjob paintjob = gunType.paintjobs.get(2 * y + x);
					ItemStack stack = gunStack.copy();
					//FlansModUtil.getItemTag(stack).putString("Paint", paintjob.iconName);
					stack.setDamageValue(paintjob.ID);
					extractor.item(stack, xOrigin + 132 + x * 18, yOrigin + 83 + y * 18);
				}
			}
		}
		
		//Draw hover box for paintjob
		if(hoveringOver != null)
		{
			int numDyes = hoveringOver.dyesNeeded.length;
			//Only draw box if there are dyes needed
			if(numDyes != 0 && !inventory.player.getAbilities().instabuild)
			{
				//Calculate which dyes we have in our inventory
				boolean[] haveDyes = new boolean[numDyes];
				for(int n = 0; n < numDyes; n++)
				{
					int amountNeeded = hoveringOver.dyesNeeded[n].getCount();
					for(int s = 0; s < inventory.getContainerSize(); s++)
					{
						ItemStack stack = inventory.getItem(s);
						if(stack != null && stack.getItem() == hoveringOver.dyesNeeded[n].getItem() && stack.getDamageValue() == hoveringOver.dyesNeeded[n].getDamageValue())
						{
							amountNeeded -= stack.getCount();
						}
					}
					if(amountNeeded <= 0)
						haveDyes[n] = true;
				}
				
				int originX = this.mouseX + 6;
				int originY = this.mouseY - 20;
				
				//If we have only one, use the double ended slot
				if(numDyes == 1)
				{
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX, originY, (haveDyes[0] ? 201 : 178), 218, 22, 22, 256, 256);
				}
				else
				{
					//First slot
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX, originY, 178, (haveDyes[0] ? 195 : 172), 20, 22, 256, 256);
					//Middle slots
					for(int s = 1; s < numDyes - 1; s++)
					{
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 2 + 18 * s, originY, 199, (haveDyes[s] ? 195 : 172), 18, 22, 256, 256);
					}
					//Last slot
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 2 + 18 * (numDyes - 1), originY, 218, (haveDyes[numDyes - 1] ? 195 : 172), 20, 22, 256, 256);
				}
				
				for(int s = 0; s < numDyes; s++)
				{
					extractor.item(hoveringOver.dyesNeeded[s], originX + 3 + s * 18, originY + 3);
					extractor.itemDecorations(font, hoveringOver.dyesNeeded[s], originX + 3 + s * 18, originY + 3);
				}
			}
		}
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY)
	{
		this.mouseX = (int)mouseX;
		this.mouseY = (int)mouseY;
		
		hoveringOver = null;
		
		ItemStack gunStack = menu.getSlot(0).getItem();
		if(gunStack != null && gunStack.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)gunStack.getItem()).GetType();
			int numPaintjobs = gunType.paintjobs.size();
			int numRows = numPaintjobs / 2 + 1;
			
			for(int j = 0; j < numRows; j++)
			{
				for(int i = 0; i < 2; i++)
				{
					if(2 * j + i >= numPaintjobs)
						continue;
					
					Paintjob paintjob = gunType.paintjobs.get(2 * j + i);
					int slotX = leftPos + 131 + i * 18;
					int slotY = topPos + 82 + j * 18;
					if(mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18)
						hoveringOver = paintjob;
				}
			}
		}
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		if(event.button() != 0)
			return true;
		if(hoveringOver == null)
			return true;
		
		FlansMod.getPacketHandler().sendToServer(new PacketGunPaint(hoveringOver.ID));
		menu.clickPaintjob(hoveringOver.ID);
		return true;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
