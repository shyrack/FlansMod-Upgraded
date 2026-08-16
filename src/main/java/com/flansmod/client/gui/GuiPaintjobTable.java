package com.flansmod.client.gui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.network.PacketGunPaint;
import com.flansmod.common.paintjob.ContainerPaintjobTable;
import com.flansmod.common.paintjob.IPaintableItem;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.paintjob.TileEntityPaintjobTable;

public class GuiPaintjobTable extends AbstractContainerScreen<ContainerPaintjobTable>
{
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/paintjobtable.png");
	private static final Identifier dynamicTextureLocation = Identifier.fromNamespaceAndPath("flansmod", "custompaintjob");
	
	private static final int paletteSizeX = 18;
	private static final int paletteSizeY = 4;
	
	private static final float componentBarLength = 68.0f;
	
	private Paintjob hoveringOver = null;
	private int mouseX, mouseY;
	private Inventory inventory;
	
	private boolean inCustomMode;
	private float customModeTransitionTimer = 0.0f;
	private float transitionSpeed = 0.9f;
	private int prevMainPageX;
	
	private static int[][] paletteColours = new int[paletteSizeX][paletteSizeY];
	private static int[] baseColours = new int[]{0x000000, 0xffffff, 0xff0000, 0xff5500, 0xffaa00, 0xffff00, 0xaaff00, 0x55ff00, 0x00ff00, 0x00ff55, 0x00ffaa, 0x00ffff, 0x00aaff, 0x0055ff, 0x0000ff, 0x5500ff, 0xaa00ff, 0xff00ff};
	private static int currentColour;
	
	private static int flatTextureWindowX = 300, flatTextureWindowY = 100;
	private static boolean movingFlatTextureWindow = false;
	
	private static DynamicTexture dynamicTexture;
	private static int dynamicTextureX, dynamicTextureY;
	
	private boolean painting = false;
	
	static
	{
		ResetPalette();
	}
	
	private static void ResetPalette()
	{
		for(int x = 0; x < paletteSizeX; x++)
		{
			for(int y = 0; y < paletteSizeY; y++)
			{
				int red = (baseColours[x] >> 16) & 0xff;
				int green = (baseColours[x] >> 8) & 0xff;
				int blue = (baseColours[x] >> 0) & 0xff;

				if(x == 0)
				{
					red = green = blue = 0xff * y / 7;
				}
				else if(x == 1)
				{
					red = green = blue = 0xff * (y + 4) / 7;
				}
				else
				{
					if(y == 3)
					{
						red /= 2;
						green /= 2;
						blue /= 2;
					}
					if(y == 1)
					{
						red = 0xff - (0xff - red) / 2;
						green = 0xff - (0xff - green) / 2;
						blue = 0xff - (0xff - blue) / 2;
					}
					if(y == 0)
					{
						red = 0xff - (0xff - red) / 4;
						green = 0xff - (0xff - green) / 4;
						blue = 0xff - (0xff - blue) / 4;
					}
				}

				paletteColours[x][y] = (red << 16) + (green << 8) + blue;
			}
		}
	}
	
	public GuiPaintjobTable(Inventory inv, Level w, TileEntityPaintjobTable te)
	{
		super(new ContainerPaintjobTable(inv, w, te), inv, Component.literal(""), 224, 264);
		inventory = inv;
	}
	
	@Override
	public void containerTick()
	{
		super.containerTick();
		
		if(inCustomMode)
		{
			customModeTransitionTimer = 1.0f - ((1.0f - customModeTransitionTimer) * transitionSpeed);
		}
		else
		{
			customModeTransitionTimer *= transitionSpeed;
		}
		
		int xPos = GetMainPageX();
		int dPos = xPos - prevMainPageX;
		prevMainPageX = xPos;
	}
	
	private int GetMainPageX()
	{
		return (int)(-500.0f * customModeTransitionTimer);
	}

