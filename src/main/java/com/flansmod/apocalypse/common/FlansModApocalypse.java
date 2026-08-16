package com.flansmod.apocalypse.common;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.flansmod.apocalypse.common.blocks.BlockPowerCube;
import com.flansmod.apocalypse.common.blocks.BlockStatic;
import com.flansmod.apocalypse.common.blocks.BlockSulphur;
import com.flansmod.apocalypse.common.blocks.BlockSulphuricAcid;
import com.flansmod.apocalypse.common.blocks.TileEntityPowerCube;
import com.flansmod.apocalypse.common.entity.EntitySkullDrone;
import com.flansmod.apocalypse.common.entity.EntityFakePlayer;
import com.flansmod.apocalypse.common.entity.EntityFlyByPlane;
import com.flansmod.apocalypse.common.entity.EntityNukeDrop;
import com.flansmod.apocalypse.common.entity.EntitySkullBoss;
import com.flansmod.apocalypse.common.entity.EntitySkuller;
import com.flansmod.apocalypse.common.entity.EntitySurvivor;
import com.flansmod.apocalypse.common.entity.EntityTeleporter;
import com.flansmod.apocalypse.common.entity.EntityFlansModShooter;
import com.flansmod.apocalypse.common.world.BiomeApocalypse;
import com.flansmod.apocalypse.common.world.BiomeProviderApocalypse;
import com.flansmod.apocalypse.common.world.ChunkProviderApocalypse;
import com.flansmod.apocalypse.common.world.buildings.WorldGenBossPillar;
import com.flansmod.common.BlockItemHolder;
import com.flansmod.common.Configuration;
import com.flansmod.common.CreativeTabFlan;
import com.flansmod.common.FlansMod;
import com.flansmod.common.IFlansModContentProvider;
import com.flansmod.common.ItemHolderType;
import com.flansmod.common.ModEntities;
import com.flansmod.common.enchantments.GloveType;
import com.flansmod.common.enchantments.ItemGlove;
import com.flansmod.common.parts.PartType;

public class FlansModApocalypse implements ModInitializer, IFlansModContentProvider
{
	//Core mod stuff
	public static boolean DEBUG = false;
	public static final String MODID = "flansmodapocalypse";
	public static final String MOD_ID = MODID;
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("flansmodapocalypse");
	public static final String VERSION = "5.10.0";
	
	public static FlansModApocalypse INSTANCE;
	public static CommonProxyApocalypse proxy;
	
	//Config options
	public static Configuration configFile;
	/**
	 * The time it takes between an AI chip being activated and the apocalypse happening (in ticks)
	 */
	public static int apocalypseCountdownLength = 469;
	public static int SURVIVOR_RARITY = 250;
	public static int WANDERING_SURVIVOR_RARITY = 500;
	public static int SKELETON_RARITY = 50;
	public static int DEAD_TREE_RARITY = 100;
	public static int VEHICLE_RARITY = 2000;
	public static int AIRPORT_RARITY = 125;
	public static int DYE_FACTORY_RARITY = 400;
	public static int LAB_RARITY = 100;

	
	// TODO: Configify
	public static int ABANDONED_PORTAL_APOC_RARITY = 4000;
	public static int ABANDONED_PORTAL_OVERWORLD_RARITY = 4000;
	
	/**
	 * The distance between where the player left the overworld, and where they return
	 */
	public static int RETURN_RADIUS = 100;
	/**
	 * How far from their death point does the player respawn?
	 */
	public static int SPAWN_RADIUS = 100;
	public static boolean RESPAWN_IN_APOC = false;
	/**
	 * Who gets teleported to the apocalypse when a player places a mecha?
	 */
	public static TeleportOption OPTION = TeleportOption.PLACER_ONLY;
	
