package com.flansmod.client.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.types.EnumType;

public class GuiDriveableCrafting extends Screen
{
	/**
	 * The background image
	 */
	private static final Identifier texture = Identifier.fromNamespaceAndPath("flansmod", "gui/driveablecrafting.png");
	private static final int CRAFT_BUTTON_ID = 0;
	private static final int BLUEPRINTS_UP_BUTTON_ID = 1;
	private static final int BLUEPRINTS_DOWN_BUTTON_ID = 2;
	private static final int RECIPE_UP_BUTTON_ID = 3;
	private static final int RECIPE_DOWN_BUTTON_ID = 4;

	/**
	 * The inventory of the player using this crafting table
	 */
	private Inventory inventory;
	/**
	 * Gui origin
	 */
	private int guiOriginX, guiOriginY;
	/**
	 * Blueprint scroller, static to save position upon exiting crafting window
	 */
	private static int blueprintsScrollPos = 0;
	/**
	 * Recipe scroller
	 */
	private int recipeScrollPos = 0;
	/**
	 * The blueprint that is currently selected
	 */
	private static int selectedBlueprint = 0;
	/**
	 * Whether or not the currently selected driveable can be crafted
	 */
	private boolean canCraft = false;
	public static final int BLUEPRINT_ROW_COUNT = 4;
	public static final int BLUEPRINT_COLUMN_COUNT = 8;
	public static final int BLUEPRINT_WIDTH = 18;
	public static final int BLUEPRINT_HEIGHT = 18;
	private int blueprintsOriginX;
	private int blueprintsOriginY;
	private int statsOriginX;
	private int statsOriginY;
	private int vehicleCraftingTextX;
	private int vehicleCraftingTextY;
	private int requiresTextX;
	private int requiresTextY;
	private int engineTextX;
	private int engineTextY;
	public static final int GUI_WIDTH = 176;
	public static final int GUI_HEIGHT = 198 + 36;
	public static final int WHITE = Color.white.getRGB();
	private int recipeOriginX;
	private int recipeOriginY;
	private int engineOriginX;
	private int engineOriginY;
	private Button craftButton;
	public static final int RECIPE_ROW_COUNT = 3;
	public static final int RECIPE_COLUMN_COUNT = 4;
	private ArrowButton blueprintsDownButton;
	private ArrowButton recipeDownButton;
	private ArrowButton recipeUpButton;
	private ArrowButton blueprintsUpButton;

	public GuiDriveableCrafting(Inventory playerInventory)
	{
		super(Component.literal(""));
		inventory = playerInventory;
	}

