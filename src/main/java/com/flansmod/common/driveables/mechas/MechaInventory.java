package com.flansmod.common.driveables.mechas;

import java.util.HashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.util.ItemStackUtil;

public class MechaInventory implements Container
{
	public EntityMecha mecha;
	public HashMap<EnumMechaSlotType, ItemStack> stacks;
	
	public MechaInventory(EntityMecha m)
	{
		mecha = m;
		stacks = new HashMap<>();
		for(EnumMechaSlotType type : EnumMechaSlotType.values())
		{
			stacks.put(type, ItemStack.EMPTY.copy());
		}
	}
	
	public MechaInventory(EntityMecha m, CompoundTag tags)
	{
		this(m);
		readFromNBT(tags);
	}
	
	public void readFromNBT(CompoundTag tags)
	{
		if(tags == null)
			return;
		for(EnumMechaSlotType type : EnumMechaSlotType.values())
		{
			stacks.put(type, ItemStackUtil.readItemStack(tags, type.toString()));
		}
	}
	
	public CompoundTag writeToNBT(CompoundTag tags)
	{
		if(tags == null)
			return null;
		for(EnumMechaSlotType type : EnumMechaSlotType.values())
		{
			ItemStackUtil.writeItemStack(tags, type.toString(), stacks.get(type));
		}
		return tags;
	}
	
	@Override
	public int getContainerSize()
	{
		return EnumMechaSlotType.values().length;
	}
	
	@Override
	public ItemStack getItem(int i)
	{
		return stacks.get(EnumMechaSlotType.values()[i]);
	}
	
	public ItemStack getItem(EnumMechaSlotType e)
	{
		return stacks.get(e);
	}
	
	@Override
	public ItemStack removeItem(int i, int j)
	{
		setChanged();
		ItemStack slot = getItem(i);
		if(slot == null || slot.isEmpty())
			return ItemStack.EMPTY.copy();
		
		int numToTake = Math.min(j, slot.getCount());
		ItemStack returnStack = slot.copy();
		returnStack.setCount(numToTake);
		slot.setCount(slot.getCount() - numToTake);
		if(slot.getCount() <= 0)
			slot = ItemStack.EMPTY.copy();
		
		setItem(i, slot);
		
		return returnStack;
	}
	
	@Override
	public ItemStack removeItemNoUpdate(int i)
	{
		ItemStack stack = getItem(i);
		setItem(i, ItemStack.EMPTY.copy());
		return stack;
	}
	
	@Override
	public void setItem(int i, ItemStack itemstack)
	{
		setItem(EnumMechaSlotType.values()[i], itemstack);
	}
	
	public void setItem(EnumMechaSlotType e, ItemStack itemstack)
	{
		setChanged();
		stacks.put(e, itemstack);
	}
	
	@Override
	public int getMaxStackSize()
	{
		return 64;
	}
	
	@Override
	public void setChanged()
	{
		if(mecha != null)
			mecha.couldNotFindFuel = false;
	}
	
	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack)
	{
		if(itemstack == null || itemstack.isEmpty())
			return true;
		Item item = itemstack.getItem();
		switch(EnumMechaSlotType.values()[i])
		{
			case leftTool: case rightTool: return item instanceof ItemGun || item instanceof ItemMechaAddon;
			case leftArm: case rightArm: return item instanceof ItemBullet;
			default: return false;
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return false;
	}
	
	@Override
	public void clearContent()
	{
	}
	
	@Override
	public boolean stillValid(Player player)
	{
		return mecha != null && player.distanceToSqr(mecha) <= 10D * 10D;
	}
}
