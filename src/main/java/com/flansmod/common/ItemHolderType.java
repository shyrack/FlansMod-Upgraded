package com.flansmod.common;

import java.util.HashMap;

import com.flansmod.client.model.ModelBase;
import com.flansmod.client.model.ModelItemHolder;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ItemHolderType extends InfoType
{
	public ModelItemHolder model;
	
	public BlockItemHolder block;
	
	private static HashMap<String, ItemHolderType> itemHolders = new HashMap<>();
	
	public ItemHolderType(TypeFile file)
	{
		super(file);
	}
	
	@Override
	protected void preRead(TypeFile file)
	{
	}
	
	@Override
	public void postRead(TypeFile file)
	{
		itemHolders.put(this.shortName, this);
	}
	
	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			if(FlansMod.isClient() && split[0].equals("Model"))
				model = FlansMod.proxy.loadModel(split[1], shortName, ModelItemHolder.class);
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading item holder file failed : " + shortName, e);
		}
	}
	
	@Override
	public void registerItem()
	{
		item = ModItems.blockItem(block);
		ModItems.registerTypeItem(item, this);
	}
	
	@Override
	public void registerBlock()
	{
		String name = (contentPack + "_" + shortName).toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
		ItemHolderType self = this;
		block = ModBlocks.registerBlock(name, p -> new BlockItemHolder(
				p.mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(2F, 4F), self));
	}
	
	public static ItemHolderType getItemHolder(String string)
	{
		return itemHolders.get(string);
	}
	
	/**
	 * To be overriden by subtypes for model reloading
	 */
	@Override
	public void reloadModel()
	{
		model = FlansMod.proxy.loadModel(modelString, shortName, ModelItemHolder.class);
	}
	
	@Override
	public ModelBase GetModel()
	{
		return model;
	}
}