	@Override
	public void init()
	{
		super.init();

		guiOriginX = width / 2 - GUI_WIDTH / 2;
		guiOriginY = height / 2 - GUI_HEIGHT / 2;
		blueprintsOriginX = guiOriginX + 8;
		blueprintsOriginY = guiOriginY + 18;
		statsOriginX = guiOriginX + 82;
		statsOriginY = guiOriginY + 64 + 36;
		vehicleCraftingTextX = guiOriginX + 6;
		vehicleCraftingTextY = guiOriginY + 6;
		requiresTextX = guiOriginX + 6;
		requiresTextY = guiOriginY + 125 + 36;
		engineTextX = guiOriginX + 114;
		engineTextY = guiOriginY + 141 + 36;
		recipeOriginX = guiOriginX + 8;
		recipeOriginY = guiOriginY + 138 + 36;
		engineOriginX = guiOriginX + 152;
		engineOriginY = guiOriginY + 138 + 36;

		craftButton = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> actionPerformed(CRAFT_BUTTON_ID))
				.bounds(guiOriginX + 110, guiOriginY + 162 + 36, 40, 20).build());
		blueprintsUpButton = new ArrowButton(BLUEPRINTS_UP_BUTTON_ID, guiOriginX + 157, guiOriginY + 21, Direction.UP);
		blueprintsDownButton = new ArrowButton(BLUEPRINTS_DOWN_BUTTON_ID, guiOriginX + 157, guiOriginY + 39 + 36, Direction.DOWN);
		recipeUpButton = new ArrowButton(RECIPE_UP_BUTTON_ID, guiOriginX + 83, guiOriginY + 141 + 36, Direction.UP);
		recipeDownButton = new ArrowButton(RECIPE_DOWN_BUTTON_ID, guiOriginX + 83, guiOriginY + 177 + 36, Direction.DOWN);

		updateButtons();
	}

	private void actionPerformed(int buttonId)
	{
		switch(buttonId)
		{
			case CRAFT_BUTTON_ID:
				FlansMod.proxy.craftDriveable(inventory.player, DriveableType.types.get(selectedBlueprint));
				break;
			case BLUEPRINTS_UP_BUTTON_ID:
				if(blueprintsScrollPos > 0)
					blueprintsScrollPos--;
				break;
			case BLUEPRINTS_DOWN_BUTTON_ID:
				if(blueprintsScrollPos * 8 + 16 < DriveableType.types.size())
					blueprintsScrollPos++;
				break;
			case RECIPE_UP_BUTTON_ID:
				if(recipeScrollPos > 0)
					recipeScrollPos--;
				break;
			case RECIPE_DOWN_BUTTON_ID:
				DriveableType selectedType = DriveableType.types.get(selectedBlueprint);
				int totalCells = RECIPE_ROW_COUNT * RECIPE_COLUMN_COUNT;
				if(recipeScrollPos * RECIPE_COLUMN_COUNT + totalCells < selectedType.driveableRecipe.size())
					recipeScrollPos++;
				break;
		}

		updateButtons();
	}


	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick)
	{
		extractMenuBackground(extractor);

		// GUI background
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, guiOriginX, guiOriginY, 0F, 0F, GUI_WIDTH, GUI_HEIGHT, 256, 256);
		extractor.text(font, "Vehicle Crafting", vehicleCraftingTextX, vehicleCraftingTextY, WHITE);

		// Blueprints selector
		List<ItemToRender> itemsToRender = getBlueprintItemsToRender(extractor);

		// Preview
		DriveableType selectedType = DriveableType.types.get(selectedBlueprint);

		// Stats
		drawStats(extractor, selectedType);

		// Engine requirements
		extractor.text(font, "Engine", engineTextX, engineTextY, WHITE);
		extractor.text(font, selectedType.numEngines() + "x", engineTextX - 14, engineTextY, WHITE);

		canCraft = true;

		// Recipe items
		itemsToRender.addAll(getRecipeItemsToRender(extractor, selectedType));

		// Collect up all the engines into neat and tidy stacks so we can find if any of them are big enough
		// and which of those stacks are best
		ItemStack bestEngineStack = getBestEngineStackForType(selectedType);

		// Draw engine slot
		itemsToRender.addAll(getEngineItemToRender(extractor, bestEngineStack));

		craftButton.active = canCraft;

		// Draw the arrows
		drawArrowButton(extractor, blueprintsUpButton);
		drawArrowButton(extractor, blueprintsDownButton);
		drawArrowButton(extractor, recipeUpButton);
		drawArrowButton(extractor, recipeDownButton);

		itemsToRender.forEach(item -> drawSlotInventory(extractor, item.itemStack, item.x, item.y, mouseX, mouseY));
	}

	private void drawArrowButton(GuiGraphicsExtractor extractor, ArrowButton button)
	{
		if(button.visible)
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, button.x, button.y,
					button.enabled ? button.enabledTextureX : button.disabledTextureX, 0, button.WIDTH, button.HEIGHT, 256, 256);
		}
	}

	private List<ItemToRender> getEngineItemToRender(GuiGraphicsExtractor extractor, ItemStack engineStack)
	{
		List<ItemToRender> itemsToRender = new ArrayList<>();
		if(engineStack.isEmpty())
		{
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, engineOriginX, engineOriginY, 195F, 11F, 16, 16, 256, 256);
			canCraft = false;
		}
		else
		{
			itemsToRender.add(new ItemToRender(engineStack, engineOriginX, engineOriginY));
		}
		return itemsToRender;
	}

	private ItemStack getBestEngineStackForType(DriveableType selectedType)
	{
		HashMap<PartType, ItemStack> engines = getPlayersEnginesForType(selectedType);

		//Find the stack of engines that is fastest but which also has enough for this driveable
		float bestEngineSpeed = -1F;
		ItemStack bestEngineStack = ItemStack.EMPTY.copy();
		for(PartType part : engines.keySet())
		{
			//If this engine outperforms the currently selected best one and there are enough of them, swap
			if(part.engineSpeed > bestEngineSpeed && engines.get(part).getCount() >= selectedType.numEngines())
			{
				bestEngineSpeed = part.engineSpeed;
				bestEngineStack = engines.get(part);
			}
		}
		return bestEngineStack;
	}

	private HashMap<PartType, ItemStack> getPlayersEnginesForType(DriveableType selectedType)
	{
		HashMap<PartType, ItemStack> engines = new HashMap<>();

		//Find some suitable engines
		for(ItemStack itemStack : inventory.getNonEquipmentItems())
		{
			if(itemStack.getItem() instanceof ItemPart)
			{
				PartType partType = ((ItemPart)itemStack.getItem()).type;
				//Check its an engine that we can use
				if(partType.category == EnumPartCategory.ENGINE
						&& partType.worksWith.contains(EnumType.getFromObject(selectedType)))
				{
					//If we already have engines of this type, add these ones to the stack
					if(engines.containsKey(partType))
					{
						engines.get(partType).setCount(engines.get(partType).getCount() + itemStack.getCount());
					}
					//Else, make this the first stack
					else engines.put(partType, itemStack);
				}
			}
		}
		return engines;
	}

	private List<ItemToRender> getRecipeItemsToRender(GuiGraphicsExtractor extractor, DriveableType selectedType)
	{
		List<ItemToRender> itemsToRender = new ArrayList<>();
		extractor.text(font, "Requires", requiresTextX, requiresTextY, WHITE);
		for(int row = 0; row < RECIPE_ROW_COUNT; row++)
		{
			for(int column = RECIPE_COLUMN_COUNT - 1; column >= 0; column--)
			{
				int pageStartIndex = recipeScrollPos * RECIPE_COLUMN_COUNT;
				int rowStartInPageIndex = row * RECIPE_COLUMN_COUNT;
				int recipeItemNumber = pageStartIndex + rowStartInPageIndex + column;
				if(recipeItemNumber < selectedType.driveableRecipe.size())
				{
					ItemStack recipeStack = selectedType.driveableRecipe.get(recipeItemNumber);
					int totalAmountFound = 0;
					for(ItemStack itemStack : inventory.getNonEquipmentItems())
					{
						if(itemStack.getItem() == recipeStack.getItem()
								&& itemStack.getDamageValue() == recipeStack.getDamageValue())
						{
							totalAmountFound += itemStack.getCount();
							if(totalAmountFound == recipeStack.getCount())
								break;
						}
					}
					//If we didn't find enough, give the stack a red outline
					if(totalAmountFound < recipeStack.getCount())
					{
						extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
								recipeOriginX + column * BLUEPRINT_WIDTH,
								recipeOriginY + row * BLUEPRINT_HEIGHT,
								195F,
								11F,
								16,
								16, 256, 256);
						canCraft = false;
					}
					//Draw the actual item we want
					itemsToRender.add(new ItemToRender(
							recipeStack,
							recipeOriginX + column * BLUEPRINT_WIDTH,
							recipeOriginY + row * BLUEPRINT_HEIGHT));
				}
			}
		}
		return itemsToRender;
	}

	private void drawStats(GuiGraphicsExtractor extractor, DriveableType selectedType)
	{
		String recipeName = selectedType.name;
		if(recipeName.length() > 16)
			recipeName = recipeName.substring(0, 15) + "...";

		// Driveable stats
		extractor.text(font, recipeName, statsOriginX, statsOriginY, WHITE);
		extractor.text(
				font,
				"Cargo Slots : " + selectedType.numCargoSlots,
				statsOriginX,
				statsOriginY + 10,
				WHITE);
		extractor.text(
				font,
				"Bomb Slots : " + selectedType.numBombSlots,
				statsOriginX,
				statsOriginY + 20,
				WHITE);
		extractor.text(
				font,
				"Passengers : " + selectedType.numPassengers,
				statsOriginX,
				statsOriginY + 30,
				WHITE);
		extractor.text(font, "Guns : " + (selectedType.ammoSlots()), statsOriginX, statsOriginY + 40, WHITE);
	}

	private List<ItemToRender> getBlueprintItemsToRender(GuiGraphicsExtractor extractor)
	{
		List<ItemToRender> itemsToRender = new ArrayList<>();
		for(int row = BLUEPRINT_ROW_COUNT - 1; row >= 0; row--)
		{
			for(int column = 0; column < BLUEPRINT_COLUMN_COUNT; column++)
			{
				int pageStartIndex = blueprintsScrollPos * BLUEPRINT_COLUMN_COUNT;
				int rowStartInPageIndex = row * BLUEPRINT_COLUMN_COUNT;
				int blueprintNumber = pageStartIndex + rowStartInPageIndex + column;

				// Draw outline for selected blueprint
				if(blueprintNumber == selectedBlueprint)
				{
					extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
							blueprintsOriginX + column * BLUEPRINT_WIDTH,
							blueprintsOriginY + row * BLUEPRINT_HEIGHT,
							213F,
							11F,
							BLUEPRINT_WIDTH - 2,
							BLUEPRINT_HEIGHT - 2, 256, 256);
				}

				// Draw blueprint
				if(blueprintNumber < DriveableType.types.size())
				{
					DriveableType type = DriveableType.types.get(blueprintNumber);
					itemsToRender.add(new ItemToRender(
							new ItemStack(type.item),
							blueprintsOriginX + column * BLUEPRINT_WIDTH,
							blueprintsOriginY + row * BLUEPRINT_HEIGHT));
				}
			}
		}
		return itemsToRender;
	}

	/**
	 * Item stack rendering method
	 */
	private void drawSlotInventory(GuiGraphicsExtractor extractor, ItemStack itemstack, int x, int y, int mouseX, int mouseY)
	{
		if(itemstack == null)
			return;
		extractor.item(itemstack, x, y);
		extractor.itemDecorations(font, itemstack, x, y);
		drawTooltip(extractor, itemstack.getHoverName().getString(), x, y, mouseX, mouseY, 16, 16);
	}

	private void drawTooltip(GuiGraphicsExtractor extractor, String text, int x, int y, int mouseX, int mouseY, int iconWidth, int iconHeight)
	{
		if(mouseX >= x && mouseY >= y && mouseX < x + iconWidth && mouseY < y + iconHeight)
		{
			extractor.setTooltipForNextFrame(font, Component.literal(text), mouseX, mouseY);
		}
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
	public boolean mouseClicked(MouseButtonEvent event, boolean bl)
	{
		super.mouseClicked(event, bl);
		int mouseX = (int)event.x();
		int mouseY = (int)event.y();
		int mouseButton = event.button();
		if(mouseButton == 0 || mouseButton == 1)
		{
			//Arrow buttons
			if(isArrowClicked(blueprintsUpButton, mouseX, mouseY))
				actionPerformed(BLUEPRINTS_UP_BUTTON_ID);
			if(isArrowClicked(blueprintsDownButton, mouseX, mouseY))
				actionPerformed(BLUEPRINTS_DOWN_BUTTON_ID);
			if(isArrowClicked(recipeUpButton, mouseX, mouseY))
				actionPerformed(RECIPE_UP_BUTTON_ID);
			if(isArrowClicked(recipeDownButton, mouseX, mouseY))
				actionPerformed(RECIPE_DOWN_BUTTON_ID);

			//Driveable buttons
			for(int row = 0; row < BLUEPRINT_ROW_COUNT; row++)
			{
				for(int column = 0; column < BLUEPRINT_COLUMN_COUNT; column++)
				{
					if(mouseX >= blueprintsOriginX + column * BLUEPRINT_WIDTH
							&& mouseX < blueprintsOriginX + column * BLUEPRINT_WIDTH + BLUEPRINT_WIDTH
							&& mouseY >= blueprintsOriginY + row * BLUEPRINT_HEIGHT
							&& mouseY < blueprintsOriginY + row * BLUEPRINT_HEIGHT + BLUEPRINT_HEIGHT)
					{
						int pageStartIndex = blueprintsScrollPos * BLUEPRINT_COLUMN_COUNT;
						int rowStartInPageIndex = row * BLUEPRINT_COLUMN_COUNT;
						int result = pageStartIndex + rowStartInPageIndex + column;
						if(result < DriveableType.types.size())
						{
							recipeScrollPos = 0;
							selectedBlueprint = result;
							return true;
						}
					}
				}
			}
		}
		return true;
	}

	private boolean isArrowClicked(ArrowButton button, int mouseX, int mouseY)
	{
		return button.visible && button.enabled
				&& mouseX >= button.x && mouseX < button.x + button.WIDTH
				&& mouseY >= button.y && mouseY < button.y + button.HEIGHT;
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	private void updateButtons()
	{
		// Blueprint buttons
		blueprintsUpButton.enabled = blueprintsScrollPos > 0;

		int totalBlueprintsCells = BLUEPRINT_COLUMN_COUNT * BLUEPRINT_ROW_COUNT;
		blueprintsDownButton.enabled =
				blueprintsScrollPos * BLUEPRINT_COLUMN_COUNT + totalBlueprintsCells < DriveableType.types.size() - 1;

		// Recipe buttons
		recipeUpButton.enabled = recipeScrollPos > 0;

		int totalRecipeItems = RECIPE_COLUMN_COUNT * RECIPE_ROW_COUNT;
		DriveableType selectedType = DriveableType.types.get(selectedBlueprint);
		recipeDownButton.enabled =
				recipeScrollPos * RECIPE_COLUMN_COUNT + totalRecipeItems < selectedType.driveableRecipe.size() - 1;
	}

	private static class ArrowButton
	{
		public static final int WIDTH = 10;
		public static final int HEIGHT = 10;
		public final int x;
		public final int y;
		public final int enabledTextureX;
		public final int enabledTextureY = 0;
		public final int disabledTextureX;
		public final int disabledTextureY = 0;
		public final Direction direction;
		public boolean enabled = true;
		public boolean visible = true;

		public ArrowButton(int buttonId, int x, int y, Direction direction)
		{
			this.x = x;
			this.y = y;
			this.direction = direction;

			switch(direction)
			{
				case UP:
					enabledTextureX = 216;
					disabledTextureX = 196;
					break;
				case DOWN:
					enabledTextureX = 226;
					disabledTextureX = 206;
					break;
				default:
					throw new IllegalStateException("Texture location not set for direction");
			}
		}
	}

	private static class ItemToRender
	{
		public ItemStack itemStack;
		public int x;
		public int y;

		public ItemToRender(ItemStack itemStack, int x, int y)
		{
			this.itemStack = itemStack;
			this.x = x;
			this.y = y;
		}
	}

	private enum Direction
	{UP, DOWN}
}
