package com.flansmod.utility.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class UtilityPackMod implements IFlansModContentProvider
{
	public static final String MODID = "flansutilitypack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Utility Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("utility", "com.flansmod.utility.client.model");
	}
}
