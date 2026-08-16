package com.flansmod.mechaparts.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class MechaPartsPackMod implements IFlansModContentProvider
{
	public static final String MODID = "mechapartspack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Mecha Parts Pack";
	}

	@Override
	public void RegisterModelRedirects() {}
}