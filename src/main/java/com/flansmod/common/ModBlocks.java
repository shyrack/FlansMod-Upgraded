package com.flansmod.common;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.paintjob.BlockPaintjobTable;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.BlockSpawner;

public class ModBlocks
{
	public static void register()
	{
		FlansMod.workbench = registerBlock("workbench",
				p -> new BlockFlansWorkbench(p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(3F, 6F)));
		FlansMod.spawner = registerBlock("teams_spawner",
				p -> new BlockSpawner(p.mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY).strength(1F)
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
		FlansMod.paintjobTable = registerBlock("paintjob_table",
				p -> new BlockPaintjobTable(p.mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2F, 4F)
						.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
		FlansMod.gunpowderBlock = registerBlock("gunpowder_block",
				p -> new BlockGunpowder(p.mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)
						.sound(net.minecraft.world.level.block.SoundType.SAND).strength(0.0F)
						.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
	}

	/**
	 * Registers a block, applying the registry key to the properties before the
	 * block is constructed (required by 26.1.2's Block constructor).
	 */
	public static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> factory)
	{
		Identifier id = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
		ResourceKey<Block> key = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
		T block = factory.apply(BlockBehaviour.Properties.of().setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}
}
