package com.flansmod.apocalypse.common.world;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class BiomeDesertCanyon
{
	private static final int field_150635_aE = 0, field_150636_aF = 1, field_150637_aG = 2;
	
	public static Biome create(float baseHeight)
	{
		// TODO APOCALYPSE: 1.12.2 BiomeProperties baseHeight/heightVariation are no longer used;
		// terrain height comes from the chunk generator instead
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
