package com.flansmod.common.driveables;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ContainerDriveableMenu extends AbstractContainerMenu
{
	//Fuel AbstractContainerMenu is combined with this because they are so similar
	public ContainerDriveableMenu(MenuType<?> type, int containerId, Inventory inventoryplayer, EntityDriveable planey, boolean fuel)
	{
		super(type, containerId);
		inventory = inventoryplayer;
		world = inventoryplayer.player != null ? inventoryplayer.player.level() : null;
		plane = planey;
		isFuel = fuel;
		
		//Fuel slot
		if(isFuel && plane != null)
		{
			addSlot(new Slot(plane.driveableData, plane.driveableData.getFuelSlot(), 35, 44));
		}
		
		//Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(inventoryplayer, col + row * 9 + 9, 8 + col * 18, 79 + (isFuel ? 0 : 19) + row * 18));
			}
			
		}
		//Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(inventoryplayer, col, 8 + col * 18, 137 + (isFuel ? 0 : 19)));
		}
	}
	
	public EntityDriveable getDriveable()
	{
		return plane;
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
			
			if(slotID != 0)
			{
				if(!moveItemStackTo(slotStack, 0, 1, false))
				{
					return ItemStack.EMPTY.copy();
				}
			}
			else
			{
				if(!moveItemStackTo(slotStack, 1, slots.size(), true))
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
	
	@Override
	public boolean stillValid(Player entityplayer)
	{
		if(plane == null || plane.isRemoved())
			return false;
		return entityplayer.getVehicle() instanceof EntitySeat
				&& ((EntitySeat)entityplayer.getVehicle()).driveable == plane;
	}
	
	public EntityDriveable plane;
	public boolean isFuel;
	public Inventory inventory;
	public Level world;
}
