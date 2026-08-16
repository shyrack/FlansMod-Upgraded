package com.flansmod.common.paintjob;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import com.flansmod.common.guns.Paintjob;

public class ContainerPaintjobTable extends AbstractContainerMenu
{
	public Inventory playerInv;
	public TileEntityPaintjobTable table;
	public Level world;
	
	public ContainerPaintjobTable(Inventory i, Level w, TileEntityPaintjobTable te)
	{
		this(0, i, te);
		world = w;
	}
	
	public ContainerPaintjobTable(int id, Inventory i, TileEntityPaintjobTable te)
	{
		super(null, id);
		playerInv = i;
		table = te;
		world = te.getLevel();
		
		// Gun slot
		addSlot(new Slot(table, 0, 187, 139));
		// Paint cans slot
		addSlot(new Slot(table, 1, 187, 193));
		
		// Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 184 + row * 18));
			}
			
		}
		// Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(playerInv, col, 8 + col * 18, 242));
		}
	}
	
	@Override
	public void removed(Player player)
	{
		// Save out paintjob?
	}
	
	@Override
	public boolean stillValid(Player entityplayer)
	{
		return true;
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
			
			if(slotID >= 1)
			{
				return ItemStack.EMPTY.copy();
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
	
	public void pressButton(boolean paint, boolean left)
	{
		//Nope.
	}
	
	public void clickPaintjob(int i)
	{
		ItemStack paintableStack = table.getPaintableStack();
		if(paintableStack != null && paintableStack.getItem() instanceof IPaintableItem)
		{
			PaintableType paintableType = ((IPaintableItem)paintableStack.getItem()).GetPaintableType();
			clickPaintjob(paintableType.getPaintjob(i));
		}
	}
	
	public void clickPaintjob(Paintjob paintjob)
	{
		ItemStack paintableStack = table.getPaintableStack();
		if(paintableStack != null && paintableStack.getItem() instanceof IPaintableItem)
		{
			PaintableType paintableType = ((IPaintableItem)paintableStack.getItem()).GetPaintableType();
			
			int numDyes = paintjob.dyesNeeded.length;
			
			if(!playerInv.player.getAbilities().instabuild)
			{
				//Calculate which dyes we have in our inventory
				for(int n = 0; n < numDyes; n++)
				{
					int amountNeeded = paintjob.dyesNeeded[n].getCount();
					for(int s = 0; s < playerInv.getContainerSize(); s++)
					{
						ItemStack stack = playerInv.getItem(s);
						if(stack != null && stack.getItem() == paintjob.dyesNeeded[n].getItem())
						{
							amountNeeded -= stack.getCount();
						}
					}
					//We don't have enough of this dye
					if(amountNeeded > 0)
						return;
				}
				
				for(int n = 0; n < numDyes; n++)
				{
					int amountNeeded = paintjob.dyesNeeded[n].getCount();
					for(int s = 0; s < playerInv.getContainerSize(); s++)
					{
						if(amountNeeded <= 0)
							continue;
						ItemStack stack = playerInv.getItem(s);
						if(stack != null && stack.getItem() == paintjob.dyesNeeded[n].getItem())
						{
							ItemStack consumed = playerInv.removeItem(s, amountNeeded);
							amountNeeded -= consumed.getCount();
						}
					}
				}
			}
			
			//Paint the gun. This line is only reached if the player is in creative or they have had their dyes taken already
			CompoundTag tag = paintableStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			tag.putInt("Paint", paintjob.ID);
			paintableStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}
}
