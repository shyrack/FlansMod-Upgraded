package com.flansmod.common.types;

import com.flansmod.common.ModItems;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import com.flansmod.client.model.ModelBase;
import com.flansmod.common.FlansMod;

public class InfoType
{
	/**
	 * infoTypes
	 */
	public static HashMap<Integer, InfoType> infoTypes = new HashMap<>();
	
	public final String contentPack;
	public Item item;
	public int colour = 0xffffff;
	public String[] recipeLine;
	public char[][] recipeGrid = new char[3][3];
	public int recipeOutput = 1;
	public boolean shapeless;
	public String smeltableFrom = null;
	public String modelString = null;
	public String name = "";
	public String shortName = "";
	public String texture = "";
	public String description = "";
	public String iconPath = "";
	public float modelScale = 1F;
	/**
	 * If this is set to false, then this item cannot be dropped
	 */
	public boolean canDrop = true;
	
	public int hash = 0;
	
	public interface ParseFunc<T extends InfoType>
	{
		void Parse(String[] split, T d);
	}
	
	/**
	 * The probability that this item will appear in a dungeon chest.
	 * Scaled so that each chest is likely to have a fixed number of Flan's Mod items.
	 * Must be greater than or equal to 0, and should probably not exceed 100
	 */
	public int dungeonChance = 1;
	
	public static Random random = new Random();
	
	/**
	 * Used for scaling
	 */
	public static int totalDungeonChance = 0;
	
	public InfoType(TypeFile file)
	{
		contentPack = file.contentPack;
	}
	
	public void read(TypeFile file)
	{
		preRead(file);
		for(; ; )
		{
			String line = null;
			line = file.readLine();
			if(line == null)
				break;
			if(line.startsWith("//"))
				continue;
			String[] split = line.split(" ");
			if(split.length < 2)
				continue;
			read(split, file);
		}
		postRead(file);
		
	hash = file.hashCode();
		infoTypes.put(shortName.hashCode(), this);
		totalDungeonChance += dungeonChance;
	}
	
	/**
	 * Method for performing actions prior to reading the type file
	 */
	protected void preRead(TypeFile file)
	{
	}
	
	/**
	 * Method for performing actions after reading the type file
	 */
	protected void postRead(TypeFile file)
	{
		// Check that recommended values were set
		if(shortName.isEmpty())
		{
			FlansMod.log.warn("ShortName not set: " + file.name);
		}
		if(name.isEmpty())
		{
			FlansMod.log.warn("Name not set: " + file.name);
		}
	}
	
	public Object GetModel()
	{
		return null;
	}
	
	/**
	 * Pack reader
	 */
	protected void read(String[] split, TypeFile file)
	{
		try
		{
			// Standard line reads
			shortName = Read(split, "ShortName", shortName);
			name = ReadAndConcatenateMultipleStrings(split, "Name", name);
			description = ReadAndConcatenateMultipleStrings(split, "Description", description);
			
			modelString = Read(split, "Model", modelString);
			modelScale = Read(split, "ModelScale", modelScale);
			texture = Read(split, "Texture", texture);
			
			iconPath = Read(split, "Icon", iconPath);
			
			dungeonChance = Read(split, "DungeonProbability", dungeonChance);
			dungeonChance = Read(split, "DungeonLootChance", dungeonChance);
			
			recipeOutput = Read(split, "RecipeOutput", recipeOutput);
			
			smeltableFrom = Read(split, "SmeltableFrom", smeltableFrom);
			canDrop = Read(split, "CanDrop", canDrop);
			
			// More complicated line reads
			if(split[0].equals("Colour") || split[0].equals("Color"))
			{
				colour = (Integer.parseInt(split[1]) << 16) + ((Integer.parseInt(split[2])) << 8) + ((Integer.parseInt(split[3])));
			}
			
			if(split[0].equals("Recipe"))
			{
				for(int i = 0; i < 3; i++)
				{
					String line = null;
					line = file.readLine();
					if(line == null)
					{
						continue;
					}
					if(line.startsWith("//"))
					{
						i--;
						continue;
					}
					
					if(line.length() > 3)
						FlansMod.log.warn("Looks like a bad recipe in " + shortName + ". Double check whether '"
								+ line + "' is supposed to be part of the recipe");
					
					for(int j = 0; j < 3; j++)
					{
						recipeGrid[i][j] = j < line.length() ? line.charAt(j) : ' ';
					}
				}
				recipeLine = split;
				shapeless = false;
			}
			else if(split[0].equals("ShapelessRecipe"))
			{
				recipeLine = split;
				shapeless = true;
			}
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading file failed : " + shortName, e);
		}
	}
	
