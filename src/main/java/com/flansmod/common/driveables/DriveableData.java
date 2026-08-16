package com.flansmod.common.driveables;

import java.util.HashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.util.ItemStackUtil;

public class DriveableData implements Container
{
	/**
	 * The name of this driveable's type
	 */
	public String type;
	/**
	 * The sizes of each inventory (guns, bombs / mines, missiles / shells, cargo)
	 */
	public int numGuns, numBombs, numMissiles, numCargo;
	/**
	 * The inventory stacks
	 */
	public ItemStack[] ammo, bombs, missiles, cargo;
	/**
	 * The engine in this driveable
	 */
	public PartType engine;
	/**
	 * The stack in the fuel slot
	 */
	public ItemStack fuel;
	/**
	 * The amount of fuel in the tank
	 */
	public float fuelInTank;
	/**
	 * Each driveable part has a small class that holds its current status
	 */
	public HashMap<EnumDriveablePart, DriveablePart> parts;
	/**
	 * Paintjob index
	 */
	public int paintjobID;
	
	public DriveableData()
	{
		parts = new HashMap<>();
		type = "";
		numGuns = numBombs = numMissiles = numCargo = 0;
		ammo = new ItemStack[0];
		bombs = new ItemStack[0];
		missiles = new ItemStack[0];
		cargo = new ItemStack[0];
		fuel = ItemStack.EMPTY.copy();
	}
	
	public DriveableData(CompoundTag tags, int paintjobID)
	{
		this(tags);
		this.paintjobID = paintjobID;
	}
	
	public DriveableData(CompoundTag tags)
	{
		parts = new HashMap<>();
		readFromNBT(tags);
	}
	
	public void readFromNBT(CompoundTag tag)
	{
		if(tag == null)
			return;
		if(!tag.contains("Type"))
			return;
		
		type = tag.getStringOr("Type", "");
		DriveableType dType = DriveableType.getDriveable(type);
		if(dType == null)
		{
			ammo = new ItemStack[0];
			bombs = new ItemStack[0];
			missiles = new ItemStack[0];
			cargo = new ItemStack[0];
			fuel = ItemStack.EMPTY.copy();
			return;
		}
		numBombs = dType.numBombSlots;
		numCargo = dType.numCargoSlots;
		numMissiles = dType.numMissileSlots;
		numGuns = dType.ammoSlots();
		engine = PartType.getPart(tag.getStringOr("Engine", ""));
		paintjobID = tag.getIntOr("Paint", 0);
		ammo = new ItemStack[numGuns];
		bombs = new ItemStack[numBombs];
		missiles = new ItemStack[numMissiles];
		cargo = new ItemStack[numCargo];
		for(int i = 0; i < numGuns; i++)
			ammo[i] = ItemStackUtil.readItemStack(tag, "Ammo " + i);
		
		for(int i = 0; i < numBombs; i++)
			bombs[i] = ItemStackUtil.readItemStack(tag, "Bombs " + i);
		
		for(int i = 0; i < numMissiles; i++)
			missiles[i] = ItemStackUtil.readItemStack(tag, "Missiles " + i);
		
		for(int i = 0; i < numCargo; i++)
			cargo[i] = ItemStackUtil.readItemStack(tag, "Cargo " + i);
		
		fuel = ItemStackUtil.readItemStack(tag, "Fuel");
		fuelInTank = tag.getIntOr("FuelInTank", 0);
		for(EnumDriveablePart part : EnumDriveablePart.values())
		{
			parts.put(part, new DriveablePart(part, dType.health.get(part)));
		}
		for(DriveablePart part : parts.values())
		{
			part.readFromNBT(tag);
		}
	}
	
	public void writeToNBT(CompoundTag tag)
	{
		tag.putString("Type", type);
		if(engine != null)
			tag.putString("Engine", engine.shortName);
		tag.putInt("Paint", paintjobID);
		for(int i = 0; i < ammo.length; i++)
		{
			ItemStackUtil.writeItemStack(tag, "Ammo " + i, ammo[i]);
		}
		for(int i = 0; i < bombs.length; i++)
		{
			ItemStackUtil.writeItemStack(tag, "Bombs " + i, bombs[i]);
		}
		for(int i = 0; i < missiles.length; i++)
		{
			ItemStackUtil.writeItemStack(tag, "Missiles " + i, missiles[i]);
		}
		for(int i = 0; i < cargo.length; i++)
		{
			ItemStackUtil.writeItemStack(tag, "Cargo " + i, cargo[i]);
		}
		ItemStackUtil.writeItemStack(tag, "Fuel", fuel);
		tag.putInt("FuelInTank", (int)fuelInTank);
		for(DriveablePart part : parts.values())
		{
			part.writeToNBT(tag);
		}
	}
	
