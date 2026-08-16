package com.flansmod.apocalypse.common.world;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

public class BiomeProviderApocalypse extends BiomeSource
{
	private final long seed;
	
	public BiomeProviderApocalypse(long seed)
	{
		this.seed = seed;
	}
	
	@Override
	protected MapCodec<? extends BiomeSource> codec()
	{
		return MapCodec.unit(this);
	}
	
	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes()
	{
		return ChunkProviderApocalypse.getPossibleBiomes().stream();
	}
	
	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler)
	{
		// TODO APOCALYPSE: 1.12.2 used GenLayer biome maps; replaced with deterministic hashing
		double a = Math.sin(x * 0.01D) + Math.cos(z * 0.01D);
		double b = Math.sin(x * 0.07D + 3D) + Math.cos(z * 0.05D + 1D);
		double c = Math.sin((x + z) * 0.03D);
		int value = (int)Mth.clamp((a * 1.5D + b + c * 2.5D + 2.5D) / 2.0D, 0, 5);
		switch(value)
		{
			case 0: return BiomeApocalypse.deepCanyon;
			case 1: return BiomeApocalypse.canyon;
			case 2: return BiomeApocalypse.desert;
			case 3: return BiomeApocalypse.plateau;
			case 4: return BiomeApocalypse.highPlateau;
			default: return BiomeApocalypse.sulphurPits;
		}
	}
}
