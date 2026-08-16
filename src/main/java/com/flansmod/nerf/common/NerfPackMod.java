package com.flansmod.nerf.common;

import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;


public class NerfPackMod implements IFlansModContentProvider
{
	public static final String MODID = "nerfpack";
	public static final String VERSION = "@VERSION@";
	
	@Override
	public String GetContentFolder() 
	{
		return "Nerf Pack";
	}
	
	@Override
	public void RegisterModelRedirects()
	{
		FlansMod.RegisterModelRedirect("nerf", "com.flansmod.nerf.client.model");
	}
}
