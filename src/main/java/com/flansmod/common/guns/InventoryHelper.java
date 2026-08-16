package com.flansmod.common.guns;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.util.FlansModUtil;

/**
 * Adds access to the Inventory stack combination methods for arbitrary inventories
 */
public class InventoryHelper
{
	public static boolean add(Container inventory, ItemStack stack, boolean creative)
	{
		if(stack == null || stack.isEmpty())
			return false;
		else if(stack.getCount() == 0)
			return false;
		else
		{
			try
			{
				int i;
				
				if(stack.getDamageValue() > 0)
				{
					i = getFirstEmptyStack(inventory);
					
					if(i >= 0)
					{
						ItemStack stackToAdd = stack.copy();
						inventory.setItem(i, stackToAdd);
						stack.setCount(0);
						return true;
					}
					else if(creative)
					{
						stack.setCount(0);
						return true;
					}
					return false;
				}
				else
				{
					do
					{
						i = stack.getCount();
						stack.setCount(storePartialItemStack(inventory, stack));
					}
					while(stack.getCount() > 0 && stack.getCount() < i);
					
					if(stack.getCount() == i && creative)
					{
						stack.setCount(0);
						return true;
					}
					else
					{
						return stack.getCount() < i;
					}
				}
			}
			catch(Throwable throwable)
			{
				FlansMod.log.error("Failed to add item stack to inventory.", throwable);
				return false;
			}
		}
	}
	
	public static int storeItemStack(Container inventory, ItemStack stack)
	{
		for(int i = 0; i < inventory.getContainerSize(); ++i)
		{
			ItemStack oldStack = inventory.getItem(i);
			if(oldStack != null && !oldStack.isEmpty() && oldStack.getItem() == stack.getItem() && oldStack.isStackable() &&
					oldStack.getCount() < oldStack.getMaxStackSize() && oldStack.getCount() < inventory.getMaxStackSize() &&
					oldStack.getDamageValue() == stack.getDamageValue() && ItemStack.isSameItemSameComponents(oldStack, stack))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public static int storePartialItemStack(Container inventory, ItemStack stack)
	{
		Item item = stack.getItem();
		int j = stack.getCount();
		int k;
		
		//If the item doesn't stack, just find an empty slot for it
		if(stack.getMaxStackSize() == 1)
		{
			k = getFirstEmptyStack(inventory);
			//If it is impossible, return
			if(k < 0)
			{
				return j;
			}
			else
			{
				if(inventory.getItem(k) == null || inventory.getItem(k).isEmpty())
				{
					inventory.setItem(k, stack.copy());
				}
				return 0;
			}
		}
		else
		{
			k = storeItemStack(inventory, stack);
			if(k < 0)
			{
				k = getFirstEmptyStack(inventory);
			}
			
			if(k < 0)
			{
				return j;
			}
			else
			{
				ItemStack oldStack = inventory.getItem(k);
				
				if(oldStack == null || oldStack.isEmpty())
				{
					oldStack = new ItemStack(item, 0);
					if(GunUtil.hasTag(stack))
						GunUtil.setTag(oldStack, GunUtil.getTag(stack).copy());
					inventory.setItem(k, oldStack);
				}
				
				int l = j;
				
				if(j > oldStack.getMaxStackSize() - oldStack.getCount())
				{
					l = oldStack.getMaxStackSize() - oldStack.getCount();
				}
				
				if(l > inventory.getMaxStackSize() - oldStack.getCount())
				{
					l = inventory.getMaxStackSize() - oldStack.getCount();
				}
				
				if(l == 0)
				{
					return j;
				}
				else
				{
					j -= l;
					oldStack.setCount(oldStack.getCount() + l);
					return j;
				}
			}
		}
	}
	
	/**
	 * Method from Inventory
	 */
	public static int getFirstEmptyStack(Container inventory)
	{
		for(int i = 0; i < inventory.getContainerSize(); ++i)
			if(inventory.getItem(i) == null || inventory.getItem(i).isEmpty())
				return i;
		
		return -1;
	}
	
}
