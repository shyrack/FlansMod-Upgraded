package com.flansmod.common.guns;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.guns.GunUtil;

public class InventoryGunModTable implements Container
{
	public static final int CONTAINER_SIZE = 13;
	
	private final ItemStack[] stacks = new ItemStack[CONTAINER_SIZE];
	
	{
		for(int i = 0; i < CONTAINER_SIZE; i++)
			stacks[i] = ItemStack.EMPTY;
	}
	
	public ItemStack lastGunStack;
	public GunType gunType;
	public int genericScroll = 0;
	/**
	 * Hacky way to change slots within onInventoryChanged without causing a huge stack overflow
	 */
	private boolean busy = false;
	
	@Override
	public int getContainerSize()
	{
		return CONTAINER_SIZE;
	}
	
	@Override
	public ItemStack getItem(int index)
	{
		return stacks[index];
	}
	
	@Override
	public ItemStack removeItem(int index, int count)
	{
		ItemStack stack = stacks[index];
		if(stack == null || stack.isEmpty())
			return ItemStack.EMPTY;
		ItemStack removed = stack.split(count);
		if(stack.isEmpty())
			stacks[index] = ItemStack.EMPTY;
		setChanged();
		return removed;
	}
	
	@Override
	public ItemStack removeItemNoUpdate(int index)
	{
		ItemStack stack = stacks[index];
		stacks[index] = ItemStack.EMPTY;
		return stack;
	}
	
	@Override
	public void setItem(int index, ItemStack stack)
	{
		stacks[index] = stack == null ? ItemStack.EMPTY : stack;
		setChanged();
	}
	
	@Override
	public boolean isEmpty()
	{
		for(ItemStack stack : stacks)
		{
			if(stack != null && !stack.isEmpty())
				return false;
		}
		return true;
	}
	
	@Override
	public void setChanged()
	{
		if(busy)
			return;
		ItemStack gunStack = getItem(0);
		if(gunStack == null || gunStack.isEmpty() || !(gunStack.getItem() instanceof ItemGun))
			return;
		
		gunType = ((ItemGun)gunStack.getItem()).GetType();
		
		//If we changed the gun (i.e. a new gun has been placed in the table)
		if(gunStack != lastGunStack)
		{
			busy = true;
			
			if(!GunUtil.hasTag(gunStack))
				GunUtil.setTag(gunStack, new CompoundTag());
			CompoundTag attachmentTags = GunUtil.getTag(gunStack).getCompoundOrEmpty("attachments");
			if(attachmentTags == null || attachmentTags.isEmpty())
			{
				attachmentTags = new CompoundTag();
				GunUtil.getTag(gunStack).put("attachments", attachmentTags);
			}
			
			setItem(1, GunUtil.stackFromTag(attachmentTags.getCompoundOrEmpty("barrel")));
			setItem(2, GunUtil.stackFromTag(attachmentTags.getCompoundOrEmpty("scope")));
			setItem(3, GunUtil.stackFromTag(attachmentTags.getCompoundOrEmpty("stock")));
			setItem(4, GunUtil.stackFromTag(attachmentTags.getCompoundOrEmpty("grip")));
			genericScroll = 0;
			for(int i = 0; i < Math.min(gunType.numGenericAttachmentSlots, 8); i++)
				setItem(5 + i, GunUtil.stackFromTag(attachmentTags.getCompoundOrEmpty("generic_" + i)));
			busy = false;
		}
		//Else we changed an attachment
		else
		{
			//Create a new NBT tag compound for our gun item
			CompoundTag gunTags = new CompoundTag();
			//Copy the ammo and paintjob from the old stack
			gunTags.put("ammo", GunUtil.getTag(getItem(0)).get("ammo"));
			if(GunUtil.getTag(getItem(0)).contains("Paint"))
				gunTags.put("Paint", GunUtil.getTag(getItem(0)).get("Paint"));
			if(GunUtil.getTag(getItem(0)).contains("LegendaryCrafter"))
				gunTags.put("LegendaryCrafter", GunUtil.getTag(getItem(0)).get("LegendaryCrafter"));
			if(GunUtil.getTag(getItem(0)).contains("display"))
				gunTags.put("display", GunUtil.getTag(getItem(0)).get("display"));
			
			//Add each attachment from the inventory to our gun stack
			CompoundTag attachmentTags = new CompoundTag();
			
			writeAttachmentTags(attachmentTags, getItem(1), "barrel");
			writeAttachmentTags(attachmentTags, getItem(2), "scope");
			writeAttachmentTags(attachmentTags, getItem(3), "stock");
			writeAttachmentTags(attachmentTags, getItem(4), "grip");
			
			//Change all the attachments that we are looking at, but copy in the old ones
			for(int i = 0; i < gunType.numGenericAttachmentSlots; i++)
			{
				if(i >= genericScroll * 4 && i < genericScroll * 4 + 8)
				{
					writeAttachmentTags(attachmentTags, getItem(i - genericScroll * 4 + 5), "generic_" + i);
				}
				else attachmentTags.put("generic_" + i, GunUtil.getTag(getItem(0)).get("generic_" + i));
			}
			
			//Set the tags to be these new tags
			gunTags.put("attachments", attachmentTags);
			GunUtil.setTag(gunStack, gunTags);
		}
		
		lastGunStack = gunStack;
	}
	
	public void writeAttachmentTags(CompoundTag attachmentTags, ItemStack attachmentStack, String attachmentName)
	{
		CompoundTag tags = new CompoundTag();
		if(attachmentStack != null && !attachmentStack.isEmpty())
			GunUtil.stackToTag(tags, attachmentStack);
		attachmentTags.put(attachmentName, tags);
	}
	
	@Override
	public void clearContent()
	{
		for(int i = 0; i < CONTAINER_SIZE; i++)
			stacks[i] = ItemStack.EMPTY;
	}
	
	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack)
	{
		return false;
	}
	
	@Override
	public boolean stillValid(Player player)
	{
		return true;
	}
}
