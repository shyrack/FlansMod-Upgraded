package com.flansmod.apocalypse.common.world;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import com.flansmod.apocalypse.common.FlansModApocalypse;

public class BiomeApocalypse
{
	public static final ResourceKey<Biome> DEEP_CANYON_KEY = key("deepCanyon_apocalypse");
	public static final ResourceKey<Biome> CANYON_KEY = key("canyon_apocalypse");
	public static final ResourceKey<Biome> DESERT_KEY = key("desert_apocalypse");
	public static final ResourceKey<Biome> PLATEAU_KEY = key("plateau_apocalypse");
	public static final ResourceKey<Biome> HIGH_PLATEAU_KEY = key("highPlateau_apocalypse");
	public static final ResourceKey<Biome> SULPHUR_PITS_KEY = key("sulphurPits_apocalypse");

	public static Holder<Biome> deepCanyon, canyon, desert, plateau, highPlateau;
	public static Holder<Biome> sulphurPits;
	// Uppercase aliases kept for the 1.12.2-era call sites
	public static Holder<Biome> DEEP_CANYON, CANYON, DESERT, PLATEAU, HIGH_PLATEAU, SULPHUR_PITS;
	
	public static void registerBiomes()
	{
		deepCanyon = Holder.direct(BiomeDesertCanyon.create(-1.8F));
		canyon = Holder.direct(BiomeDesertCanyon.create(-1F));
		desert = Holder.direct(BiomeDesertCanyon.create(0F));
		plateau = Holder.direct(BiomeDesertCanyon.create(1F));
		highPlateau = Holder.direct(BiomeDesertCanyon.create(2.5F));
		
		sulphurPits = Holder.direct(BiomeSulphurPits.create());
		
		DEEP_CANYON = deepCanyon;
		CANYON = canyon;
		DESERT = desert;
		PLATEAU = plateau;
		HIGH_PLATEAU = highPlateau;
		SULPHUR_PITS = sulphurPits;
		// TODO APOCALYPSE: 26.1.2 has no runtime biome registry registration; biomes are
		// direct holders used only by the apocalypse biome source
	}
	
	private static ResourceKey<Biome> key(String name)
	{
		return ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Identifier.fromNamespaceAndPath(FlansModApocalypse.MODID, name));
	}
}
