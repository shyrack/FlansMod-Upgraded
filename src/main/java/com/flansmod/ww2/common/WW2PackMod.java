package com.flansmod.ww2.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class WW2PackMod implements IFlansModContentProvider
{
	public static final String MODID = "ww2pack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "WW2 Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("ww2", "com.flansmod.ww2.client.model");
	}
}
