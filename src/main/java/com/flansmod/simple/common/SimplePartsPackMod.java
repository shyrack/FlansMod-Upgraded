package com.flansmod.simple.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class SimplePartsPackMod implements IFlansModContentProvider
{
	public static final String MODID = "simplepartspack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Parts Pack";
	}

	@Override
	public void RegisterModelRedirects() {}
}