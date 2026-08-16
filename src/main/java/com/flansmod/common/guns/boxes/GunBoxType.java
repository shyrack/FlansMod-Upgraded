package com.flansmod.common.guns.boxes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public class GunBoxType extends BoxType
{
	public BlockGunBox block;
	
	/**
	 * Stores pages for the gun box indexed by their title (unlocalized!)
	 */
	public HashMap<String, GunBoxPage> pagesByTitle;
	public ArrayList<GunBoxPage> pages;
	
	/**
	 * Points to the page we are currently adding to.
	 */
	private GunBoxPage currentPage;
	
	public GunBoxPage defaultPage;
	
	private static int lastIconIndex = 2;
	public static HashMap<String, GunBoxType> gunBoxMap = new HashMap<>();
	
	public GunBoxType(TypeFile file)
	{
		super(file);
		
		pagesByTitle = new HashMap<>();
		pages = new ArrayList<>();
	}
	
	@Override
	public void preRead(TypeFile file)
	{
		super.preRead(file);
		//Make sure NumGuns is read before anything else
		for(String line : file.getLines())
		{
			if(line == null)
				break;
			if(line.startsWith("//"))
				continue;
			String[] split = line.split(" ");
			if(split.length < 2)
				continue;
			
			if(split[0].equals("NumGuns"))
			{
				pagesByTitle.put("default", currentPage = new GunBoxPage("default"));
				pages.add(currentPage);
			}
		}
	}
	
	@Override
	public void postRead(TypeFile file)
	{
		super.postRead(file);
		gunBoxMap.put(this.shortName, this);
	}

	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			//Sets the current page of the reader.
			if(split[0].equals("SetPage"))
			{
				String pageName = split[1];
				for(int i = 2; i < split.length; i++)
					pageName += " " + split[i];
				if(pagesByTitle.get(pageName) == null)
					pagesByTitle.put(pageName, currentPage = new GunBoxPage(pageName));
				pages.add(currentPage);
				
			}
			//Add an info type at the top level.
			else if(split[0].equals("AddGun") || split[0].equals("AddType"))
			{
				currentPage.addNewEntry(InfoType.getType(split[1]), getRecipe(split));
			}
			//Add a subtype (such as ammo) to the current top level InfoType
			else if(split[0].equals("AddAmmo") || split[0].equals("AddAltType") || split[0].equals("AddAltAmmo") || split[0].equals("AddAlternateAmmo"))
			{
				currentPage.addAmmoToCurrentEntry(InfoType.getType(split[1]), getRecipe(split));
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading gun box file failed : " + shortName, e);
		}
	}
	
	private List<ItemStack> getRecipe(String[] split)
	{
		List<ItemStack> recipe = new ArrayList<>();
		
		for(int i = 0; i < (split.length - 2) / 2; i++)
		{
			if(split[i * 2 + 3].contains("."))
				recipe.add(getRecipeElement(split[i * 2 + 3].split("\\.")[0], Integer.parseInt(split[i * 2 + 2]), Integer.valueOf(split[i * 2 + 3].split("\\.")[1])));
			else
				recipe.add(getRecipeElement(split[i * 2 + 3], Integer.parseInt(split[i * 2 + 2]), 0));
		}
		
		return recipe;
	}

	public static GunBoxType getBox(String s)
	{
		return gunBoxMap.get(s);
	}
	
	public static GunBoxType getBox(Block block)
	{
		for(GunBoxType type : gunBoxMap.values())
		{
			if(type.block == block)
				return type;
		}
		return null;
	}
	
	/**
	 * Represents a page in the gun box
	 */
	public static class GunBoxPage
	{
		public List<GunBoxEntryTopLevel> entries;
		/**
		 * Points to the gun box entry we are currently reading from file. Allows for the old format to write in ammo on a separate line to the gun.
		 */
		private GunBoxEntryTopLevel currentlyEditing;
		public String name;
		
		public GunBoxPage(String s)
		{
			name = s;
			entries = new ArrayList<>();
		}
		
		public void addNewEntry(InfoType type, List<ItemStack> requiredParts)
		{
			GunBoxEntryTopLevel entry = new GunBoxEntryTopLevel(type, requiredParts);
			entries.add(entry);
			currentlyEditing = entry;
		}
		
		public void addAmmoToCurrentEntry(InfoType type, List<ItemStack> requiredParts)
		{
			currentlyEditing.addAmmo(type, requiredParts);
		}
	}
	
	/**
	 * Represents an entry on a page of the gun box.
	 */
	public static class GunBoxEntry
	{
		public InfoType type;
		public List<ItemStack> requiredParts;
		
		public GunBoxEntry(InfoType type, List<ItemStack> requiredParts)
		{
			this.type = type;
			this.requiredParts = requiredParts;
		}
		
		public boolean haveEnoughOf(Container inv, ItemStack stackNeeded)
		{
			//Create a temporary copy of the player inventory for backup purposes
			Inventory temporaryInventory = new Inventory(null, null);
			for(int i = 0; i < inv.getContainerSize(); i++)
			{
				temporaryInventory.setItem(i, inv.getItem(i).copy());
			}
			
			return haveEnoughOf(temporaryInventory, stackNeeded, false);
		}
		
		private boolean haveEnoughOf(Container temporaryInventory, ItemStack stackNeeded, boolean takeItems)
		{
			//The total amount of items found that match this recipe stack
			int totalAmountFound = 0;
			//Iterate over the temporary inventory
			for(int m = 0; m < temporaryInventory.getContainerSize(); m++)
			{
				//Get the stack in each slot
				ItemStack stackInSlot = temporaryInventory.getItem(m).copy();
				//If the stack is what we want
				if(stackInSlot.getItem() == stackNeeded.getItem() && stackInSlot.getDamageValue() == stackNeeded.getDamageValue())
				{
					//Work out the amount to take from the stack
					int amountFound = Math.min(stackInSlot.getCount(), stackNeeded.getCount() - totalAmountFound);
					//Take it
					stackInSlot.setCount(stackInSlot.getCount() - amountFound);
					//Check for empty stacks
					if(stackInSlot.getCount() <= 0)
						stackInSlot = ItemStack.EMPTY.copy();
					//Put the modified stack back in the inventory
					temporaryInventory.setItem(m, stackInSlot);
					//Increase the amount found counter
					totalAmountFound += amountFound;
					//If we have enough, stop looking
					if(totalAmountFound == stackNeeded.getCount())
						break;
				}
			}
			return totalAmountFound >= stackNeeded.getCount();
		}
		
		public boolean canCraft(Container inv, boolean takeItems)
		{
			//Create a temporary copy of the player inventory for backup purposes
			Inventory temporaryInventory = new Inventory(null, null);
			for(int i = 0; i < inv.getContainerSize(); i++)
			{
				temporaryInventory.setItem(i, inv.getItem(i).copy());
			}

			//This becomes false if some recipe element is not found on the player
			boolean canCraft = true;
			
			//Draw the stacks that should be in each slot
			for(ItemStack stackNeeded : requiredParts)
			{
				if(!haveEnoughOf(temporaryInventory, stackNeeded, takeItems))
				{
					canCraft = false;
					break;
				}
			}
			
			if(canCraft && takeItems)
			{
				for(int i = 0; i < inv.getContainerSize(); i++)
					inv.setItem(i, temporaryInventory.getItem(i));
			}
			
			return canCraft;
		}
	}
	
	/**
	 * Represents a top level entry, normally a gun. This has child entries, normally ammo that are listed below in the GUI.
	 */
	public static class GunBoxEntryTopLevel extends GunBoxEntry
	{
		public List<GunBoxEntry> childEntries;
		
		public GunBoxEntryTopLevel(InfoType type, List<ItemStack> requiredParts)
		{
			super(type, requiredParts);
			childEntries = new ArrayList<>();
		}

		public void addAmmo(InfoType type, List<ItemStack> requiredParts)
		{
			childEntries.add(new GunBoxEntry(type, requiredParts));
		}
	}

	public GunBoxEntry canCraft(InfoType type)
	{
		for(GunBoxPage page : pagesByTitle.values())
		{
			for(GunBoxEntryTopLevel entry : page.entries)
			{
				if(entry.type == type)
					return entry;
				else
				{
					for(GunBoxEntry child : entry.childEntries)
					{
						if(child.type == type)
							return child;
					}
				}
			}
		}
		return null;
	}
}
