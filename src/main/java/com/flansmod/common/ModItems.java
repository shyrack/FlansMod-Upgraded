package com.flansmod.common;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.guns.ItemAAGun;
import com.flansmod.common.guns.ItemAttachment;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGrenade;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.teams.BlockSpawner;
import com.flansmod.common.paintjob.BlockPaintjobTable;
import com.flansmod.common.teams.ItemFlagpole;
import com.flansmod.common.teams.ItemOpStick;
import com.flansmod.common.teams.ItemRewardBox;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.types.InfoType;

/**
 * Registers the static (non content-pack) items: the generic per-category
 * items, the special items and the block items.
 */
public class ModItems
{
	public static Item gunItem;
	public static Item bulletItem;
	public static Item grenadeItem;
	public static Item attachmentItem;
	public static Item aaGunItem;
	public static Item planeItem;
	public static Item vehicleItem;
	public static Item mechaItem;
	public static Item mechaAddonItem;
	public static Item partItem;
	public static Item toolItem;
	public static Item teamArmourItem;
	public static Item flagpoleItem;
	public static Item opStickItem;
	public static Item rewardBoxItem;
	public static Item shootableItem;
	public static Item paintcanItem;

	public static void register()
	{
		//Special items
		ModBlocks.register();
		FlansMod.workbenchItem = (ItemBlockManyNames)registerItem("workbench", p -> new ItemBlockManyNames(FlansMod.workbench, p));
		FlansMod.opStick = (ItemOpStick)registerItem("op_stick", ItemOpStick::new);
		FlansMod.flag = (ItemFlagpole)registerItem("flagpole", ItemFlagpole::new);
		FlansMod.spawnerItem = (ItemBlockManyNames)registerItem("teams_spawner", p -> new ItemBlockManyNames(FlansMod.spawner, p));
		FlansMod.rainbowPaintcan = registerItem("rainbow_paintcan", Item::new);
		FlansMod.crosshairsymbol = registerItem("crosshairsymbol", Item::new);
		FlansMod.gunpowderBlockItem = registerItem("gunpowder_block", p -> new BlockItem(FlansMod.gunpowderBlock, p));

		//Generic category items
		gunItem = registerItem("gun", ItemGun::new);
		bulletItem = registerItem("bullet", ItemBullet::new);
		grenadeItem = registerItem("grenade", ItemGrenade::new);
		attachmentItem = registerItem("attachment", ItemAttachment::new);
		aaGunItem = registerItem("aa_gun", ItemAAGun::new);
		planeItem = registerItem("plane", ItemPlane::new);
		vehicleItem = registerItem("vehicle", ItemVehicle::new);
		mechaItem = registerItem("mecha", ItemMecha::new);
		mechaAddonItem = registerItem("mecha_addon", ItemMechaAddon::new);
		partItem = registerItem("part", ItemPart::new);
		toolItem = registerItem("tool", ItemTool::new);
		teamArmourItem = registerItem("team_armour", ItemTeamArmour::new);
		flagpoleItem = FlansMod.flag;
		opStickItem = FlansMod.opStick;
		rewardBoxItem = registerItem("reward_box", ItemRewardBox::new);
		shootableItem = registerItem("shootable", p -> new ItemShootable(p) {});
		paintcanItem = registerItem("paintcan", Item::new);
	}

	public static Item registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, Item> factory)
	{
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
		Item item = factory.apply(new net.minecraft.world.item.Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static BlockItem blockItem(Block block)
	{
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
		return new BlockItem(block, new net.minecraft.world.item.Item.Properties().setId(key));
	}

	public static ResourceKey<Item> itemKey(InfoType type)
	{
		String name = (type.contentPack + "_" + type.shortName).toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		return ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
	}

	public static Item registerTypeItem(Item item, InfoType type)
	{
		ResourceKey<Item> key = itemKey(type);
		if(BuiltInRegistries.ITEM.containsKey(key))
		{
			FlansMod.log.warn("Duplicate item key, skipping registration : " + key.identifier());
			return BuiltInRegistries.ITEM.getValue(key.identifier());
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static ItemStack getItemStack(Item item)
	{
		return new ItemStack(item);
	}
}
