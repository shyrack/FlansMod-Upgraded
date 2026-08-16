package com.flansmod.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CraftingInstance
{
	//Input fields
	public Container inventory;
	public List<ItemStack> requiredStacks;
	public List<ItemStack> outputStacks;
	
	//Output fields
	public boolean craftingSuccessful;
	
	/**
	 * The second AbstractContainerMenu is an empty one to copy into
	 */
	public CraftingInstance(Container i, List<ItemStack> in, List<ItemStack> out)
	{
		inventory = i;
		requiredStacks = in;
		outputStacks = out;
	}
	
	public CraftingInstance(Container i, ArrayList<ItemStack> in, ItemStack out)
	{
		this(i, in, Arrays.asList(out));
	}
	
	public boolean canCraft()
	{
		craftingSuccessful = true;
		for(ItemStack check : requiredStacks)
		{
			int numMatchingStuff = 0;
			for(int j = 0; j < inventory.getContainerSize(); j++)
			{
				ItemStack stack = inventory.getItem(j);
				if(stack != null && !stack.isEmpty() && stack.getItem() == check.getItem() && stack.getDamageValue() == check.getDamageValue())
				{
					numMatchingStuff += stack.getCount();
				}
			}
			if(numMatchingStuff < check.getCount())
			{
				craftingSuccessful = false;
			}
		}
		return craftingSuccessful;
	}
	
	public void craft(Player player)
	{
		if(!craftingSuccessful)
			return;
		
		for(ItemStack remove : requiredStacks)
		{
			int amountLeft = remove.getCount();
			for(int j = 0; j < inventory.getContainerSize(); j++)
			{
				ItemStack stack = inventory.getItem(j);
				if(amountLeft > 0 && stack != null && !stack.isEmpty() && stack.getItem() == remove.getItem() && stack.getDamageValue() == remove.getDamageValue())
				{
					amountLeft -= inventory.removeItem(j, amountLeft).getCount();
				}
			}
		}
		
		for(ItemStack stack : outputStacks)
			if(!player.getInventory().add(stack))
				player.drop(stack, false);
	}
}
