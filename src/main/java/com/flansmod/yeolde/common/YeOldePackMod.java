package com.flansmod.yeolde.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class YeOldePackMod implements IFlansModContentProvider
{
	public static final String MODID = "yeoldepack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Ye Olde Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("yeolde", "com.flansmod.yeolde.client.model");
	}
}
