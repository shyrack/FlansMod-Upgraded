package com.flansmod.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.EntityWheel;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.enchantments.EnchantmentModule;
import com.flansmod.common.eventhandlers.PlayerDeathEventListener;
import com.flansmod.common.guns.AAGunType;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.EntityAAGun;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.EntityMG;
import com.flansmod.common.guns.GrenadeType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemAAGun;
import com.flansmod.common.guns.ItemAttachment;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGrenade;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.network.PacketHandler;
import com.flansmod.common.paintjob.BlockPaintjobTable;
import com.flansmod.common.paintjob.TileEntityPaintjobTable;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.BlockSpawner;
import com.flansmod.common.teams.CommandTeams;
import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;
import com.flansmod.common.teams.EntityGunItem;
import com.flansmod.common.teams.EntityTeamItem;
import com.flansmod.common.teams.ItemFlagpole;
import com.flansmod.common.teams.ItemOpStick;
import com.flansmod.common.teams.ItemRewardBox;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.teams.LoadoutPool;
import com.flansmod.common.teams.PlayerClass;
import com.flansmod.common.teams.RewardBox;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.teams.TeamsManagerRanked;
import com.flansmod.common.teams.TileEntitySpawner;
import com.flansmod.common.tools.EntityParachute;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.tools.ToolType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

public class FlansMod implements ModInitializer
{
	//Core mod stuff
	public static Logger log;
	public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("flansmod");
	public static boolean DEBUG = false;
	public static Configuration configFile;
	public static final String MOD_ID = "flansmod";
	public static final String MODID = MOD_ID;
	public static final String VERSION = "5.10.0";
	public static FlansMod INSTANCE;
	public static File gameDirectory;
	public static net.minecraft.server.MinecraftServer serverInstance;

	public static CommonProxy proxy;

	//A standardised ticker for all bits of the mod to call upon if they need one
	public static int ticker = 0;
	public static long lastTime;
	public static File flanDir, modDir;
	public static final float soundRange = 50F;
	public static final float driveableUpdateRange = 200F;
	public static final int numPlayerSnapshots = 20;
	public static boolean isApocalypseLoaded = false;
	public static boolean addAllPaintjobsToCreative = false;
	public static boolean addGunpowderRecipe = true;
	public static boolean shootOnRightClick = false;
	public static boolean forceUpdateJSONs = false;
	public static boolean enchantmentModuleEnabled = true;

	public static float armourSpawnRate = 0.25F;

	public static int dungeonLootChance = 500;

	/**
	 * The spectator team. Moved here to avoid a concurrent modification error
	 */
	public static Team spectators = new Team("spectators", "Spectators", 0x404040, '7');

	//Handlers
	public static final PacketHandler packetHandler = new PacketHandler();
	public static final PlayerHandler playerHandler = new PlayerHandler();
	public static final TeamsManager teamsManager = new TeamsManagerRanked();
	public static final CommonTickHandler tickHandler = new CommonTickHandler();
	public static FlansHooks hooks = new FlansHooks();
	public static final ContentManager contentManager = new ContentManager();
	public static final EnchantmentModule enchantmentModule = new EnchantmentModule();
	public static HashMap<String, String> modelDirectories = new HashMap<>();

	//Items and creative tabs
	public static BlockFlansWorkbench workbench;
	public static ItemBlockManyNames workbenchItem;
	public static Item gunpowderBlockItem;
	public static BlockSpawner spawner;
	public static Block gunpowderBlock;
	public static ItemBlockManyNames spawnerItem;
	public static ItemOpStick opStick;
	public static ItemFlagpole flag;
	public static Item crosshairsymbol;
	public static ArrayList<ItemPart> partItems = new ArrayList<>();
	public static ArrayList<ItemMecha> mechaItems = new ArrayList<>();
	public static ArrayList<ItemTool> toolItems = new ArrayList<>();
	public static ArrayList<ItemTeamArmour> armourItems = new ArrayList<>();
	public static CreativeTabFlan tabFlanGuns, tabFlanDriveables, tabFlanParts, tabFlanTeams, tabFlanMechas;
	public static CreativeModeTab[] tabs = new CreativeModeTab[5];

	/**
	 * Custom paintjob item
	 */
	public static Item rainbowPaintcan;
	public static BlockPaintjobTable paintjobTable;

	static
	{
		log = LOGGER;
	}

	public static boolean isClient()
	{
		return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
	}

