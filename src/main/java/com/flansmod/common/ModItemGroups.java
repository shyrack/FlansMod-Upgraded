package com.flansmod.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;

import com.flansmod.common.guns.GunType;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.driveables.mechas.MechaType;

public class ModItemGroups
{
	public static final List<ItemStack> guns = new ArrayList<>();
	public static final List<ItemStack> driveables = new ArrayList<>();
	public static final List<ItemStack> parts = new ArrayList<>();
	public static final List<ItemStack> teams = new ArrayList<>();
	public static final List<ItemStack> mechas = new ArrayList<>();

	public static void register()
	{
		FlansMod.tabFlanGuns = new CreativeTabFlan(0);
		FlansMod.tabFlanDriveables = new CreativeTabFlan(1);
		FlansMod.tabFlanParts = new CreativeTabFlan(2);
		FlansMod.tabFlanTeams = new CreativeTabFlan(3);
		FlansMod.tabFlanMechas = new CreativeTabFlan(4);
		CreativeModeTab[] tabs = {FlansMod.tabFlanGuns.tab, FlansMod.tabFlanDriveables.tab,
				FlansMod.tabFlanParts.tab, FlansMod.tabFlanTeams.tab, FlansMod.tabFlanMechas.tab};
		FlansMod.tabs = tabs;
	}

	public static void populateFromContentPacks()
	{
		for(GunType type : GunType.gunList)
		{
			if(type.item != null)
				FlansMod.tabFlanGuns.addItem(new ItemStack(type.item));
		}
		for(DriveableType type : DriveableType.types)
		{
			if(type.item != null)
				FlansMod.tabFlanDriveables.addItem(new ItemStack(type.item));
		}
		for(MechaType type : MechaType.types)
		{
			if(type.item != null)
				FlansMod.tabFlanMechas.addItem(new ItemStack(type.item));
		}
		for(PartType type : PartType.parts)
		{
			if(type.item != null)
				FlansMod.tabFlanParts.addItem(new ItemStack(type.item));
		}
		for(ArmourType type : ArmourType.armours)
		{
			if(type.item != null)
				FlansMod.tabFlanTeams.addItem(new ItemStack(type.item));
		}
	}
}