	/** -------------------------------------------------------------------------------------------------------- */
	/** HELPER FUNCTIONS FOR READING. Should give better debug output                                            */
	/**
	 * --------------------------------------------------------------------------------------------------------
	 */
	protected boolean KeyMatches(String[] split, String key)
	{
		return split != null && split.length > 1 && key != null && split[0].toLowerCase().equals(key.toLowerCase());
	}
	
	protected int Read(String[] split, String key, int currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length == 2)
			{
				try
				{
					currentValue = Integer.parseInt(split[1]);
				}
				catch(Exception e)
				{
					InfoType.LogError(shortName, "Incorrect format for " + key + ". Passed in value is not an integer");
				}
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <integer value>\"");
			}
		}
		
		return currentValue;
	}
	
	protected float Read(String[] split, String key, float currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length == 2)
			{
				try
				{
					currentValue = Float.parseFloat(split[1]);
				}
				catch(Exception e)
				{
					InfoType.LogError(shortName, "Incorrect format for " + key + ". Passed in value is not an float");
				}
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <float value>\"");
			}
		}
		
		return currentValue;
	}
	
	protected double Read(String[] split, String key, double currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length == 2)
			{
				try
				{
					currentValue = Double.parseDouble(split[1]);
				}
				catch(Exception e)
				{
					InfoType.LogError(shortName, "Incorrect format for " + key + ". Passed in value is not an float");
				}
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <float value>\"");
			}
		}
		
		return currentValue;
	}
	
	protected String Read(String[] split, String key, String currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length == 2)
			{
				currentValue = split[1];
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <singleWord>\"");
			}
		}
		
		return currentValue;
	}
	
	protected String ReadAndConcatenateMultipleStrings(String[] split, String key, String currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length > 1)
			{
				currentValue = split[1];
				for(int i = 0; i < split.length - 2; i++)
				{
					currentValue = currentValue + " " + split[i + 2];
				}
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <long string>\"");
			}
		}
		
		return currentValue;
	}
	
	protected boolean Read(String[] split, String key, boolean currentValue)
	{
		if(KeyMatches(split, key))
		{
			if(split.length == 2)
			{
				try
				{
					currentValue = Boolean.parseBoolean(split[1]);
				}
				catch(Exception e)
				{
					InfoType.LogError(shortName, "Incorrect format for " + key + ". Passed in value is not an boolean");
				}
			}
			else
			{
				InfoType.LogError(shortName, "Incorrect format for " + key + ". Should be \"" + key + " <true/false>\"");
			}
		}
		
		return currentValue;
	}
	/** -------------------------------------------------------------------------------------------------------- */
	/**                                                                                                          */
	/**
	 * --------------------------------------------------------------------------------------------------------
	 */
	
	protected static void LogError(String shortName, String s)
	{
		FlansMod.log.error("[Problem in " + shortName + ".txt]" + s);
	}
	
	@Override
	public String toString()
	{
		return super.getClass().getSimpleName() + ": " + shortName;
	}
	
	public void registerItem()
	{
		if(item != null)
			ModItems.registerTypeItem(item, this);
	}
	
	public void registerBlock()
	{
		
	}
	
	public void addRecipe()
	{
		// Modern recipes are JSON only. Recipe registration is handled by the JSON generator
		FlansMod.log.debug("Recipe data for " + shortName + " was not registered. Recipes are JSON only in modern Minecraft.");
	}
	
	/**
	 * Return a dye damage value from a string name
	 */
	protected int getDyeDamageValue(String dyeName)
	{
		int damage = -1;
		for(int i = 0; i < DyeColor.values().length; i++)
		{
			if(DyeColor.byId(i).getName().equals(dyeName))
				damage = i;
		}
		if(damage == -1)
			FlansMod.log.warn("Failed to find dye colour : " + dyeName + " while adding " + contentPack);
		
		return damage;
	}
	
	public Item getItem()
	{
		return item;
	}
			
	public static ItemStack getRecipeElement(String str)
	{
		String[] split = str.split("\\.");
		if(split.length == 0)
			return ItemStack.EMPTY;
		
		String id = split[0];
		int damage = split.length > 1 ? Short.parseShort(split[1]) : Short.MAX_VALUE;
		int amount = 1;
		
		return getRecipeElement(id, amount, damage);
	}
	
	public static Ingredient getRecipeIngredient(String str)
	{
		String[] split = str.split("\\.");
		if(split.length == 0)
			return Ingredient.of();
		
		String id = split[0];
		int damage = split.length > 1 ? Short.parseShort(split[1]) : Short.MAX_VALUE;
		int amount = 1;
		
		return getRecipeIngredient(id, amount, damage);
	}
	
	public static Ingredient getRecipeIngredient(String id, int amount, int damage)
	{
		// Legacy cases
		switch(id)
		{
			case "doorIron": return Ingredient.of(Items.IRON_DOOR);
			case "clayItem": return Ingredient.of(Items.CLAY_BALL);
			case "iron_trapdoor": return Ingredient.of(Blocks.OAK_TRAPDOOR.asItem());
			case "trapdoor": return Ingredient.of(Blocks.OAK_TRAPDOOR.asItem());
			case "gunpowder": return Ingredient.of(Items.GUNPOWDER);
			case "ingotIron":
			case "iron": return Ingredient.of(Items.IRON_INGOT);
			case "boat": return Ingredient.of(Items.OAK_BOAT);
		}
		
		// Special ingredients, allows for steel with iron fallback etc.
		if(SPECIAL_INGREDIENTS.containsKey(id))
		{
			return SPECIAL_INGREDIENTS.get(id);
		}
		
		return Ingredient.of(getRecipeElement(id, amount, damage).getItem());
	}
	
	public static ItemStack getRecipeElement(String id, int amount, int damage)
	{
		// Do a handful of special cases, mostly legacy recipes
		switch(id)
		{
			case "doorIron": return new ItemStack(Items.IRON_DOOR, amount);
			case "clayItem": return new ItemStack(Items.CLAY_BALL, amount);
			case "iron_trapdoor": return new ItemStack(Blocks.OAK_TRAPDOOR, amount);
			case "trapdoor": return new ItemStack(Blocks.OAK_TRAPDOOR, amount);
			case "gunpowder": return new ItemStack(Items.GUNPOWDER, amount);
			case "ingotIron":
			case "iron": return new ItemStack(Items.IRON_INGOT, amount);
			case "boat": return new ItemStack(Items.OAK_BOAT, amount);
		}
		
		// Now try a modern "modid:itemid" style lookup
		// No modid, try a search with "minecraft:"
		{
			String modPrefixName = id;
			if(!modPrefixName.contains(":"))
				modPrefixName = "minecraft:" + modPrefixName;
	
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(modPrefixName));
			if(item != null)
				return new ItemStack(item, amount);
		}
		
		// Then fallback to the original way we used to do it, for legacy packs
		for(InfoType type : infoTypes.values())
		{
			if(type.shortName.equals(id))
				return new ItemStack(type.item, amount);
		}
		
		// Special ingredients, just pick the first matching item
		if(SPECIAL_INGREDIENTS.containsKey(id))
		{
			Ingredient ing = SPECIAL_INGREDIENTS.get(id);
			for(Item item : BuiltInRegistries.ITEM)
			{
				if(item != null && ing.test(new ItemStack(item)))
					return new ItemStack(item, amount);
			}
		}

		for(Item item : BuiltInRegistries.ITEM)
		{
			if(item != null && (item.getDescriptionId().equals("item." + id) || item.getDescriptionId().equals("tile." + id)))
			{
				// Turned off console spam for this case. It's legacy, but there's so much of it now that this is pretty standard in official packs
				return new ItemStack(item, amount); 
			}
		}
		
		FlansMod.log.warn("Could not find " + id + " in recipe");		
		return ItemStack.EMPTY.copy();
	}
	
	/**
	 * To be overriden by subtypes for model reloading
	 */
	public void reloadModel()
	{
		
	}
	
	@Override
	public int hashCode()
	{
		return shortName.hashCode();
	}
	
	public static InfoType getType(String s)
	{
		return infoTypes.get(s.hashCode());
	}
	
	public static InfoType getType(int hash)
	{
		return infoTypes.get(hash);
	}
	
	//public void onWorldLoad(Level world)
	//{
	//	
	//}
	
	public static InfoType getType(ItemStack itemStack)
	{
		if(itemStack == null || itemStack.isEmpty())
			return null;
		Item item = itemStack.getItem();
		if(item instanceof IFlanItem)
			return ((IFlanItem)item).getInfoType();
		return null;
	}
	
	public static MobEffectInstance getPotionEffect(String[] split)
	{
		int potionID = Integer.parseInt(split[1]);
		int duration = Integer.parseInt(split[2]);
		int amplifier = Integer.parseInt(split[3]);
		Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(potionID);
		if(effect.isEmpty())
			return null;
		return new MobEffectInstance(effect.get(), duration, amplifier, false, false);
	}
	
	private static HashMap<String, Ingredient> SPECIAL_INGREDIENTS = new HashMap<String, Ingredient>();
	public static void InitializeSpecialIngredients()
	{
		// Steel ingot - fallback is iron
		AddOreDictEntry("nuggetSteel", Ingredient.of(Items.IRON_NUGGET));
		AddOreDictEntry("ingotSteel", Ingredient.of(Items.IRON_INGOT));
		AddOreDictEntry("blockSteel", Ingredient.of(Blocks.IRON_BLOCK.asItem()));
		// Nickel with fallback iron
		AddOreDictEntry("nuggetNickel", Ingredient.of(Items.IRON_NUGGET));
		AddOreDictEntry("ingotNickel", Ingredient.of(Items.IRON_INGOT));
		AddOreDictEntry("blockNickel", Ingredient.of(Blocks.IRON_BLOCK.asItem()));
		// Lead with fallback iron
		AddOreDictEntry("nuggetLead", Ingredient.of(Items.IRON_NUGGET));
		AddOreDictEntry("ingotLead", Ingredient.of(Items.IRON_INGOT));
		AddOreDictEntry("blockLead", Ingredient.of(Blocks.IRON_BLOCK.asItem()));
		// Copper with fallback iron
		AddOreDictEntry("nuggetCopper", Ingredient.of(Items.IRON_NUGGET));
		AddOreDictEntry("ingotCopper", Ingredient.of(Items.IRON_INGOT));
		AddOreDictEntry("blockCopper", Ingredient.of(Blocks.IRON_BLOCK.asItem()));
		// Tin with fallback iron
		AddOreDictEntry("nuggetTin", Ingredient.of(Items.IRON_NUGGET));
		AddOreDictEntry("ingotTin", Ingredient.of(Items.IRON_INGOT));
		AddOreDictEntry("blockTin", Ingredient.of(Blocks.IRON_BLOCK.asItem()));
		
		// Electrum with fallback gold
		AddOreDictEntry("nuggetElectrum", Ingredient.of(Items.GOLD_NUGGET));
		AddOreDictEntry("ingotElectrum", Ingredient.of(Items.GOLD_INGOT));
		AddOreDictEntry("blockElectrum", Ingredient.of(Blocks.GOLD_BLOCK.asItem()));
		// Constantan with fallback gold
		AddOreDictEntry("nuggetConstantan", Ingredient.of(Items.GOLD_NUGGET));
		AddOreDictEntry("ingotConstantan", Ingredient.of(Items.GOLD_INGOT));
		AddOreDictEntry("blockConstantan", Ingredient.of(Blocks.GOLD_BLOCK.asItem()));
		// Silver with fallback gold
		AddOreDictEntry("nuggetSilver", Ingredient.of(Items.GOLD_NUGGET));
		AddOreDictEntry("ingotSilver", Ingredient.of(Items.GOLD_INGOT));
		AddOreDictEntry("blockSilver", Ingredient.of(Blocks.GOLD_BLOCK.asItem()));
		// Bronze with fallback gold
		AddOreDictEntry("nuggetBronze", Ingredient.of(Items.GOLD_NUGGET));
		AddOreDictEntry("ingotBronze", Ingredient.of(Items.GOLD_INGOT));
		AddOreDictEntry("blockBronze", Ingredient.of(Blocks.GOLD_BLOCK.asItem()));

		// IE lookups
		AddModEntry("treatedPlanks", "immersiveengineering:treated_wood",  Ingredient.of(Blocks.OAK_PLANKS.asItem()));
	}
	
	private static void AddModEntry(String name, String resLoc, Ingredient fallback)
	{
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(resLoc));
		if(item != null)
			SPECIAL_INGREDIENTS.put(name, Ingredient.of(item));
		else
			SPECIAL_INGREDIENTS.put(name, fallback);
	}
	
	private static void AddOreDictEntry(String name, Ingredient fallback)
	{
		SPECIAL_INGREDIENTS.put(name, fallback);
	}
}
