package com.flansmod.common;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.flansmod.common.paintjob.TileEntityPaintjobTable;
import com.flansmod.common.teams.TileEntitySpawner;

public class ModBlockEntities
{
	public static BlockEntityType<TileEntityItemHolder> ITEM_HOLDER;
	public static BlockEntityType<TileEntitySpawner> SPAWNER;
	public static BlockEntityType<TileEntityPaintjobTable> PAINTJOB_TABLE;

	public static void register()
	{
		ITEM_HOLDER = register("item_holder", TileEntityItemHolder::new, FlansMod.workbench);
		SPAWNER = register("teams_spawner", TileEntitySpawner::new, FlansMod.spawner);
		PAINTJOB_TABLE = register("paintjob_table", TileEntityPaintjobTable::new, FlansMod.paintjobTable);
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name,
			net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.Factory<? extends T> factory, Block block)
	{
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		ResourceKey<BlockEntityType<?>> key = ResourceKey.create(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), id);
		BlockEntityType<T> type = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<T>create(factory, block).build();
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
	}
}
