package com.flansmod.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import com.flansmod.client.debug.EntityDebugAABB;
import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.client.debug.RenderDebugAABB;
import com.flansmod.client.debug.RenderDebugDot;
import com.flansmod.client.debug.RenderDebugVector;
import com.flansmod.client.gui.GuiArmourBox;
import com.flansmod.client.gui.GuiDriveableCrafting;
import com.flansmod.client.gui.GuiDriveableFuel;
import com.flansmod.client.gui.GuiDriveableInventory;
import com.flansmod.client.gui.GuiDriveableMenu;
import com.flansmod.client.gui.GuiDriveableRepair;
import com.flansmod.client.gui.GuiGunBox;
import com.flansmod.client.gui.GuiGunModTable;
import com.flansmod.client.gui.GuiMechaInventory;
import com.flansmod.client.gui.GuiPaintjobTable;
import com.flansmod.client.handlers.ClientEventHandler;
import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.client.handlers.KeyInputHandler;
import com.flansmod.client.model.RenderAAGun;
import com.flansmod.client.model.RenderBullet;
import com.flansmod.client.model.RenderFlag;
import com.flansmod.client.model.RenderFlagpole;
import com.flansmod.client.model.RenderGrenade;
import com.flansmod.client.model.RenderGun;
import com.flansmod.client.model.RenderGunItem;
import com.flansmod.client.model.RenderItemHolder;
import com.flansmod.client.model.RenderMG;
import com.flansmod.client.model.RenderMecha;
import com.flansmod.client.model.RenderNull;
import com.flansmod.client.model.RenderParachute;
import com.flansmod.client.model.RenderPlane;
import com.flansmod.client.model.RenderVehicle;
import com.flansmod.common.CommonProxy;
import com.flansmod.common.EntityItemCustomRender;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.TileEntityItemHolder;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntitySeat;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.EntityWheel;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.EntityAAGun;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.EntityMG;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.network.PacketBuyArmour;
import com.flansmod.common.network.PacketBuyWeapon;
import com.flansmod.common.network.PacketCraftDriveable;
import com.flansmod.common.network.PacketRepairDriveable;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.paintjob.TileEntityPaintjobTable;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;
import com.flansmod.common.teams.TileEntitySpawner;
import com.flansmod.common.tools.EntityParachute;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.vector.Vector3f;

public class ClientProxy extends CommonProxy
{
	public static String modelDir = "com.flansmod.client.model.";

	/* These renderers handle rendering in hand items */
	public static RenderGun gunRenderer;

	public List<SoundEvent> eventsToRegister = new ArrayList<>();

	private FlansModClient flansModClient;

	@Override
	public void preInit()
	{
	}

	@Override
	public void init()
	{
		flansModClient = new FlansModClient();
		flansModClient.load();

		ClientEventHandler eventHandler = new ClientEventHandler();

		//Client side driveable interaction raycasts
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
		{
			playerClickAttack(player, entity);
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
		{
			playerClickInteract(player, entity, hand);
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
		{
			playerClickBlock(player, hitResult.getBlockPos());
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) ->
		{
			playerClickItem(player);
			return InteractionResult.PASS;
		});
	}

	public void playerClickAttack(Player player, Entity target)
	{
		Vec3 eye = player.getEyePosition(0F);
		Vec3 look = player.getViewVector(1.0F);
		double interactDistance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE).getValue();
		look = look.normalize().scale(interactDistance);

		for(Entity entity : player.level().getEntitiesOfClass(EntityDriveable.class, player.getBoundingBox().inflate(8D)))
		{
			if(entity instanceof EntityDriveable)
			{
				EntityDriveable d = (EntityDriveable)entity;
				Vec3 L = entity.position().subtract(eye);
				double tca = L.dot(look);
				if(tca < 0)
					continue;
				double d2 = L.dot(L) - tca * tca;
				if(d2 > d.getDriveableType().hitboxRadius)
					continue;
				DriveablePart partHit = d.raytraceParts(new Vector3f((float)eye.x, (float)eye.y, (float)eye.z), new Vector3f((float)look.x, (float)look.y, (float)look.z));
				Minecraft.getInstance().gameMode.attack(player, d);
			}
		}
	}

