package com.flansmod.apocalypse.common.world;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.util.Mth;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.WorldGenLevel;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.apocalypse.common.world.buildings.WorldGenAbandonedPortal;
import com.flansmod.apocalypse.common.world.buildings.WorldGenBossPillar;
import com.flansmod.apocalypse.common.world.buildings.WorldGenDeadTree;
import com.flansmod.apocalypse.common.world.buildings.WorldGenDyeFactory;
import com.flansmod.apocalypse.common.world.buildings.WorldGenResearchLab;
import com.flansmod.apocalypse.common.world.buildings.WorldGenRoads;
import com.flansmod.apocalypse.common.world.buildings.WorldGenRunway;
import com.flansmod.apocalypse.common.world.buildings.WorldGenSkeleton;

public class ChunkProviderApocalypse extends ChunkGenerator
{
	protected static final BlockState STONE = Blocks.STONE.defaultBlockState();
	protected static final BlockState SAND = Blocks.SAND.defaultBlockState();
	protected static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
	protected static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
	private final Random rand = new Random();
	private final long seed;

	private WorldGenDyeFactory dyeFactoryGenerator = new WorldGenDyeFactory();
	private WorldGenRunway runwayGenerator = new WorldGenRunway();
	private WorldGenSkeleton skeletonGenerator = new WorldGenSkeleton();
	private WorldGenRoads roadsGenerator = new WorldGenRoads();
	private WorldGenBossPillar bossPillarGenerator = new WorldGenBossPillar();
	private WorldGenDeadTree deadTreeGenerator = new WorldGenDeadTree();
	private WorldGenResearchLab researchLabGenerator = new WorldGenResearchLab();
	private WorldGenAbandonedPortal abandonedPortalGenerator = new WorldGenAbandonedPortal();

	private final int seaLevel = 24;

	public ChunkProviderApocalypse(BiomeSource biomeSource, long seed)
	{
		super(biomeSource);
		this.seed = seed;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec()
	{
		return MapCodec.unit(this);
	}

	@Override
	public void applyCarvers(WorldGenRegion level, long seed, RandomState random, net.minecraft.world.level.biome.BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk)
	{
	}

	@Override
	public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk)
	{
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion level)
	{
	}

	@Override
	public int getGenDepth()
	{
		return 0;
	}