	private int GetMainPageY()
	{
		return 0;
	}
	
	private int GetCustomPageX()
	{
		return (int)(500.0f * (1.0f - customModeTransitionTimer));
	}

	private int GetCustomPageY()
	{
		return 0;
	}
	
	private int GetFlatTextureWindowX()
	{
		return GetCustomPageX() + flatTextureWindowX;
	}

	private int GetFlatTextureWindowY()
	{
		return GetCustomPageY() + flatTextureWindowY;
	}
	
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY)
	{
		// EntityRenderer main screen
		if(customModeTransitionTimer <= 0.999f)
		{
			extractor.text(font, "Inventory", GetMainPageX() + 8, GetMainPageY() + (imageHeight - 94) + 2, 0x404040);
			extractor.text(font, "Paintjob Table", GetMainPageX() + 8, GetMainPageY() + 6, 0x404040);
		}

		// EntityRenderer custom screen
		if(customModeTransitionTimer >= 0.001f)
		{
			int xOrigin = ((width - imageWidth) / 2) + GetCustomPageX() - 32;
			int yOrigin = ((height - imageHeight) / 2) + GetCustomPageY();

			extractor.text(font, "Confirm", xOrigin - 7, yOrigin + 169, 0x000000);
			extractor.text(font, "Cancel", xOrigin - 6, yOrigin + 186, 0x000000);
			extractor.text(font, "Inventory", xOrigin - 12, yOrigin + 203, 0x000000);
		}
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		super.extractBackground(extractor, mouseX, mouseY, partialTick);

		int textureX = 512;
		int textureY = 256;

		// EntityRenderer main screen
		if(customModeTransitionTimer <= 0.999f)
		{
			int xOrigin = ((width - imageWidth) / 2) + GetMainPageX();
			int yOrigin = ((height - imageHeight) / 2) + GetMainPageY();

			// Gun render box
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin, yOrigin, 0F, 0F, imageWidth, 114, textureX, textureY);
			// Inventory box
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin, yOrigin + 122, 0F, 114F, imageWidth, 142, textureX, textureY);

			ItemStack gunStack = menu.getSlot(0).getItem();
			if(gunStack != null && gunStack.getItem() instanceof IPaintableItem)
			{
				PaintableType gunType = ((IPaintableItem)gunStack.getItem()).GetPaintableType();

				int numPaintjobs = gunType.paintjobs.size();
				int numRows = numPaintjobs / 9 + 1;

				for(int y = 0; y < numRows; y++)
				{
					for(int x = 0; x < 9; x++)
					{
						// Only render up to the number of paintjobs in the row
						if(9 * y + x >= numPaintjobs)
							continue;

						Paintjob paintjob = gunType.paintjobs.get(9 * y + x);
						ItemStack stack = gunStack.copy();
						stack.setDamageValue(paintjob.ID);
						extractor.item(stack, xOrigin + 8 + x * 18, yOrigin + 130 + y * 18);
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
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX, originY, (haveDyes[0] ? 379 : 356), 0, 22, 22, textureX, textureY);
					}
					else
					{
						//First slot
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX, originY, 256, (haveDyes[0] ? 23 : 0), 20, 22, textureX, textureY);
						//Middle slots
						for(int s = 1; s < numDyes - 1; s++)
						{
							extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 2 + 18 * s, originY, 277, (haveDyes[s] ? 23 : 0), 18, 22, textureX, textureY);
						}
						//Last slot
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture, originX + 2 + 18 * (numDyes - 1), originY, 296, (haveDyes[numDyes - 1] ? 23 : 0), 20, 22, textureX, textureY);
					}

					for(int s = 0; s < numDyes; s++)
					{
						extractor.item(hoveringOver.dyesNeeded[s], originX + 3 + s * 18, originY + 3);
						extractor.itemDecorations(font, hoveringOver.dyesNeeded[s], originX + 3 + s * 18, originY + 3);
					}
				}
			}

		}


		// EntityRenderer custom paintjob screen
		if(customModeTransitionTimer >= 0.001f)
		{
			int xOrigin = ((width - imageWidth) / 2) + GetCustomPageX() - 32;
			int yOrigin = ((height - imageHeight) / 2) + GetCustomPageY();

			// Palette
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin, yOrigin + 200, 224, 206, 288, 50, textureX, textureY);

			for(int x = 0; x < paletteSizeX; x++)
			{
				for(int y = 0; y < paletteSizeY; y++)
				{
					int colour = paletteColours[x][y];
					extractor.fill(xOrigin + 8 + 9 * x, yOrigin + 200 + 8 + 9 * y, xOrigin + 15 + 9 * x, yOrigin + 207 + 9 * y, 0xff000000 | colour);
				}
			}

			extractor.fill(xOrigin + 172, yOrigin + 208, xOrigin + 206, yOrigin + 242, 0xff000000 | currentColour);


			// Slider bars
			int red = (currentColour >> 16) & 0xff;
			int green = (currentColour >> 8) & 0xff;
			int blue = (currentColour >> 0) & 0xff;

			for(int n = 0; n < componentBarLength; n++)
			{
				int barRed = (int)(0xff * n / componentBarLength);
				extractor.fill(xOrigin + 212 + n, yOrigin + 208, xOrigin + 213 + n, yOrigin + 218, 0xff000000 | (barRed << 16) | (green << 8) | blue);
			}
			for(int n = 0; n < componentBarLength; n++)
			{
				int barGreen = (int)(0xff * n / componentBarLength);
				extractor.fill(xOrigin + 212 + n, yOrigin + 220, xOrigin + 213 + n, yOrigin + 230, 0xff000000 | (red << 16) | (barGreen << 8) | blue);
			}
			for(int n = 0; n < componentBarLength; n++)
			{
				int barBlue = (int)(0xff * n / componentBarLength);
				extractor.fill(xOrigin + 212 + n, yOrigin + 232, xOrigin + 213 + n, yOrigin + 242, 0xff000000 | (red << 16) | (green << 8) | barBlue);
			}

			// Sliders
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 212 + (int)(red * componentBarLength / 0xff), yOrigin + 207, 317, 21, 3, 12, textureX, textureY);
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 212 + (int)(green * componentBarLength / 0xff), yOrigin + 219, 317, 21, 3, 12, textureX, textureY);
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 212 + (int)(blue * componentBarLength / 0xff), yOrigin + 231, 317, 21, 3, 12, textureX, textureY);

			for(int n = 0; n < 3; n++)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xOrigin + 290, yOrigin + 200 + 17 * n, 401, 0, 78, 16, textureX, textureY);
			}


			int xFlatOrigin = GetFlatTextureWindowX();
			int yFlatOrigin = GetFlatTextureWindowY();

			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xFlatOrigin, yFlatOrigin, 242, 54, 64 + 7, 152, textureX, textureY);
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, xFlatOrigin + 64 + 7, yFlatOrigin, 242 + 270 - 64 - 7, 54, 64 + 7, 152, textureX, textureY);

			if(dynamicTexture != null)
			{
				extractor.blit(RenderPipelines.GUI_TEXTURED, dynamicTextureLocation, xFlatOrigin + 7, yFlatOrigin + 17, 0F, 0F, 128, 128, dynamicTextureX, dynamicTextureY);
			}
		}
	}
	
	public static void copyImageToTexture()
	{
		dynamicTexture.upload();
	}
	
	private void SetCustomMode(boolean active)
	{
		if(active)
		{
			if(dynamicTexture == null)
			{
				copyTextureFromGunToCustomTexture();
			}
		}
		else
		{
			
		}
		inCustomMode = active;
	}
	
	private void copyTextureFromGunToCustomTexture()
	{
		ItemStack gunStack = menu.getSlot(0).getItem();
		if(gunStack != null && gunStack.getItem() instanceof IPaintableItem)
		{
			PaintableType paintableType = ((IPaintableItem)gunStack.getItem()).GetPaintableType();

			Paintjob paintjob = paintableType.getPaintjob(gunStack.getDamageValue());

			try
			{
				String imageLocation = "Flan/" + paintableType.contentPack + "/assets/flansmod/skins/" + paintjob.textureName + ".png";
				BufferedImage bufferedImage = ImageIO.read(new File(imageLocation));
				NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
				for(int x = 0; x < bufferedImage.getWidth(); x++)
				{
					for(int y = 0; y < bufferedImage.getHeight(); y++)
					{
						nativeImage.setPixel(x, y, bufferedImage.getRGB(x, y));
					}
				}
				dynamicTexture = new DynamicTexture(() -> "customPaintjob", nativeImage);
				dynamicTextureX = bufferedImage.getWidth();
				dynamicTextureY = bufferedImage.getHeight();
				Minecraft.getInstance().getTextureManager().register(dynamicTextureLocation, dynamicTexture);
			}
			catch(IOException e)
			{
				FlansMod.log.error("Failed to load paintjob texture", e);
			}

			copyImageToTexture();
		}
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY)
	{
		this.mouseX = (int)mouseX;
		this.mouseY = (int)mouseY;

		int mouseXInGUI = (int)mouseX - leftPos;
		int mouseYInGUI = (int)mouseY - topPos;

		if(inCustomMode)
		{
			if(painting)
			{
				int flatTexOriginX = GetFlatTextureWindowX();
				int flatTexOriginY = GetFlatTextureWindowY();

				int pixelX = mouseXInGUI + 64 - (flatTexOriginX + 7) - 4;
				int pixelY = mouseYInGUI - (flatTexOriginY + 17) + 5;

				if(pixelX >= 0 && pixelX < 128 && pixelY >= 0 && pixelY < 128 && dynamicTexture != null)
				{
					for(int i = -2; i < 2; i++)
					{
						for(int j = -2; j < 2; j++)
						{
							if((i == -2 || i == 2) && (j == -2 || j == 2))
							{
								continue;
							}
							int px = Math.min(Math.max(0, pixelX + i), dynamicTextureX - 1);
							int py = Math.min(Math.max(0, pixelY + j), dynamicTextureY - 1);

							dynamicTexture.getPixels().setPixel(px, py, 0xff000000 + currentColour);
							copyImageToTexture();
						}
					}
				}
			}

			if(movingFlatTextureWindow)
			{
				int flatTexOriginX = GetFlatTextureWindowX();
				int flatTexOriginY = GetFlatTextureWindowY();
				if(mouseXInGUI >= flatTexOriginX - 64 + 7 && mouseXInGUI <= flatTexOriginX + 64 + 14 && mouseYInGUI >= flatTexOriginY - 4 && mouseYInGUI <= flatTexOriginY + 6)
				{
					flatTextureWindowX = (int)mouseX - leftPos;
					flatTextureWindowY = (int)mouseY - topPos;
				}
			}
		}
		else
		{
			hoveringOver = null;

			ItemStack gunStack = menu.getSlot(0).getItem();
			if(gunStack != null && gunStack.getItem() instanceof IPaintableItem)
			{
				PaintableType paintableType = ((IPaintableItem)gunStack.getItem()).GetPaintableType();
				int numPaintjobs = paintableType.paintjobs.size();
				int numRows = numPaintjobs / 9 + 1;

				for(int j = 0; j < numRows; j++)
				{
					for(int i = 0; i < 9; i++)
					{
						if(9 * j + i >= numPaintjobs)
							continue;

						Paintjob paintjob = paintableType.paintjobs.get(9 * j + i);
						int slotX = leftPos + GetMainPageX() + 7 + i * 18;
						int slotY = topPos + GetMainPageY() + 129 + j * 18;
						if(mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18)
							hoveringOver = paintjob;
					}
				}
			}
		}
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int x = (int)event.x();
		int y = (int)event.y();
		int button = event.button();

		if(button == 2)
		{
			SetCustomMode(!inCustomMode);
		}
		
		int mouseXInGUI = x - leftPos;
		int mouseYInGUI = y - topPos;
		
		if(inCustomMode)
		{
			int xOrigin = GetCustomPageX() - 32;
			int yOrigin = GetCustomPageY();

			for(int px = 0; px < paletteSizeX; px++)
			{
				for(int py = 0; py < paletteSizeY; py++)
				{
					if(mouseXInGUI >= xOrigin + 8 + 9 * px && mouseXInGUI < xOrigin + 15 + 9 * px && mouseYInGUI >= yOrigin + 208 + 9 * py && mouseYInGUI < yOrigin + 215 + 9 * py)
					{
						switch(button)
						{
							case 0: // Left click. Pick colour
							{
								currentColour = paletteColours[px][py];
								break;
							}
							case 1: // Right click. Set colour from custom
							{
								paletteColours[px][py] = currentColour;
								break;
							}
						}
					}
				}
			}

			if(button == 0)
			{
				if(mouseXInGUI >= xOrigin + 212 && mouseXInGUI < xOrigin + 212 + componentBarLength && mouseYInGUI >= yOrigin + 208 && mouseYInGUI < yOrigin + 218)
				{
					int red = (int)(((mouseXInGUI - (xOrigin + 212)) * 0xff) / componentBarLength);
					currentColour &= 0x00ffff; // Clear red component
					currentColour |= (red << 16);
				}
				if(mouseXInGUI >= xOrigin + 212 && mouseXInGUI < xOrigin + 212 + componentBarLength && mouseYInGUI >= yOrigin + 220 && mouseYInGUI < yOrigin + 230)
				{
					int green = (int)(((mouseXInGUI - (xOrigin + 212)) * 0xff) / componentBarLength);
					currentColour &= 0xff00ff; // Clear green component
					currentColour |= (green << 8);
				}
				if(mouseXInGUI >= xOrigin + 212 && mouseXInGUI < xOrigin + 212 + componentBarLength && mouseYInGUI >= yOrigin + 232 && mouseYInGUI < yOrigin + 242)
				{
					int blue = (int)(((mouseXInGUI - (xOrigin + 212)) * 0xff) / componentBarLength);
					currentColour &= 0xffff00; // Clear blue component
					currentColour |= (blue << 0);
				}
			}

			int flatTexOriginX = GetFlatTextureWindowX();
			int flatTexOriginY = GetFlatTextureWindowY();

			if(button == 0)
			{
				int pixelX = mouseXInGUI + 64 - (flatTexOriginX + 7) - 4;
				int pixelY = mouseYInGUI - (flatTexOriginY + 17) + 5;

				if(pixelX >= 0 && pixelX < 128 && pixelY >= 0 && pixelY < 128)
				{
					painting = true;
				}
			}
			
			if(mouseXInGUI >= flatTexOriginX - 64 + 7 && mouseXInGUI <= flatTexOriginX + 64 + 14 && mouseYInGUI >= flatTexOriginY - 4 && mouseYInGUI <= flatTexOriginY + 6)
			{
				if(button == 0)
					movingFlatTextureWindow = true;
			}
		}
		else
		{
			if(button != 0)
				return true;
			if(hoveringOver == null)
				return true;
			
			FlansMod.getPacketHandler().sendToServer(new PacketGunPaint(hoveringOver.ID));
			menu.clickPaintjob(hoveringOver.ID);
		}
		return true;
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent event)
	{
		super.mouseReleased(event);
		painting = false;
		movingFlatTextureWindow = false;
		return true;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