	public void playerClickInteract(Player player, Entity target, InteractionHand hand)
	{
		Vec3 eye = player.getEyePosition(0F);
		Vec3 look = player.getViewVector(1.0F);
		double interactDistance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE).getValue();
		look = look.normalize().scale(interactDistance);

		for(Entity entity : player.level().getEntitiesOfClass(EntityDriveable.class, player.getBoundingBox().inflate(8D)))
		{
			if(entity instanceof EntityDriveable)
			{
				EntityDriveable d = (EntityDriveable)entity;
				Vec3 L = entity.position().subtract(eye);
				double tca = L.dot(look);
				if(tca < 0)
					continue;
				double d2 = L.dot(L) - tca * tca;
				if(d2 > d.getDriveableType().hitboxRadius)
					continue;
				DriveablePart partHit = d.raytraceParts(new Vector3f((float)eye.x, (float)eye.y, (float)eye.z), new Vector3f((float)look.x, (float)look.y, (float)look.z));
				Minecraft.getInstance().gameMode.interact(player, d, new net.minecraft.world.phys.EntityHitResult(d), hand);
			}
		}
	}

	public void playerClickBlock(Player player, BlockPos pos)
	{
		Vec3 eye = player.getEyePosition(0F);
		Vec3 look = player.getViewVector(1.0F);
		double interactDistance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE).getValue();
		look = look.normalize().scale(interactDistance);

		for(Entity entity : player.level().getEntitiesOfClass(EntityDriveable.class, player.getBoundingBox().inflate(8D)))
		{
			if(entity instanceof EntityDriveable)
			{
				EntityDriveable d = (EntityDriveable)entity;
				Vec3 L = entity.position().subtract(eye);
				double tca = L.dot(look);
				if(tca < 0)
					continue;
				double d2 = L.dot(L) - tca * tca;
				if(d2 > d.getDriveableType().hitboxRadius)
					continue;
				d.raytraceParts(new Vector3f((float)eye.x, (float)eye.y, (float)eye.z), new Vector3f((float)look.x, (float)look.y, (float)look.z));
				Minecraft.getInstance().gameMode.attack(player, d);
			}
		}
	}

	public void playerClickItem(Player player)
	{
		Vec3 eye = player.getEyePosition(0F);
		Vec3 look = player.getViewVector(1.0F);
		double interactDistance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE).getValue();
		look = look.normalize().scale(interactDistance);

		for(Entity entity : player.level().getEntitiesOfClass(EntityDriveable.class, player.getBoundingBox().inflate(8D)))
		{
			if(entity instanceof EntityDriveable)
			{
				EntityDriveable d = (EntityDriveable)entity;
				Vec3 L = entity.position().subtract(eye);
				double tca = L.dot(look);
				if(tca < 0)
					continue;
				double d2 = L.dot(L) - tca * tca;
				if(d2 > d.getDriveableType().hitboxRadius)
					continue;
				d.raytraceParts(new Vector3f((float)eye.x, (float)eye.y, (float)eye.z), new Vector3f((float)look.x, (float)look.y, (float)look.z));
				Minecraft.getInstance().gameMode.interact(player, d, new net.minecraft.world.phys.EntityHitResult(d), net.minecraft.world.InteractionHand.MAIN_HAND);
			}
		}
	}

	@Override
	public void registerSoundEvents()
	{
		for(SoundEvent sound : eventsToRegister)
		{
			Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
			if(id != null && BuiltInRegistries.SOUND_EVENT.get(id) == null && !BuiltInRegistries.SOUND_EVENT.containsKey(id))
				Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound);
		}
		SoundEvent bulletFlyby = FlansModResourceHandler.getSoundEvent("bulletflyby");
		if(bulletFlyby != null && !BuiltInRegistries.SOUND_EVENT.containsKey(BuiltInRegistries.SOUND_EVENT.getKey(bulletFlyby)))
			Registry.register(BuiltInRegistries.SOUND_EVENT, BuiltInRegistries.SOUND_EVENT.getKey(bulletFlyby), bulletFlyby);
	}

	/**
	 * This method reloads all textures from all mods and resource packs. It forces Minecraft to read images from the content packs added after mod init
	 */
	@Override
	public void forceReload()
	{
		Minecraft.getInstance().reloadResourcePacks();
	}

	/**
	 * This method grabs all the content packs and puts them in a list. The client side part registers them as resource packs
	 */
	@Override
	public void LoadAssetsFromFlanFolder()
	{
		PackRepository repo = Minecraft.getInstance().getResourcePackRepository();
		java.util.Set<net.minecraft.server.packs.repository.RepositorySource> sources = null;
		try
		{
			java.lang.reflect.Field f = PackRepository.class.getDeclaredField("sources");
			f.setAccessible(true);
			sources = (java.util.Set<net.minecraft.server.packs.repository.RepositorySource>)f.get(repo);
		}
		catch(Exception e)
		{
			FlansMod.log.error("Failed to access pack repository sources", e);
		}
		if(sources != null)
		{
			sources.add(new FolderRepositorySource(FlansMod.flanDir.toPath(), PackType.CLIENT_RESOURCES, PackSource.BUILT_IN,
					new DirectoryValidator(path -> true)));
		}
		FlansMod.log.info("Loaded textures and models.");
	}

	/**
	 * Register entity renderers
	 */
	@Override
	public void registerRenderers()
	{
		FlansMod.log.info("Registering Renderers");

		EntityRendererRegistry.register(com.flansmod.common.ModEntities.BULLET, RenderBullet::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.GRENADE, RenderGrenade::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.PLANE, RenderPlane::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.VEHICLE, RenderVehicle::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.AA_GUN, RenderAAGun::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.FLAGPOLE, RenderFlagpole::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.FLAG, RenderFlag::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.SEAT, RenderNull::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.WHEEL, RenderNull::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.MG, RenderMG::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.PARACHUTE, RenderParachute::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.DEBUG_DOT, RenderDebugDot::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.DEBUG_VECTOR, RenderDebugVector::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.DEBUG_AABB, RenderDebugAABB::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.MECHA, RenderMecha::new);
		EntityRendererRegistry.register(com.flansmod.common.ModEntities.CUSTOM_ITEM, RenderGunItem::new);
	}

	/**
	 * Old one time tutorial code that displays messages the first time you enter a plane / vehicle. Needs reworking
	 */
	@Override
	public void doTutorialStuff(Player player, EntityDriveable entityType)
	{
		if(!FlansModClient.doneTutorial)
		{
			FlansModClient.doneTutorial = true;

			player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.vehicleMenuKey.getName()).getString() + " to open the menu"));
			player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(Minecraft.getInstance().options.keyShift.getName()).getString() + " to get out"));
			player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.controlSwitchKey.getName()).getString() + " to switch controls"));
			player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.modeKey.getName()).getString() + " to switch VTOL mode"));
			if(entityType instanceof EntityPlane)
			{
				if(PlaneType.getPlane(((EntityPlane)entityType).driveableType).hasGear)
					player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.gearKey.getName()).getString() + " to switch the gear"));
				if(PlaneType.getPlane(((EntityPlane)entityType).driveableType).hasDoor)
					player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.doorKey.getName()).getString() + " to switch the doors"));
				if(PlaneType.getPlane(((EntityPlane)entityType).driveableType).hasWing)
					player.sendSystemMessage(Component.literal("Press " + net.minecraft.network.chat.Component.translatable(KeyInputHandler.modeKey.getName()).getString() + " to switch the wings"));
			}
		}
	}

	/**
	 * Adds the client side text message regarding mouse control mode switching
	 */
	@Override
	public void changeControlMode(Player player)
	{
		if(FlansModClient.flipControlMode())
			player.sendSystemMessage(Component.literal("Mouse Control mode is now set to " + FlansModClient.controlModeMouse));
	}

	/**
	 * Whether the player is in mouse control mode for planes. Now the default setting for planes, but it can be deactivated to look around while flying
	 */
	@Override
	public boolean mouseControlEnabled()
	{
		return FlansModClient.controlModeMouse;
	}

	/**
	 * Client GUI object getter
	 */
	@Override
	public Object getClientGui(int ID, Player player, Level world, int x, int y, int z)
	{
		//Null riding entity, don't open GUI in this case
		if(((ID >= 6 && ID <= 10) || ID == 12) && player.getVehicle() == null)
			return null;

		switch(ID)
		{
			case 0: return new GuiDriveableCrafting(player.getInventory());
			case 1: return new GuiDriveableRepair(player);
			case 2: return new GuiGunModTable(player.getInventory(), world);
			case 5: return new GuiGunBox(player.getInventory(), ((BlockGunBox)world.getBlockState(new BlockPos(x, y, z)).getBlock()).type);
			case 6: return new GuiDriveableInventory(player.getInventory(), world, ((EntitySeat)player.getVehicle()).driveable, 0);
			case 7: return new GuiDriveableInventory(player.getInventory(), world, ((EntitySeat)player.getVehicle()).driveable, 1);
			case 8: return new GuiDriveableFuel(player.getInventory(), world, ((EntitySeat)player.getVehicle()).driveable);
			case 9: return new GuiDriveableInventory(player.getInventory(), world, ((EntitySeat)player.getVehicle()).driveable, 2);
			case 10: return new GuiMechaInventory(player.getInventory(), world, (EntityMecha)((EntitySeat)player.getVehicle()).driveable);
			case 11: return new GuiArmourBox(player.getInventory(), ((BlockArmourBox)world.getBlockState(new BlockPos(x, y, z)).getBlock()).type);
			case 12: return new GuiDriveableInventory(player.getInventory(), world, ((EntitySeat)player.getVehicle()).driveable, 3);
			case 13: return new GuiPaintjobTable(player.getInventory(), world, (TileEntityPaintjobTable)world.getBlockEntity(new BlockPos(x, y, z)));
		}
		return null;
	}

	/**
	 * Called when the player presses the plane inventory key. Opens menu client side
	 */
	@Override
	public void openDriveableMenu(Player player, Level world, EntityDriveable driveable)
	{
		Minecraft.getInstance().setScreen(new GuiDriveableMenu(player.getInventory(), world, driveable));
	}

	/**
	 * Helper method that sorts out packages with model name input
	 * For example, the model class "com.flansmod.client.model.mw.ModelMP5"
	 * is referenced in the type file by the string "mw.MP5"
	 */
	private String getModelName(String in)
	{
		//Split about dots
		String[] split = in.split("\\.");
		//If there is no dot, our model class is in the default model package
		if(split.length == 1)
			return modelDir + "Model" + in;
		//Otherwise, we need to slightly rearrange the wording of the string for it to make sense
		else if(split.length > 1)
		{
			if(split.length == 2 && FlansMod.modelDirectories.containsKey(split[0]))
			{
				return FlansMod.modelDirectories.get(split[0]) + ".Model" + split[1];
			}
			else
			{
				String out = "Model" + split[split.length - 1];
				for(int i = split.length - 2; i >= 0; i--)
				{
					out = split[i] + "." + out;
				}
				return modelDir + out;
			}
		}
		return modelDir + in;
	}

	/**
	 * Generic model loader method for getting model classes and casting them to the required class type
	 */
	@Override
	public <T> T loadModel(String s, String shortName, Class<T> typeClass)
	{
		if(s == null || shortName == null)
			return null;
		try
		{
			return typeClass.cast(Class.forName(getModelName(s)).getDeclaredConstructor().newInstance());
		}
		catch(Exception e)
		{
			FlansMod.log.error("Failed to load model : " + shortName + " (" + s + ")");
			FlansMod.log.error(e.getMessage());
		}
		return null;
	}

	/**
	 * Sound loading method. Defers to FlansModResourceHandler
	 */
	@Override
	public void loadSound(String contentPack, String type, String sound)
	{
		SoundEvent event = FlansModResourceHandler.getSoundEvent(sound);
		if(event == null)
		{
			FlansMod.log.warn("Null sound event");
			return;
		}
		if(!eventsToRegister.contains(event))
		{
			eventsToRegister.add(event);
		}
	}

	/**
	 * Checks whether "player" is the current player. Always false on server, since there is no current player
	 */
	@Override
	public boolean isThePlayer(Player player)
	{
		return player == Minecraft.getInstance().player;
	}

	/* Gun and armour box crafting methods */
	@Override
	public void buyGun(GunBoxType type, InfoType gun)
	{
		FlansMod.getPacketHandler().sendToServer(new PacketBuyWeapon(type, gun));
		PlayerData data = PlayerHandler.getPlayerData(Minecraft.getInstance().player);
		data.shootTimeLeft = data.shootTimeRight = 10;
	}

	@Override
	public void buyArmour(String shortName, int piece, ArmourBoxType box)
	{
		FlansMod.getPacketHandler().sendToServer(new PacketBuyArmour(box.shortName, shortName, piece));
		PlayerData data = PlayerHandler.getPlayerData(Minecraft.getInstance().player);
		data.shootTimeLeft = data.shootTimeRight = 10;
	}

	@Override
	public void craftDriveable(Player player, DriveableType type)
	{
		//Craft it this side (so the inventory updates immediately) and then send a packet to the server so that it is crafted that side too
		super.craftDriveable(player, type);
		if(player.level().isClientSide())
			FlansMod.getPacketHandler().sendToServer(new PacketCraftDriveable(type.shortName));
	}

	@Override
	public void repairDriveable(Player driver, EntityDriveable driving, DriveablePart part)
	{
		//Repair it this side (so the inventory updates immediately) and then send a packet to the server so that it is repaired that side too
		super.repairDriveable(driver, driving, part);
		if(driver.level().isClientSide())
			FlansMod.getPacketHandler().sendToServer(new PacketRepairDriveable(part.type));
	}

	/**
	 * Helper method that returns whether there is a GUI open
	 */
	@Override
	public boolean isScreenOpen()
	{
		return Minecraft.getInstance().screen != null;
	}

	/**
	 * Mecha input getters
	 */
	@Override
	public boolean isKeyDown(int key)
	{
		switch(key)
		{
			case 0: //Press Forwards
				return Minecraft.getInstance().options.keyUp.isDown();

			case 1: //Press Backwards
				return Minecraft.getInstance().options.keyDown.isDown();

			case 2: //Press Left
				return Minecraft.getInstance().options.keyLeft.isDown();

			case 3: //Press Right
				return Minecraft.getInstance().options.keyRight.isDown();

			case 4: //Press Jump
				return Minecraft.getInstance().options.keyJump.isDown();
		}
		return false;
	}

	/**
	 * Helper method that deals with the way Minecraft handles binding keys to the mouse
	 */
	@Override
	public boolean keyDown(int keyCode)
	{
		return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(Minecraft.getInstance().options.keyJump).getValue() == keyCode
				&& Minecraft.getInstance().options.keyJump.isDown();
	}

	@Override
	public void addMissingJSONs(HashMap<Integer, InfoType> types)
	{
		for(InfoType type : types.values())
		{
			try
			{
				EnumType typeToCheckFor = EnumType.getFromObject(type);
				File contentPackDir = new File(FlansMod.flanDir, type.contentPack);
				if(contentPackDir.isDirectory())
				{
					File itemModelsDir = new File(contentPackDir, "/assets/flansmod/models/item");
					if(!itemModelsDir.exists())
						itemModelsDir.mkdirs();
					File blockModelsDir = new File(contentPackDir, "/assets/flansmod/models/block");
					if(!blockModelsDir.exists())
						blockModelsDir.mkdirs();
					File blockstatesDir = new File(contentPackDir, "/assets/flansmod/blockstates");
					if(!blockstatesDir.exists())
						blockstatesDir.mkdirs();

					if(typeToCheckFor != EnumType.team && typeToCheckFor != EnumType.playerClass)
					{
						createJSONFile(new File(itemModelsDir, type.shortName.toLowerCase() + ".json"),
								"{ \"parent\": \"minecraft:item/generated\", \"textures\": { \"layer0\": \"flansmod:items/" + type.iconPath + "\" } }");
					}
				}
			}
			catch(Exception e)
			{
				FlansMod.log.error(e.getMessage());
			}
		}
	}

	private void createJSONFile(File file, String contents) throws Exception
	{
		if(FlansMod.forceUpdateJSONs)
		{
			if(file.exists())
			{
				if(!file.delete())
					FlansMod.log.warn("FAILED TO DELETE");
			}

			file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(contents);
			out.close();
		}
		else if(!file.exists())
		{
			file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(contents);
			out.close();
		}
	}
}