	public static DimensionType APOCALYPSE_DIM = null;
	public static final ResourceKey<DimensionType> APOCALYPSE_DIM_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(MODID, "apocalypse"));
	public static final ResourceKey<Level> APOCALYPSE_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(MODID, "apocalypse"));
	public static final ResourceKey<LevelStem> APOCALYPSE_LEVEL_STEM_KEY = ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(MODID, "apocalypse"));
	public static FlansModLootGenerator lootGenerator;
	
	//Custom apoclypse defined items and blocks
	public static Item sulphur;
	public static Block blockSulphur;
	public static Block blockSulphuricAcid;
	public static Block blockLabStone;
	public static Block blockPowerCube;
	
	public static Item itemBlockPowerCube, itemBlockLabStone, itemBlockSulphur;
	
	public static CreativeTabFlan tabApocalypse = new CreativeTabFlan(5);
	
	//References to apocalypse specific items and blocks:
	public static BlockItemHolder skeleton, slumpedSkeleton, gunRack;
	
	public static ItemGlove nukraniumGauntlet;
	
	public static BlockEntityType<TileEntityPowerCube> POWER_CUBE_BE;
	
	private static <T extends Block> T registerBlock(String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory)
	{
		Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
		ResourceKey<Block> key = ResourceKey.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK.key(), id);
		T block = factory.apply(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(key));
		return Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK, key, block);
	}
	
	private static Item registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, Item> factory)
	{
		Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
		ResourceKey<Item> key = ResourceKey.create(net.minecraft.core.registries.BuiltInRegistries.ITEM.key(), id);
		Item item = factory.apply(new Item.Properties().setId(key));
		return Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, key, item);
	}
	
	@Override
	public void onInitialize()
	{
		INSTANCE = this;
		
		//Load config
		configFile = new Configuration(new java.io.File(FlansMod.gameDirectory, "config/flansmodapocalypse.cfg"));
		syncConfig();
		
		//Custom apoclypse defined items and blocks
		
		//Sulphur block and item
		blockSulphur = registerBlock("blocksulphur", p -> new BlockSulphur(p.mapColor(MapColor.SAND).sound(net.minecraft.world.level.block.SoundType.SAND).strength(0.5F)));
		sulphur = registerItem("flansulphur", Item::new);
		
		itemBlockSulphur = registerItem("blocksulphur", p -> new BlockItem(blockSulphur, p));
		
		//Sulphuric acid
		// TODO APOCALYPSE: 1.12.2 was a Forge fluid; now a plain damaging block
		blockSulphuricAcid = registerBlock("blocksulphuricacid", p -> new BlockSulphuricAcid(p.mapColor(MapColor.COLOR_YELLOW).noCollision().strength(100.0F)));
		
		//Laboratory Stone
		blockLabStone = registerBlock("blocklabstone", p -> new BlockStatic(p.mapColor(MapColor.STONE).strength(3F, 5F)));
		itemBlockLabStone = registerItem("blocklabstone", p -> new BlockItem(blockLabStone, p));
		
		//Power Cube
		blockPowerCube = registerBlock("blockpowercube", p -> new BlockPowerCube(p.noOcclusion().strength(3F, 5F)));
		itemBlockPowerCube = registerItem("blockpowercube", p -> new BlockItem(blockPowerCube, p));
		
		POWER_CUBE_BE = Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "powercube"),
				net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(TileEntityPowerCube::new, blockPowerCube).build());
		
		//Biomes
		BiomeApocalypse.registerBiomes();
		
		//Entities (registered in ModEntities.init() by the main FlansMod entrypoint)
		FabricDefaultAttributeRegistry.register(ModEntities.SURVIVOR, EntityFlansModShooter.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.FLANSMOD_SHOOTER, EntityFlansModShooter.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKULLER, EntitySkuller.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKULL_DRONE, EntitySkullDrone.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SKULL_BOSS, EntitySkullBoss.createAttributes());
		
		//Set up proxy
		proxy = FlansMod.isClient() ? new com.flansmod.apocalypse.client.ClientProxyApocalypse() : new CommonProxyApocalypse();
		proxy.preInit();
		proxy.init();
		
		//Grab references to apocalypse specific items and blocks here:
		if(ItemHolderType.getItemHolder("flanSkeleton") != null)
		{
			skeleton = ItemHolderType.getItemHolder("flanSkeleton").block;
		}
		else
		{
			FlansMod.log.warn("Could not find skeleton item holder!");
		}
		if(ItemHolderType.getItemHolder("flanSkeleton2") != null)
		{
			slumpedSkeleton = ItemHolderType.getItemHolder("flanSkeleton2").block;
		}
		else
		{
			FlansMod.log.warn("Could not find skeleton2 item holder!");
		}
		if(ItemHolderType.getItemHolder("flanGunRack") != null)
		{
			gunRack = ItemHolderType.getItemHolder("flanGunRack").block;
		}
		else
		{
			FlansMod.log.warn("Could not find gun rack item holder!");
		}
		
		//Put ai chip in apocalypse tab
		if(PartType.getPart("aiChip") != null)
			tabApocalypse.addItem(PartType.getPart("aiChip").item);
		if(PartType.getPart("complicatedCircuit") != null)
			tabApocalypse.addItem(PartType.getPart("complicatedCircuit").item);
		if(PartType.getPart("nuclearPowerCore") != null)
			tabApocalypse.addItem(PartType.getPart("nuclearPowerCore").item);
		
		if(GloveType.getGlove("nukranium_gauntlet") != null)
		{
			nukraniumGauntlet = (ItemGlove)GloveType.getGlove("nukranium_gauntlet").item;
		}
		
		lootGenerator = new FlansModLootGenerator();
		
		ServerTickEvents.END_SERVER_TICK.register(server -> proxy.tick());
		ServerLifecycleEvents.SERVER_STARTED.register(this::registerDimension);
	}
	
	private void registerDimension(MinecraftServer server)
	{
		try
		{
			DimensionType overworldType = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD).value();
			// TODO APOCALYPSE: the 1.12.2 WorldProviderApocalypse (custom sky/lightning/respawn rules) no longer exists;
			// the apocalypse dimension reuses the overworld's DimensionType settings
			APOCALYPSE_DIM = new DimensionType(
					overworldType.hasFixedTime(), overworldType.hasSkyLight(), overworldType.hasCeiling(), overworldType.hasEnderDragonFight(),
					overworldType.coordinateScale(), overworldType.minY(), overworldType.height(), overworldType.logicalHeight(),
					overworldType.infiniburn(), overworldType.ambientLight(), overworldType.monsterSettings(), overworldType.skybox(),
					overworldType.cardinalLightType(), overworldType.attributes(), overworldType.timelines(), overworldType.defaultClock());
			Registry.register(server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE), APOCALYPSE_DIM_TYPE_KEY, APOCALYPSE_DIM);
			
			long seed = server.overworld() != null ? server.overworld().getSeed() : 0L;
			ChunkProviderApocalypse chunkGenerator = new ChunkProviderApocalypse(new BiomeProviderApocalypse(seed), seed);
			Registry.register(server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM), APOCALYPSE_LEVEL_STEM_KEY, new LevelStem(Holder.direct(APOCALYPSE_DIM), chunkGenerator));
		}
		catch(Exception e)
		{
			// TODO APOCALYPSE: the dynamic registries are frozen after datapack load, so the apocalypse
			// dimension cannot currently be created at runtime; only the dimension type holder is available
			FlansMod.log.error("Failed to register the apocalypse dimension", e);
		}
	}
	
	public static FlansModLootGenerator getLootGenerator()
	{
		return lootGenerator;
	}
	
	public static void syncConfig()
	{
		apocalypseCountdownLength = configFile.getInt("Apocalypse Countdown Length", Configuration.CATEGORY_GENERAL, apocalypseCountdownLength, 19, Integer.MAX_VALUE, "Time between placing an AI mecha and going to the apocalypse");
		SURVIVOR_RARITY = configFile.getInt("Survivor Rarity", Configuration.CATEGORY_GENERAL, SURVIVOR_RARITY, 1, Integer.MAX_VALUE, "Rarity of survivor entities spawned during world creation");
		WANDERING_SURVIVOR_RARITY = configFile.getInt("Wandering Survivor Rarity", Configuration.CATEGORY_GENERAL, WANDERING_SURVIVOR_RARITY, 1, Integer.MAX_VALUE, "Rarity of survivor entities spawned at night");
		SKELETON_RARITY = configFile.getInt("Skeleton Rarity", Configuration.CATEGORY_GENERAL, SKELETON_RARITY, 1, Integer.MAX_VALUE, "Rarity of buried skeletons");
		DEAD_TREE_RARITY = configFile.getInt("Dead Tree Rarity", Configuration.CATEGORY_GENERAL, DEAD_TREE_RARITY, 1, Integer.MAX_VALUE, "Rarity of dead trees");
		VEHICLE_RARITY = configFile.getInt("Vehicle Rarity", Configuration.CATEGORY_GENERAL, VEHICLE_RARITY, 1, Integer.MAX_VALUE, "Rarity of broken vehicles");
		AIRPORT_RARITY = configFile.getInt("Airport Rarity", Configuration.CATEGORY_GENERAL, AIRPORT_RARITY, 1, Integer.MAX_VALUE, "Rarity of airstrips");
		DYE_FACTORY_RARITY = configFile.getInt("Dye Factory Rarity", Configuration.CATEGORY_GENERAL, DYE_FACTORY_RARITY, 1, Integer.MAX_VALUE, "Rarity of dye factories");
		LAB_RARITY = configFile.getInt("Lab Rarity", Configuration.CATEGORY_GENERAL, LAB_RARITY, 1, Integer.MAX_VALUE, "Rarity of the research lab");
		RETURN_RADIUS = configFile.getInt("Return Radius", Configuration.CATEGORY_GENERAL, RETURN_RADIUS, 1, Integer.MAX_VALUE, "The distance away from your initial AI mecha that your return portal appears");
		SPAWN_RADIUS = configFile.getInt("Spawn Radius", Configuration.CATEGORY_GENERAL, SPAWN_RADIUS, 1, Integer.MAX_VALUE, "The distance from your deathpoint that you respawn in the apocalypse");
		OPTION = TeleportOption.getOption(configFile.getString("Option", Configuration.CATEGORY_GENERAL, OPTION.toString()));
		
		ABANDONED_PORTAL_APOC_RARITY = configFile.getInt("Abandoned Portal Rarity (Apocalypse)", Configuration.CATEGORY_GENERAL, ABANDONED_PORTAL_APOC_RARITY, 1, Integer.MAX_VALUE, "Rarity of the abandoned portal structures in the apocalypse");
		ABANDONED_PORTAL_OVERWORLD_RARITY = configFile.getInt("Abandoned Portal Rarity (Other Dimensions)", Configuration.CATEGORY_GENERAL, ABANDONED_PORTAL_OVERWORLD_RARITY, 1, Integer.MAX_VALUE, "Rarity of the abandoned portal structures in other dimensions");
		RESPAWN_IN_APOC = configFile.getBoolean("Respawn in Apocalypse", Configuration.CATEGORY_GENERAL, RESPAWN_IN_APOC);
		
		if(configFile.hasChanged())
			configFile.save();
	}
	
	public enum TeleportOption
	{
		PLACER_ONLY, DIM, DIM_OPT_IN, NEARBY, NEARBY_OPT_IN;
		
		public static TeleportOption getOption(String s)
		{
			if(s.equals("PLACER_ONLY"))
				return PLACER_ONLY;
			else if(s.equals("DIM"))
				return DIM;
			else if(s.equals("DIM_OPT_IN"))
				return DIM_OPT_IN;
			else if(s.equals("NEARBY"))
				return NEARBY;
			else if(s.equals("NEARBY_OPT_IN"))
				return NEARBY_OPT_IN;
			return PLACER_ONLY;
		}
	}

	@Override
	public String GetContentFolder() 
	{
		return "Apocalypse";
	}

	@Override
	public void RegisterModelRedirects() 
	{
		FlansMod.RegisterModelRedirect("apocalypse", "com.flansmod.apocalypse.client.model");
	}

	// Boss fight server control
	
	private static final int kBossWarmupTicks = 200;
	
	
	private static int sElapsedTicks = 0;
	private static boolean sBossFightInProgress = false;
	private static EntitySkullBoss sTheBoss = null;
	
	public void TriggerBossFight(Level world, LivingEntity placer) 
	{
		sElapsedTicks = 0;
		
		if(world.isClientSide()) {
			return;
		}
		
		sTheBoss = new EntitySkullBoss(world);
		sTheBoss.setPos(0d, WorldGenBossPillar.kBossSpawnHeight, 0d);
		sTheBoss.SetTarget(placer);
		((net.minecraft.server.level.ServerLevel)world).addFreshEntity(sTheBoss);
		
	}
	
	public void UpdateBossFight(Level world)
	{
		sElapsedTicks++;
		
		if(sElapsedTicks >= kBossWarmupTicks)
		{
			
		}
	}
}
