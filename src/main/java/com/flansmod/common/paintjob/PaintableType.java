package com.flansmod.common.paintjob;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public abstract class PaintableType extends InfoType
{
	//Paintjobs
	/**
	 * The list of all available paintjobs for this gun
	 */
	public ArrayList<Paintjob> paintjobs = new ArrayList<>();
	public ArrayList<Paintjob> nonlegendarypaintjobs = new ArrayList<>();
	/**
	 * The default paintjob for this gun. This is created automatically in the load process from existing info
	 */
	public Paintjob defaultPaintjob;
	/**
	 * Assigns IDs to paintjobs
	 */
	private int nextPaintjobID = 1;
	
	private static HashMap<Integer, PaintableType> paintableTypes = new HashMap<>();
	
	public static PaintableType GetPaintableType(int iHash)
	{
		return paintableTypes.get(iHash);
	}
	
	public static PaintableType GetPaintableType(String name)
	{
		return paintableTypes.get(name.hashCode());
	}
	
	public PaintableType(TypeFile file)
	{
		super(file);
	}
	
	@Override
	public void postRead(TypeFile file)
	{
		super.postRead(file);
		
		//After all lines have been read, set up the default paintjob
		defaultPaintjob = new Paintjob(this, 0, "", texture, new ItemStack[0]);
		//Move to a new list to ensure that the default paintjob is always first
		ArrayList<Paintjob> newPaintjobList = new ArrayList<>();
		newPaintjobList.add(defaultPaintjob);
		newPaintjobList.addAll(paintjobs);
		paintjobs = newPaintjobList;
		if(infoTypes.containsKey(shortName.hashCode()))
		{
			FlansMod.Assert(false, "Duplicate info type name " + shortName);
		}
		
		nonlegendarypaintjobs.clear();
		for(Paintjob p : paintjobs)
		{
			if(!p.IsLegendary())
				nonlegendarypaintjobs.add(p);
		}
		
		paintableTypes.put(shortName.hashCode(), this);
	}
	
	/**
	 * Pack reader
	 */
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			//Paintjobs
			if(KeyMatches(split, "Paintjob"))
			{
				ItemStack[] dyeStacks = new ItemStack[(split.length - 3) / 2];
				for(int i = 0; i < (split.length - 3) / 2; i++)
				{
					if(split[i * 2 + 3].equals("rainbow"))
						dyeStacks[i] = new ItemStack(FlansMod.rainbowPaintcan, Integer.parseInt(split[i * 2 + 4]));
					else
						dyeStacks[i] = new ItemStack(getDyeItem(getDyeDamageValue(split[i * 2 + 3])), Integer.parseInt(split[i * 2 + 4]));
				}
				if(split[1].contains("_"))
				{
					int indexOf_ = split[1].indexOf('_');
					if(indexOf_ != -1 && split[1].toLowerCase().startsWith(iconPath.toLowerCase()))
					{
						split[1] = split[1].substring(indexOf_ + 1);
					}
				}
				paintjobs.add(new Paintjob(this, nextPaintjobID++, split[1], split[2], dyeStacks));
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading file failed : " + shortName, e);
		}
	}
	
	public static Item getDyeItem(int damage)
	{
		switch(damage)
		{
			case 0: return Items.WHITE_DYE;
			case 1: return Items.ORANGE_DYE;
			case 2: return Items.MAGENTA_DYE;
			case 3: return Items.LIGHT_BLUE_DYE;
			case 4: return Items.YELLOW_DYE;
			case 5: return Items.LIME_DYE;
			case 6: return Items.PINK_DYE;
			case 7: return Items.GRAY_DYE;
			case 8: return Items.LIGHT_GRAY_DYE;
			case 9: return Items.CYAN_DYE;
			case 10: return Items.PURPLE_DYE;
			case 11: return Items.BLUE_DYE;
			case 12: return Items.BROWN_DYE;
			case 13: return Items.GREEN_DYE;
			case 14: return Items.RED_DYE;
			case 15: return Items.BLACK_DYE;
			default: return Items.WHITE_DYE;
		}
	}
	
	public Paintjob getPaintjob(String s)
	{
		for(Paintjob paintjob : paintjobs)
		{
			if(paintjob.textureName.equals(s))
				return paintjob;
			
			if(paintjob.iconName.equals(s))
			{
				FlansMod.Assert(false, "Not sure this should be the right way to find a paintjob");
				return paintjob;
			}
		}
		FlansMod.Assert(false, "Could not find paintjob " + s);
		return defaultPaintjob;
	}
	
	public Paintjob getPaintjob(int i)
	{
		if(0 <= i && i < paintjobs.size())
			return paintjobs.get(i);
		return defaultPaintjob;
	}
	
	public float GetRecommendedScale()
	{
		return 50.0f;
	}
	
	public static boolean HasCustomPaintjob(ItemStack stack)
	{
		if(stack == null || stack.isEmpty())
		{
			return false;
		}
		
		if(stack.getItem() instanceof IPaintableItem)
		{
			return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("CustomPaint");
		}
		return false;
	}
	
	public static Identifier GetCustomPaintjobSkinResource(ItemStack stack)
	{
		CompoundTag tags = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty("CustomPaint");
		int customPaintHash = tags.getIntOr("Hash", 0);
		
		if(!FlansModResourceHandler.HasResourceForHash(customPaintHash))
		{
			FlansModResourceHandler.CreateSkinResourceFromByteArray(tags.getByteArray("Skin").orElse(new byte[0]), tags.getIntOr("SkinWidth", 0), tags.getIntOr("SkinHeight", 0), customPaintHash);
			FlansModResourceHandler.CreateIconResourceFromByteArray(tags.getByteArray("Icon").orElse(new byte[0]), tags.getIntOr("IconWidth", 0), tags.getIntOr("IconHeight", 0), customPaintHash);
		}
		
		return FlansModResourceHandler.GetSkinResourceFromHash(customPaintHash);
	}
	
	public static Identifier GetCustomPaintjobIconResource(ItemStack stack)
	{
		CompoundTag tags = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty("CustomPaint");
		int customPaintHash = tags.getIntOr("Hash", 0);
		
		if(!FlansModResourceHandler.HasResourceForHash(customPaintHash))
		{
			FlansModResourceHandler.CreateSkinResourceFromByteArray(tags.getByteArray("Skin").orElse(new byte[0]), tags.getIntOr("SkinWidth", 0), tags.getIntOr("SkinHeight", 0), customPaintHash);
			FlansModResourceHandler.CreateIconResourceFromByteArray(tags.getByteArray("Icon").orElse(new byte[0]), tags.getIntOr("IconWidth", 0), tags.getIntOr("IconHeight", 0), customPaintHash);
		}
		
		return FlansModResourceHandler.GetIconResourceFromHash(customPaintHash);
	}
}
