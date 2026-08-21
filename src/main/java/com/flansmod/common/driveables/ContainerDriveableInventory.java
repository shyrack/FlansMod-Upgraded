package com.flansmod.common.driveables;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ContainerDriveableInventory extends AbstractContainerMenu
{
	public Inventory inventory;
	public Level world;
	public EntityDriveable plane;
	public int numItems;
	public int screen;
	public int maxScroll;
	public int scroll;
	
	public ContainerDriveableInventory(MenuType<?> type, int containerId, Inventory inventoryplayer, EntityDriveable entPlane, int i)
	{
		super(type, containerId);
		inventory = inventoryplayer;
		world = inventoryplayer.player != null ? inventoryplayer.player.level() : null;
		plane = entPlane;
		screen = i;
		//Find the number of items in the inventory
		numItems = 0;
		if(plane != null)
		{
			switch(i)
			{
				case 0:
				{
					numItems = plane.driveableData.numGuns;
					maxScroll = (numItems > 3 ? numItems - 3 : 0);
					break;
				}
				case 1:
				{
					numItems = plane.getDriveableType().numBombSlots;
					maxScroll = (((numItems + 7) / 8) > 3 ? ((numItems + 7) / 8) - 3 : 0);
					break;
				}
				case 2:
				{
					numItems = plane.getDriveableType().numCargoSlots;
					maxScroll = (((numItems + 7) / 8) > 3 ? ((numItems + 7) / 8) - 3 : 0);
					break;
				}
				case 3:
				{
					numItems = plane.getDriveableType().numMissileSlots;
					maxScroll = (((numItems + 7) / 8) > 3 ? ((numItems + 7) / 8) - 3 : 0);
					break;
				}
			}
		}
		
		//Add screen specific slots
		if(plane != null)
		{
		switch(screen)
		{
			case 0: //Guns
			{
				int slotsDone = 0;
				for(int j = 0; j < numItems; j++)
				{
					int yPos = -1000;
					if(slotsDone < 3 + scroll && slotsDone >= scroll)
						yPos = 25 + 19 * slotsDone;
					addSlot(new Slot(plane.driveableData, j, 29, yPos));
					slotsDone++;
				}
				break;
			}
			case 1: //Bombs
			case 2: //Cargo
			case 3: //Missiles
			{
				int startSlot = plane.driveableData.getBombInventoryStart();
				if(screen == 2)
					startSlot = plane.driveableData.getCargoInventoryStart();
				if(screen == 3)
					startSlot = plane.driveableData.getMissileInventoryStart();
				int m = ((numItems + 7) / 8);
				for(int row = 0; row < m; row++)
				{
					int yPos = -1000;
					if(row < 3 + scroll && row >= scroll)
						yPos = 25 + 19 * (row - scroll);
					for(int col = 0; col < ((row + scroll + 1) * 8 <= numItems ? 8 : numItems % 8); col++)
					{
						addSlot(new Slot(plane.driveableData, startSlot + row * 8 + col, 10 + 18 * col, yPos));
					}
				}
				break;
			}
		}
		}
		
		//Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(inventoryplayer, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
			}
			
		}
		//Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(inventoryplayer, col, 8 + col * 18, 156));
		}
	}
	
	public EntityDriveable getDriveable()
	{
		return plane;
	}
	
	public void updateScroll(int scrololol)
	{
		scroll = scrololol;
	}
	
	@Override
	public boolean stillValid(Player entityplayer)
	{
		if(plane == null || plane.isRemoved())
			return false;
		return entityplayer.getVehicle() instanceof EntitySeat
				&& ((EntitySeat)entityplayer.getVehicle()).driveable == plane;
	}
	
	@Override
	public ItemStack quickMoveStack(Player player, int slotID)
	{
		ItemStack stack = ItemStack.EMPTY.copy();
		Slot currentSlot = slots.get(slotID);
		
		if(currentSlot != null && currentSlot.hasItem())
		{
			ItemStack slotStack = currentSlot.getItem();
			stack = slotStack.copy();
			
			if(slotID >= numItems)
			{
				if(!moveItemStackTo(slotStack, 0, numItems, false))
				{
					return ItemStack.EMPTY.copy();
				}
			}
			else
			{
				if(!moveItemStackTo(slotStack, numItems, slots.size(), true))
				{
					return ItemStack.EMPTY.copy();
				}
			}
			
			if(slotStack.getCount() == 0)
			{
				currentSlot.set(ItemStack.EMPTY.copy());
			}
			else
			{
				currentSlot.setChanged();
			}
			
			if(slotStack.getCount() == stack.getCount())
			{
				return ItemStack.EMPTY.copy();
			}
			
			currentSlot.onTake(player, slotStack);
		}
		
		return stack;
	}
}
