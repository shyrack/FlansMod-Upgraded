package com.flansmod.apocalypse.common.world;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class BiomeSulphurPits
{
	public static Biome create()
	{
		// TODO APOCALYPSE: 1.12.2 enabled sulphur lake decoration via the biome decorator;
		// lakes are now handled by ChunkProviderApocalypse decoration
		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(2F)
				.downfall(0F)
				.specialEffects(new BiomeSpecialEffects.Builder().waterColor(6316128).build())
				.mobSpawnSettings(MobSpawnSettings.EMPTY)
				.generationSettings(BiomeGenerationSettings.EMPTY)
				.build();
	}
}