	@Override
	public void onInitialize()
	{
		INSTANCE = this;
		log.info("Initialising Flan's Mod.");

		gameDirectory = FabricLoader.getInstance().getGameDir().toFile();
		configFile = new Configuration(new File(gameDirectory, "config/flansmod.cfg"));
		syncConfig();

		//Set up directories
		modDir = new File(gameDirectory, "/mods/");
		flanDir = new File(gameDirectory, "/Flan/");

		if(!flanDir.exists())
		{
			log.info("Flan folder not found. Creating empty folder.");
			flanDir.mkdirs();
		}

		//Set up creative tabs
		ModItemGroups.register();

		//Set up mod blocks and items
		ModBlockEntities.register();
		ModEntities.init();
		ModItems.register();

		//Populate the creative tabs with the static items
		tabFlanGuns.addItem(ModItems.opStickItem);
		tabFlanGuns.addItem(ModItems.flagpoleItem);
		tabFlanGuns.addItem(FlansMod.rainbowPaintcan);
		tabFlanGuns.addItem(ModItems.paintcanItem);

		//Set up proxy
		proxy = isClient() ? new com.flansmod.client.ClientProxy() : new CommonProxy();
		proxy.preInit();

		//Bind vanilla item data components so that ItemStacks can be created
		//during content pack parsing
		bindItemComponents();

		//Read content packs
		contentManager.FindContentInFlanFolder();
		contentManager.LoadAssetsFromFlanFolder();
		contentManager.RegisterModelRedirects();
		loadContentPacks();

		//Bind data components of the content pack items
		bindItemComponents();

		//Initialising handlers
		packetHandler.initialise();
		proxy.init();

		if(enchantmentModuleEnabled)
			enchantmentModule.Init();

		//Start the EventListener
		new PlayerDeathEventListener();

		//Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				CommandTeams.register(dispatcher));

		//Server tick
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> serverInstance = server);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> serverInstance = null);
		ServerTickEvents.END_SERVER_TICK.register(server -> CommonTickHandler.serverTick(server));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playerHandler.playerLoggedIn(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerHandler.playerLoggedOut(handler.getPlayer()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> playerHandler.onLivingDeath(entity, damageSource));

		//Loot
		LootTableEvents.MODIFY.register((key, builder, source, provider) -> registerLoot(key, builder));

		//Mob armour spawn
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {});

		log.info("Loading complete.");
	}

	public static void bindItemComponents()
	{
		try
		{
			net.minecraft.core.component.DataComponentInitializers initializers =
					net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS;
			net.minecraft.core.HolderLookup.Provider base =
					net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
			net.minecraft.core.HolderLookup.Provider provider = new net.minecraft.core.HolderLookup.Provider()
			{
				@Override
				public java.util.stream.Stream<net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>>> listRegistryKeys()
				{
					return base.listRegistryKeys();
				}

				@Override
				public <T> java.util.Optional<? extends net.minecraft.core.HolderLookup.RegistryLookup<T>> lookup(
						net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> key)
				{
					return base.lookup(key);
				}

				@Override
				public <T> java.util.Optional<net.minecraft.core.HolderSet.Named<T>> get(net.minecraft.tags.TagKey<T> key)
				{
					return java.util.Optional.empty();
				}

				@Override
				public <T> net.minecraft.core.HolderSet.Named<T> getOrThrow(net.minecraft.tags.TagKey<T> key)
				{
					net.minecraft.core.HolderOwner<T> owner =
							(net.minecraft.core.HolderOwner<T>)net.minecraft.core.registries.BuiltInRegistries.REGISTRY.getValue(key.registry().identifier());
					return net.minecraft.core.HolderSet.emptyNamed(owner, key);
				}

				@Override
				@SuppressWarnings({"unchecked", "rawtypes"})
				public <T> net.minecraft.core.Holder.Reference<T> getOrThrow(net.minecraft.resources.ResourceKey<T> key)
				{
					java.util.Optional<? extends net.minecraft.core.HolderLookup.RegistryLookup<T>> registryLookup =
							(java.util.Optional)(Object)base.lookup(net.minecraft.resources.ResourceKey.createRegistryKey(key.registry()));
					if(registryLookup.isPresent())
					{
						java.util.Optional<net.minecraft.core.Holder.Reference<T>> ref = registryLookup.get().get(key);
						if(ref.isPresent())
							return ref.get();
					}
					net.minecraft.core.HolderOwner<T> owner =
							(net.minecraft.core.HolderOwner<T>)(Object)net.minecraft.core.registries.BuiltInRegistries.REGISTRY.getValue(key.registry());
					return net.minecraft.core.Holder.Reference.createStandAlone(owner, key);
				}
			};
			for(net.minecraft.core.component.DataComponentInitializers.PendingComponents<?> pending : initializers.build(provider))
			{
				pending.apply();
			}
		}
		catch(Exception e)
		{
			log.error("Failed to bind item data components", e);
		}
	}

	private static boolean contentPacksLoaded = false;

	public static void loadContentPacks()
	{
		if(contentPacksLoaded)
			return;
		contentPacksLoaded = true;
		contentManager.LoadTypes();
		contentManager.CreateItems();
		Team.spectators = spectators;

		//Automates JSON adding for old content packs
		proxy.addMissingJSONs(InfoType.infoTypes);
	}

	public static void registerLoot(net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> key,
									LootTable.Builder builder)
	{
		boolean addBasicLoot = key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.ABANDONED_MINESHAFT)
				|| key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.VILLAGE_WEAPONSMITH)
				|| key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.END_CITY_TREASURE)
				|| key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.NETHER_BRIDGE)
				|| key.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_PYRAMID);
		if(addBasicLoot && gunpowderBlockItem != null && workbenchItem != null)
		{
			LootPool.Builder pool = LootPool.lootPool()
					.setRolls(UniformGenerator.between(1, 3))
					.setBonusRolls(UniformGenerator.between(1, 2))
					.add(LootItem.lootTableItem(gunpowderBlockItem).setWeight(8)
							.apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 6))))
					.add(LootItem.lootTableItem(workbenchItem).setWeight(1))
					.add(LootItem.lootTableItem(Blocks.IRON_BLOCK.asItem()).setWeight(4)
							.apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))));
			builder.withPool(pool);
		}
	}

	/**
	 * Reads type files from all content packs
	 */
	public static void RegisterModelRedirect(String key, String redirect)
	{
		modelDirectories.put(key, redirect);
	}

	public static PacketHandler getPacketHandler()
	{
		return INSTANCE.packetHandler;
	}

	public static void syncConfig()
	{
		addGunpowderRecipe = configFile.getBoolean("Gunpowder Recipe", Configuration.CATEGORY_GENERAL, addGunpowderRecipe);
		shootOnRightClick = configFile.getBoolean("ShootOnRightClick", Configuration.CATEGORY_GENERAL, shootOnRightClick);
		addAllPaintjobsToCreative = configFile.getBoolean("Add All Paintjobs to Creative", Configuration.CATEGORY_GENERAL, addAllPaintjobsToCreative);
		forceUpdateJSONs = configFile.getBoolean("ForceUpdateJSONs", Configuration.CATEGORY_GENERAL, forceUpdateJSONs);
		enchantmentModuleEnabled = configFile.getBoolean("EnchantmentModuleEnabled", Configuration.CATEGORY_GENERAL, enchantmentModuleEnabled);

		if(configFile.hasChanged())
			configFile.save();
	}

	public static void Assert(boolean b, String string)
	{
		if(!b)
		{
			log.warn(string);
		}
	}

	public static net.minecraft.core.particles.ParticleOptions getParticleType(String s)
	{
		if(s.equals("hugeexplosion")) return ParticleTypes.EXPLOSION_EMITTER;
		else if(s.equals("largeexplode")) return ParticleTypes.EXPLOSION_EMITTER;
		else if(s.equals("explode")) return ParticleTypes.EXPLOSION_EMITTER;
		else if(s.equals("fireworksSpark")) return ParticleTypes.FIREWORK;
		else if(s.equals("bubble")) return ParticleTypes.BUBBLE;
		else if(s.equals("splash")) return ParticleTypes.BUBBLE_POP;
		else if(s.equals("wake")) return ParticleTypes.FISHING;
		else if(s.equals("drop")) return ParticleTypes.DRIPPING_WATER;
		else if(s.equals("suspended")) return ParticleTypes.CLOUD;
		else if(s.equals("depthsuspend")) return ParticleTypes.CLOUD;
		else if(s.equals("townaura")) return ParticleTypes.END_ROD;
		else if(s.equals("crit")) return ParticleTypes.CRIT;
		else if(s.equals("magicCrit")) return ParticleTypes.ENCHANTED_HIT;
		else if(s.equals("smoke")) return ParticleTypes.SMOKE;
		else if(s.equals("largesmoke")) return ParticleTypes.SMOKE;
		else if(s.equals("spell")) return ParticleTypes.ENCHANT;
		else if(s.equals("instantSpell")) return ParticleTypes.ENCHANT;
		else if(s.equals("mobSpell")) return ParticleTypes.ENCHANTED_HIT;
		else if(s.equals("mobSpellAmbient")) return ParticleTypes.ENCHANTED_HIT;
		else if(s.equals("witchMagic")) return ParticleTypes.WITCH;
		else if(s.equals("dripWater")) return ParticleTypes.DRIPPING_WATER;
		else if(s.equals("dripLava")) return ParticleTypes.DRIPPING_LAVA;
		else if(s.equals("angryVillager")) return ParticleTypes.ANGRY_VILLAGER;
		else if(s.equals("happyVillager")) return ParticleTypes.HAPPY_VILLAGER;
		else if(s.equals("note")) return ParticleTypes.NOTE;
		else if(s.equals("portal")) return ParticleTypes.PORTAL;
		else if(s.equals("enchantmenttable")) return ParticleTypes.ENCHANT;
		else if(s.equals("flame")) return ParticleTypes.FLAME;
		else if(s.equals("lava")) return ParticleTypes.LAVA;
		else if(s.equals("footstep")) return ParticleTypes.CLOUD;
		else if(s.equals("cloud")) return ParticleTypes.CLOUD;
		else if(s.equals("reddust")) return ParticleTypes.SMOKE;
		else if(s.equals("snowballpoof")) return ParticleTypes.ITEM_SNOWBALL;
		else if(s.equals("snowshovel")) return ParticleTypes.ITEM_SNOWBALL;
		else if(s.equals("slime")) return ParticleTypes.ITEM_SLIME;
		else if(s.equals("heart")) return ParticleTypes.HEART;
		else if(s.equals("barrier")) return ParticleTypes.ITEM_COBWEB;

		return ParticleTypes.BUBBLE;
	}
}
