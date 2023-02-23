package com.shyrack.flansmodupgraded.common.guns;

import java.util.HashMap;

import net.minecraft.item.ItemStack;

import com.flansmod.common.FlansMod;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.types.EnumPaintjobRarity;

public class Paintjob
{
	private static HashMap<Integer, Paintjob> paintjobs = new HashMap<>();
	
	public PaintableType parent;
	public String iconName;
	public String textureName;
	public ItemStack[] dyesNeeded;
	public int ID;
	public EnumPaintjobRarity rarity;
	
	public Paintjob(PaintableType parent, int id, String iconName, String textureName, ItemStack[] dyesNeeded)
	{
		this.parent = parent;
		this.ID = id;
		this.iconName = iconName;
		this.textureName = textureName;
		this.dyesNeeded = dyesNeeded;
		this.rarity = EnumPaintjobRarity.UNKNOWN;
		
		paintjobs.put(hashCode(), this);
	}
	
	@Override
	public int hashCode()
	{
		return parent.hashCode() ^ ID;
	}
	
	public boolean IsLegendary()
	{
		for(ItemStack stack : dyesNeeded)
		{
			if(stack.getItem() == FlansMod.rainbowPaintcan)
				return true;
		}
		return false;
	}
	
	public static Paintjob GetPaintjob(int hash)
	{
		return paintjobs.get(hash);
	}
}