	@Override
	public int getContainerSize()
	{
		return getFuelSlot() + 1;
	}
	
	@Override
	public ItemStack getItem(int i)
	{
		//Find the correct inventory
		ItemStack[] inv = ammo;
		if(i >= ammo.length)
		{
			i -= ammo.length;
			inv = bombs;
			if(i >= bombs.length)
			{
				i -= bombs.length;
				inv = missiles;
				if(i >= missiles.length)
				{
					i -= missiles.length;
					inv = cargo;
					if(i >= cargo.length)
					{
						return fuel;
					}
				}
			}
		}
		//Return the stack in the slot
		return inv[i];
	}
	
	@Override
	public ItemStack removeItem(int i, int j)
	{
		//Find the correct inventory
		ItemStack[] inv = ammo;
		if(i >= ammo.length)
		{
			i -= ammo.length;
			inv = bombs;
			if(i >= bombs.length)
			{
				i -= bombs.length;
				inv = missiles;
				if(i >= missiles.length)
				{
					i -= missiles.length;
					inv = cargo;
					if(i >= cargo.length)
					{
						//Put the fuel stack in a stack array just to simplify the code
						i -= cargo.length;
						inv = new ItemStack[1];
						inv[0] = fuel;
						
						setItem(getFuelSlot(), ItemStack.EMPTY.copy());
					}
				}
			}
		}
		//Decrease the stack size
		if(inv[i] != null)
		{
			if(inv[i].getCount() <= j)
			{
				ItemStack itemstack = inv[i];
				inv[i] = ItemStack.EMPTY.copy();
				return itemstack;
			}
			ItemStack itemstack1 = inv[i].split(j);
			if(inv[i].getCount() <= 0)
			{
				inv[i] = ItemStack.EMPTY.copy();
			}
			return itemstack1;
		}
		else
		{
			return ItemStack.EMPTY.copy();
		}
		
	}
	
	@Override
	public ItemStack removeItemNoUpdate(int i)
	{
		ItemStack stack = getItem(i);
		setItem(i, ItemStack.EMPTY.copy());
		return stack;
	}
	
	@Override
	public void setItem(int i, ItemStack stack)
	{
		//Find the correct inventory
		ItemStack[] inv = ammo;
		if(i >= ammo.length)
		{
			i -= ammo.length;
			inv = bombs;
			if(i >= bombs.length)
			{
				i -= bombs.length;
				inv = missiles;
				if(i >= missiles.length)
				{
					i -= missiles.length;
					inv = cargo;
					if(i >= cargo.length)
					{
						fuel = stack;
						return;
					}
				}
			}
		}
		//Set the stack
		inv[i] = stack;
	}
	
	@Override
	public int getMaxStackSize()
	{
		return 64;
	}
	
	@Override
	public void setChanged()
	{
	}
	
	@Override
	public boolean stillValid(Player player)
	{
		return true;
	}
	
	public int getAmmoInventoryStart()
	{
		return 0;
	}
	
	public int getBombInventoryStart()
	{
		return ammo.length;
	}
	
	public int getMissileInventoryStart()
	{
		return ammo.length + bombs.length;
	}
	
	public int getCargoInventoryStart()
	{
		return ammo.length + bombs.length + missiles.length;
	}
	
	public int getFuelSlot()
	{
		return ammo.length + bombs.length + missiles.length + cargo.length;
	}
	
	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack)
	{
		if(i < getBombInventoryStart() && itemstack != null && itemstack.getItem() instanceof ItemBullet) //Ammo
		{
			return true;
		}
		if(i >= getBombInventoryStart() && i < getMissileInventoryStart() && itemstack != null && itemstack.getItem() instanceof ItemBullet) //Ammo
		{
			return true;
		}
		if(i >= getMissileInventoryStart() && i < getCargoInventoryStart() && itemstack != null && itemstack.getItem() instanceof ItemBullet)
		{
			return true;
		}
		if(i >= getCargoInventoryStart() && i < getFuelSlot())
		{
			return true;
		}
		if(i == getFuelSlot() && itemstack != null && itemstack.getItem() instanceof ItemPart && ((ItemPart)itemstack.getItem()).type.category == EnumPartCategory.FUEL) //Fuel
		{
			return true;
		}
		
		return false;
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
	
}
