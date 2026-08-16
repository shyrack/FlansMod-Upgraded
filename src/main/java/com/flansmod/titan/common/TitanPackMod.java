package com.flansmod.titan.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class TitanPackMod implements IFlansModContentProvider
{
	public static final String MODID = "titanpack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Titan Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("titan", "com.flansmod.titan.client.model");
	}
}
