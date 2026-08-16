package com.flansmod.common.guns.boxes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerGunBox extends AbstractContainerMenu
{
	public Inventory inventory;
	public GunBoxType type;
	
	public ContainerGunBox(Inventory inventoryplayer)
	{
		this(0, inventoryplayer, null);
	}
	
	public ContainerGunBox(int id, Inventory inventoryplayer)
	{
		this(id, inventoryplayer, null);
	}
	
	public ContainerGunBox(int id, Inventory inventoryplayer, GunBoxType type)
	{
		super(null, id);
		inventory = inventoryplayer;
		this.type = type;
		
		//Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(inventoryplayer, col + row * 9 + 9, 48 + col * 18, 177 + row * 18));
			}
		}
		
		//Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(inventoryplayer, col, 48 + col * 18, 235));
		}
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
		return true;
	}

}