	/**
	 * The height of the terrain at a given x, z coordinate. A cheap deterministic pseudo-noise
	 * replacement for the 1.12.2 octave noise pipeline.
	 * // TODO APOCALYPSE: original Perlin-octave heightmap generation was not portable to 26.1.2
	 */
	private int getHeightAt(int x, int z)
	{
		double a = Math.sin(x * 0.05D) * 10D + Math.cos(z * 0.05D) * 10D;
		double b = Math.sin(x * 0.13D + 7D) * 6D + Math.cos(z * 0.11D + 3D) * 6D;
		double c = (Math.sin((x + z) * 0.021D) * 0.5D + Math.cos((x - z) * 0.017D) * 0.5D) * 24D;
		int height = (int)(seaLevel + 4D + a * 0.5D + b * 0.5D + c);
		return Mth.clamp(height, 8, 128);
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk)
	{
		// TODO APOCALYPSE: original noise/heightmap pipeline not portable; simple desert terrain generated instead
		int minY = chunk.getMinY();
		int maxY = chunk.getMaxY();
		for(int x = 0; x < 16; x++)
		{
			for(int z = 0; z < 16; z++)
			{
				int worldX = chunk.getPos().getMinBlockX() + x;
				int worldZ = chunk.getPos().getMinBlockZ() + z;
				int height = Mth.clamp(getHeightAt(worldX, worldZ), minY + 1, maxY - 1);
				for(int y = minY; y <= height; y++)
				{
					BlockState state;
					if(y == minY)
						state = BEDROCK;
					else if(y == height)
						state = SAND;
					else if(y >= height - 3)
						state = SANDSTONE;
					else
						state = STONE;
					chunk.setBlockState(new BlockPos(x, y, z), state);
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager)
	{
		super.applyBiomeDecoration(level, chunk, structureManager);

		if(!(level.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel))
			return;
		int x = chunk.getPos().x();
		int z = chunk.getPos().z();
		int i = x * 16;
		int j = z * 16;
		BlockPos blockpos = new BlockPos(i, 0, j);
		this.rand.setSeed(seed);
		long k = this.rand.nextLong() / 2L * 2L + 1L;
		long l = this.rand.nextLong() / 2L * 2L + 1L;
		this.rand.setSeed((long)x * k + (long)z * l ^ seed);

		if(rand.nextInt(FlansModApocalypse.DEAD_TREE_RARITY) == 0)
		{
			int k1 = this.rand.nextInt(16) + 8;
			int l1 = this.rand.nextInt(this.rand.nextInt(248) + 8);
			int i2 = this.rand.nextInt(16) + 8;
			deadTreeGenerator.generate(serverLevel, rand, blockpos.offset(k1, l1, i2));
		}

		if(rand.nextInt(FlansModApocalypse.DYE_FACTORY_RARITY) == 0)
		{
			int k1 = this.rand.nextInt(16) + 8;
			int l1 = this.rand.nextInt(this.rand.nextInt(248) + 8);
			int i2 = this.rand.nextInt(16) + 8;
			dyeFactoryGenerator.generate(serverLevel, rand, blockpos.offset(k1, l1, i2));
		}

		if(rand.nextInt(FlansModApocalypse.ABANDONED_PORTAL_APOC_RARITY) == 0)
		{
			int height = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, i + 8, j + 8);
			abandonedPortalGenerator.generate(serverLevel, rand, blockpos.offset(8, height, 8));
		}

		if(rand.nextInt(FlansModApocalypse.SKELETON_RARITY) == 0)
		{
			int k1 = this.rand.nextInt(16) + 8;
			int l1 = this.rand.nextInt(this.rand.nextInt(248) + 8);
			int i2 = this.rand.nextInt(16) + 8;
			skeletonGenerator.generate(serverLevel, rand, blockpos.offset(k1, l1, i2));
		}

		roadsGenerator.generate(serverLevel, rand, blockpos);
		bossPillarGenerator.generate(serverLevel, rand, blockpos);

		// TODO APOCALYPSE: runway and lab placement previously required BiomeProvider.areBiomesViable;
		// simplified to pure chance
		if(rand.nextInt(FlansModApocalypse.AIRPORT_RARITY) == 0)
		{
			runwayGenerator.generate(serverLevel, rand, new BlockPos(i + 8, 0, j + 8));
		}
		if(rand.nextInt(FlansModApocalypse.LAB_RARITY) == 0)
		{
			researchLabGenerator.generate(serverLevel, rand, new BlockPos(i, 0, j));
		}
	}

	@Override
	public int getSeaLevel()
	{
		return seaLevel;
	}

	@Override
	public int getMinY()
	{
		return 0;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random)
	{
		return Mth.clamp(getHeightAt(x, z), level.getMinY() + 1, level.getMaxY() - 1);
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random)
	{
		int minY = level.getMinY();
		int maxY = level.getMaxY();
		BlockState[] states = new BlockState[maxY - minY];
		int height = Mth.clamp(getHeightAt(x, z), minY + 1, maxY - 1);
		for(int y = minY; y < maxY; y++)
		{
			if(y == minY)
				states[y - minY] = BEDROCK;
			else if(y == height)
				states[y - minY] = SAND;
			else if(y > height)
				states[y - minY] = null;
			else if(y >= height - 3)
				states[y - minY] = SANDSTONE;
			else
				states[y - minY] = STONE;
		}
		return new NoiseColumn(minY, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos)
	{
	}

	/**
	 * The biome list this chunk generator can spawn. Used by the biome source.
	 */
	public static List<Holder<Biome>> getPossibleBiomes()
	{
		return List.of(
				BiomeApocalypse.DESERT,
				BiomeApocalypse.CANYON,
				BiomeApocalypse.DEEP_CANYON,
				BiomeApocalypse.HIGH_PLATEAU,
				BiomeApocalypse.PLATEAU,
				BiomeApocalypse.SULPHUR_PITS);
	}
}
