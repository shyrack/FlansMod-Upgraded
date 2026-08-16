package com.flansmod.common.guns.boxes;

import com.flansmod.client.model.ModelBase;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public abstract class BoxType extends InfoType
{
	public String topTexturePath;
	public String sideTexturePath;
	public String bottomTexturePath;
	
	public BoxType(TypeFile file)
	{
		super(file);
	}

	@Override
	protected void read(String[] split, TypeFile file)
	{
		super.read(split, file);
		try
		{
			topTexturePath = Read(split, "TopTexture", topTexturePath);
			bottomTexturePath = Read(split, "BottomTexture", bottomTexturePath);
			sideTexturePath = Read(split, "SideTexture", sideTexturePath);
		}
		catch(Exception e)
		{
			FlansMod.log.error("Reading box file failed : " + shortName, e);
		}
	}

	@Override
	protected void preRead(TypeFile file)
	{
	}

	@Override
	protected void postRead(TypeFile file)
	{
	}

	@Override
	public ModelBase GetModel()
	{
		return null;
	}
}
