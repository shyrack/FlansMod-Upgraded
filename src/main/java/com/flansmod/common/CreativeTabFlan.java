package com.flansmod.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

/**
 * Creative tab wrapper. Keeps the classic FlansMod tab API while building a
 * modern {@link CreativeModeTab}. Items are appended through
 * {@link #addItem(ItemStack)} and flushed to the tab on the modify-entries
 * event.
 */
public class CreativeTabFlan
{
	public int type; //0 = Guns, 1 = Vehicles + Planes, 2 = Teams, 3 = Parts, 4 = Mechas
	public int icon;
	public int time = 0;
	public final CreativeModeTab tab;
	public final List<Item> items = new ArrayList<>();
	public final List<ItemStack> extraStacks = new ArrayList<>();

	public CreativeTabFlan(int i)
	{
		type = i;
		final CreativeTabFlan self = this;
		net.minecraft.resources.ResourceKey<CreativeModeTab> key = net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.key(),
				net.minecraft.resources.Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "tab_" + tabName(i)));
		tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.flansmod." + tabName(i)))
				.icon(() -> self.getIconStack())
				.build();
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB, key, tab);
		net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(key).register(output ->
		{
			for(Item item : self.items)
				output.accept(new ItemStack(item));
			for(ItemStack stack : self.extraStacks)
				output.accept(stack);
		});
	}

	private static String tabName(int i)
	{
		switch(i)
		{
			case 0: return "guns";
			case 1: return "driveables";
			case 2: return "parts";
			case 3: return "teams";
			case 4: return "mechas";
		}
		return "guns";
	}

	public ItemStack getIconStack()
	{
		icon = FlansMod.ticker / 20;
		switch(type)
		{
			case 0: return GunType.gunList.isEmpty() ? new ItemStack(Blocks.WHITE_WOOL) : new ItemStack(GunType.gunList.get(icon % GunType.gunList.size()).item);
			case 1: return DriveableType.types.isEmpty() ? new ItemStack(Blocks.WHITE_WOOL) : new ItemStack(DriveableType.types.get(icon % DriveableType.types.size()).item);
			case 2: return FlansMod.partItems.isEmpty() ? new ItemStack(Blocks.WHITE_WOOL) : new ItemStack(FlansMod.partItems.get(icon % FlansMod.partItems.size()));
			case 3: return FlansMod.armourItems.isEmpty() ? new ItemStack(Blocks.WHITE_WOOL) : new ItemStack(FlansMod.armourItems.get(icon % FlansMod.armourItems.size()));
			case 4: return FlansMod.mechaItems.isEmpty() ? new ItemStack(Blocks.WHITE_WOOL) : new ItemStack(FlansMod.mechaItems.get(icon % FlansMod.mechaItems.size()));
			case 5: return new ItemStack(Blocks.SAND);
		}
		return new ItemStack(FlansMod.workbench);
	}

	public void addItem(ItemStack stack)
	{
		extraStacks.add(stack);
	}

	public void addItem(Item item)
	{
		items.add(item);
		items.sort((a, b) -> ItemSorter.compareItems(a, b));
	}

	public void addItem(net.minecraft.world.level.ItemLike itemLike)
	{
		addItem(itemLike.asItem());
	}

	public Consumer<ItemStack> getConsumer()
	{
		return this::addItem;
	}

	private static class ItemSorter
	{
		public static int compareItems(Item itemA, Item itemB)
		{
			boolean invalidA = !(itemA instanceof IFlanItem);
			boolean invalidB = !(itemB instanceof IFlanItem);
			if(invalidA)
			{
				return invalidB ? 0 : -1;
			}
			else if(invalidB)
			{
				return 1;
			}

			InfoType typeA = ((IFlanItem)itemA).getInfoType();
			InfoType typeB = ((IFlanItem)itemB).getInfoType();
			if(typeA == null)
			{
				return typeB == null ? 0 : -1;
			}
			else if(typeB == null)
			{
				return 1;
			}

			int contentPackComparison = typeA.contentPack.compareTo(typeB.contentPack);
			if(contentPackComparison != 0)
			{
				return contentPackComparison;
			}

			int classComparison = typeA.getClass().getSimpleName().compareTo(typeB.getClass().getSimpleName());
			if(classComparison != 0)
			{
				return classComparison;
			}

			return typeA.name.compareTo(typeB.name);
		}
	}
}
