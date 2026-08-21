package com.flansmod.common;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import com.flansmod.common.driveables.ContainerDriveableInventory;
import com.flansmod.common.driveables.ContainerDriveableMenu;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;
import com.flansmod.common.driveables.mechas.EntityMecha;

public class ModMenus
{
	public record DriveableMenuData(int driveableId, int screenId)
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, DriveableMenuData> STREAM_CODEC =
				StreamCodec.composite(
						ByteBufCodecs.INT, DriveableMenuData::driveableId,
						ByteBufCodecs.INT, DriveableMenuData::screenId,
						DriveableMenuData::new);
	}

	public static ExtendedMenuType<ContainerDriveableMenu, DriveableMenuData> DRIVEABLE_MENU;
	public static ExtendedMenuType<ContainerDriveableMenu, DriveableMenuData> DRIVEABLE_FUEL;
	public static ExtendedMenuType<ContainerDriveableInventory, DriveableMenuData> DRIVEABLE_INVENTORY;
	public static ExtendedMenuType<ContainerMechaInventory, DriveableMenuData> MECHA_INVENTORY;

	public static void init()
	{
		DRIVEABLE_MENU = register("driveable_menu", new ExtendedMenuType<>((containerId, inventory, data) ->
				new ContainerDriveableMenu(ModMenus.DRIVEABLE_MENU, containerId, inventory,
						resolveDriveable(inventory, data.driveableId()), false), DriveableMenuData.STREAM_CODEC));
		DRIVEABLE_FUEL = register("driveable_fuel", new ExtendedMenuType<>((containerId, inventory, data) ->
				new ContainerDriveableMenu(ModMenus.DRIVEABLE_FUEL, containerId, inventory,
						resolveDriveable(inventory, data.driveableId()), true), DriveableMenuData.STREAM_CODEC));
		DRIVEABLE_INVENTORY = register("driveable_inventory", new ExtendedMenuType<>((containerId, inventory, data) ->
				new ContainerDriveableInventory(ModMenus.DRIVEABLE_INVENTORY, containerId, inventory,
						resolveDriveable(inventory, data.driveableId()), screenForGUI(data.screenId())), DriveableMenuData.STREAM_CODEC));
		MECHA_INVENTORY = register("mecha_inventory", new ExtendedMenuType<>((containerId, inventory, data) ->
		{
			EntityDriveable driveable = resolveDriveable(inventory, data.driveableId());
			return new ContainerMechaInventory(ModMenus.MECHA_INVENTORY, containerId, inventory,
					driveable instanceof EntityMecha ? (EntityMecha)driveable : null);
		}, DriveableMenuData.STREAM_CODEC));
	}

	/**
	 * Resolves the driveable entity for a container being created. On the
	 * client the entity is looked up in the client level; on the server it is
	 * looked up in the passed inventory's player's level. Returns null if the
	 * entity is not (yet) loaded, in which case the container is created
	 * without driveable slots and its stillValid check closes it again.
	 */
	public static EntityDriveable resolveDriveable(Inventory inventory, int driveableId)
	{
		Level level = null;
		if(FlansMod.isClient())
		{
			level = FlansMod.proxy.getClientLevel();
		}
		else if(inventory != null && inventory.player != null)
		{
			level = inventory.player.level();
		}
		if(level == null)
			return null;
		Entity entity = level.getEntity(driveableId);
		return entity instanceof EntityDriveable ? (EntityDriveable)entity : null;
	}

	/**
	 * Maps a PacketDriveableGUI id to the screen layout used by
	 * ContainerDriveableInventory (0 = Guns, 1 = Bombs, 2 = Cargo, 3 = Missiles).
	 */
	public static int screenForGUI(int guiID)
	{
		switch(guiID)
		{
			case 1: return 1; //Bombs / Mines
			case 3: return 2; //Cargo
			case 5: return 3; //Missiles / Shells
			default: return 0; //Guns
		}
	}

	private static <T extends AbstractContainerMenu> ExtendedMenuType<T, DriveableMenuData> register(String name, ExtendedMenuType<T, DriveableMenuData> type)
	{
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		Registry.register(BuiltInRegistries.MENU, id, type);
		return type;
	}
}
