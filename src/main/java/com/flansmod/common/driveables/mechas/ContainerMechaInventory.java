package com.flansmod.common.driveables.mechas;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.common.driveables.EntitySeat;

public class ContainerMechaInventory extends AbstractContainerMenu
{
	public Inventory inventory;
	public Level world;
	public EntityMecha mecha;
	public int numItems;
	public int maxScroll;
	public int scroll;
	
	public ContainerMechaInventory(MenuType<?> type, int containerId, Inventory inv, EntityMecha em)
	{
		super(type, containerId);
		inventory = inv;
		world = inv.player != null ? inv.player.level() : null;
		mecha = em;
		numItems = 0;
		if(mecha != null)
			numItems = mecha.getDriveableType().numCargoSlots;
		int numRows = ((numItems + 7) / 8);
		maxScroll = (numRows > 3 ? numRows - 3 : 0);
		
		if(mecha != null)
		{
			int startSlot = mecha.driveableData.getCargoInventoryStart();
			
			for(int row = 0; row < numRows; row++)
			{
				int yPos = -1000;
				if(row < 3 + scroll && row >= scroll)
					yPos = 25 + 19 * (row - scroll);
				for(int col = 0; col < ((row + scroll + 1) * 8 <= numItems ? 8 : numItems % 8); col++)
				{
					addSlot(new Slot(mecha.driveableData, startSlot + row * 8 + col, 186 + 18 * col, yPos));
				}
			}
			
			//Equipment Slots
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.legs, 84, 128));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.hips, 60, 128));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.leftArm, 36, 80));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.leftTool, 36, 56));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.leftShoulder, 60, 32));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.head, 84, 32));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.feet, 108, 128));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.rightArm, 132, 80));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.rightTool, 132, 56));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.rightShoulder, 108, 32));
			
			//Upgrade Slots
			
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.u1, 10, 32));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.u2, 10, 56));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.u3, 10, 80));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.u4, 10, 104));
			addSlot(new SlotMecha(mecha.inventory, EnumMechaSlotType.u5, 10, 128));
		}
		
		//Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(inventory, col + row * 9 + 9, 182 + col * 18, 98 + row * 18));
			}
			
		}
		//Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(inventory, col, 182 + col * 18, 156));
		}
	}
	
	public EntityMecha getMecha()
	{
		return mecha;
	}
	
	@Override
	public void removed(Player par1EntityPlayer)
	{
		super.removed(par1EntityPlayer);
		if(mecha != null)
			mecha.couldNotFindFuel = false;
	}
	
	public void updateScroll(int scrololol)
	{
		scroll = scrololol;
	}
	
	@Override
	public boolean stillValid(Player entityplayer)
	{
		if(mecha == null || mecha.isRemoved())
			return false;
		return entityplayer.getVehicle() instanceof EntitySeat
				&& ((EntitySeat)entityplayer.getVehicle()).driveable == mecha;
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
			
			///if(stack.getItem() instanceof ItemMechaAddon)
			// {
			//((ItemMechaAddon)stack.getItem()).type;
			//}
			
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
