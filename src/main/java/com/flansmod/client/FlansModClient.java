package com.flansmod.client;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;

import com.flansmod.client.handlers.KeyInputHandler;
import com.flansmod.client.handlers.MouseInputHandler;
import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.teams.ClientTeamsData;
import com.flansmod.client.util.WorldRenderer;
import com.flansmod.common.ContentManager.ContentPackFlanFolder;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.EntityBullet;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.IScope;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.network.PacketTeamInfo;
import com.flansmod.common.teams.Team;
import com.flansmod.common.types.InfoType;

public class FlansModClient extends FlansMod implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		proxy.registerRenderers();
		proxy.registerSoundEvents();
		FlansMod.getPacketHandler().registerClient();
		KeyInputHandler.init();
		MouseInputHandler.init();
	}

	// Plane / Vehicle control handling
	/**
	 * Whether the player has received the vehicle tutorial text
	 */
	public static boolean doneTutorial = false;
	/**
	 * Whether the player is in mouse control mode
	 */
	public static boolean controlModeMouse = true;
	/**
	 * A delayer on the mouse control switch
	 */
	public static int controlModeSwitchTimer = 20;
	
	// Recoil variables
	/**
	 * The recoil applied to the player view by shooting
	 */
	public static float playerRecoil;
	/**
	 * The amount of compensation to apply to the recoil in order to bring it back to normal
	 */
	public static float antiRecoil;
	
	// Gun animations
	/**
	 * Gun animation variables for each entity holding a gun. Currently only applicable to the player
	 */
	public static HashMap<LivingEntity, GunAnimations> gunAnimationsRight = new HashMap<>(),
		gunAnimationsLeft = new HashMap<>();
	
	// Scope variables
	/**
	 * A delayer on the scope button to avoid repeat presses
	 */
	public static int scopeTime;
	/**
	 * The scope that is currently being looked down
	 */
	public static IScope currentScope = null;
	/**
	 * The transition variable for zooming in / out with a smoother. 0 = unscoped, 1 = scoped
	 */
	public static float zoomProgress = 0F, lastZoomProgress = 0F;
	/**
	 * The zoom level of the last scope used, for transitioning out of being scoped, even after the scope is forgotten
	 */
	public static float lastZoomLevel = 1F, lastFOVZoomLevel = 1F;
	
	// Variables to hold the state of some settings so that after being hacked for scopes, they may be restored
	/**
	 * The player's mouse sensitivity setting, as it was before being hacked by my mod
	 */
	public static float originalMouseSensitivity = 0.5F;
	/**
	 * The player's original FOV
	 */
	public static float originalFOV = 90F;
	/**
	 * The original third person mode, before being hacked
	 */
	public static int originalThirdPerson = 0;
	
	/**
	 * Whether the player is in a plane or not
	 */
	public static boolean inPlane = false;
	public static int numVehicleExceptions = 0;
	
	/**
	 * Packet containing teams mod information from the server
	 */
	public static PacketTeamInfo teamInfo;
	
	public static int hitMarkerTime = 0;
	
	public static List<BlockPos> blockLightOverrides = new ArrayList<>();
	public static int lightOverrideRefreshRate = 5;
	
	private static WorldRenderer wr;
	
	public static WorldRenderer getWorldRenderer()
	{
		return wr;
	}
	
	public void load()
	{
		log.info("Loading Flan's mod client side.");
		wr = new WorldRenderer();
	}
	
	private static void DoTextureTrim()
	{
		for(File contentPack : FlansMod.INSTANCE.contentManager.GetFolderContentPacks())
		{
			File skinFolder = new File(contentPack, "assets/flansmod/skins");
			if(skinFolder.exists() && skinFolder.isDirectory())
			{
				List<File> skins = Arrays.asList(skinFolder.listFiles());
				
				// Group together variant skins
				HashMap<String, List<File>> skinGroups = new HashMap<String, List<File>>();
				for(File skin : skins)
				{
					String skinName = skin.getName().split("\\.")[0];
					boolean foundParent = false;
					for(File other : skins)
					{
						String otherName = other.getName().split("\\.")[0];
						
						// If we are a substring of any other skin, go to that group
						if(skinName.startsWith(otherName))
						{
							if(!skinGroups.containsKey(otherName))
								skinGroups.put(otherName, new ArrayList<File>(8));
							skinGroups.get(otherName).add(skin);
							foundParent = true;
							break;
						}
					}
					if(!foundParent)
					{
						if(!skinGroups.containsKey(skinName))
							skinGroups.put(skinName, new ArrayList<File>(8));
						skinGroups.get(skinName).add(skin);
					}
				}
				
				// Now process
				for(HashMap.Entry<String, List<File>> kvp : skinGroups.entrySet())
				{
					String key = kvp.getKey();
					
					// Calculate the size
					int x = 1, y = 1;
					for(File skin : kvp.getValue())
					{
						try 
						{
							BufferedImage img = ImageIO.read(skin);
							WritableRaster alpha = img.getAlphaRaster();
							if(alpha != null)
							{
								for(int i = 0; i < alpha.getWidth(); i++)
								{
									for(int j = 0; j < alpha.getHeight(); j++)
									{
										// Skip the area we know we already contain
										if(i < x && j < y)
											continue;
										
										if(alpha.getSample(i, j, 0) > 0.0f)
										{
											if(i >= x && x < alpha.getWidth())
												x *= 2;
											if(j >= y && y < alpha.getHeight())
												y *= 2;
										}
									}
								}
							}
						} 
						catch (Exception e) 
						{
							//e.printStackTrace();
						}
					}
					
					// Then apply
					boolean anyResizeApplied = false;
					for(File skin : kvp.getValue())
					{
						try 
						{
							BufferedImage img = ImageIO.read(skin);
							if(x < img.getWidth() || y < img.getHeight())
							{
								
								Raster subImg = img.getData(new Rectangle(0, 0, x, y));
								//img.setData(subImg);
								BufferedImage cropped = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
								cropped.setData(subImg);
								ImageIO.write(cropped, "PNG", skin);
										//new File("C:\\JavaProjects\\FlansMod1.12.2_3\\tests\\" + skin.getName()));
								anyResizeApplied = true;
							}
						} 
						catch (Exception e) 
						{
							//e.printStackTrace();
						}
					}
					
					if(anyResizeApplied)
						FlansMod.log.info(key + " was resized to " + x + ", " + y);
				}	
			}
		}
	}
	
	public static void tick()
	{
		if(minecraft.player == null || minecraft.level == null)
			return;
		
		if(teamInfo != null && teamInfo.timeLeft > 0)
			teamInfo.timeLeft--;
		
		ClientTeamsData.Tick();
		
		/*
		if(Keyboard.isKeyDown(Keyboard.KEY_PAUSE))
			DoTextureTrim();
		*/
		
		// Force shutdown if too many vehicles break to prevent save data corruption
		if(numVehicleExceptions > 2)
		{
			log.error("Too many vehicle exceptions, shutting down.");
			minecraft.stop();
		}
		
		// Guns
		if(scopeTime > 0)
			scopeTime--;
		if(playerRecoil > 0)
			playerRecoil *= 0.8F;
		if(hitMarkerTime > 0)
			hitMarkerTime--;
		minecraft.player.setXRot(minecraft.player.getXRot() - playerRecoil);
		antiRecoil += playerRecoil;
		
		minecraft.player.setXRot(minecraft.player.getXRot() + antiRecoil * 0.2F);
		antiRecoil *= 0.8F;
		
		// Update gun animations for the gun in hand
		for(GunAnimations g : gunAnimationsRight.values())
		{
			g.update();
		}
		for(GunAnimations g : gunAnimationsLeft.values())
		{
			g.update();
		}
		
		// If the currently held item is not a gun or is the wrong gun, unscope
		ItemStack itemstackInHand = minecraft.player.getMainHandItem();
		Item itemInHand = itemstackInHand.getItem();
		if(currentScope != null)
		{
			// If we've opened a GUI page, or we switched weapons, close the current scope
			if(minecraft.screen != null
				|| !(itemInHand instanceof ItemGun)
				|| ((ItemGun)itemInHand).GetType().getCurrentScope(itemstackInHand) != currentScope)
			{
				currentScope = null;
				minecraft.options.fov().set((int)originalFOV);
				minecraft.options.sensitivity().set((double)originalMouseSensitivity);
				minecraft.options.setCameraType(cameraTypeFromInt(originalThirdPerson));
			}
		}
		
		// Calculate new zoom variables
		lastZoomProgress = zoomProgress;
		if(currentScope == null)
		{
			zoomProgress *= 0.66F;
		}
		else
		{
			zoomProgress = 1F - (1F - zoomProgress) * 0.66F;
		}
		
		if(controlModeSwitchTimer > 0)
			controlModeSwitchTimer--;
		
		if(minecraft.getCameraEntity() == null ||
			!minecraft.getCameraEntity().isAlive())
		{
			minecraft.setCameraEntity(minecraft.player);
		}
	}
	
	public static void setScope(IScope scope)
	{
		Options gameSettings = Minecraft.getInstance().options;
		
		if(scopeTime <= 0 && Minecraft.getInstance().screen == null)
		{
			if(currentScope == null)
			{
				currentScope = scope;
				lastZoomLevel = scope.getZoomFactor();
				lastFOVZoomLevel = scope.getFOVFactor();
				float f = originalMouseSensitivity = gameSettings.sensitivity().get().floatValue();
				gameSettings.sensitivity().set((double)(f / (float)Math.sqrt(scope.getZoomFactor())));
				originalThirdPerson = gameSettings.getCameraType().ordinal();
				gameSettings.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				originalFOV = gameSettings.fov().get().floatValue();
			}
			else
			{
				currentScope = null;
				gameSettings.sensitivity().set((double)originalMouseSensitivity);
				gameSettings.setCameraType(cameraTypeFromInt(originalThirdPerson));
				gameSettings.fov().set((int)originalFOV);
			}
			scopeTime = 10;
		}
	}

	private static net.minecraft.client.CameraType cameraTypeFromInt(int id)
	{
		switch(id)
		{
			case 1: return net.minecraft.client.CameraType.THIRD_PERSON_BACK;
			case 2: return net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
			default: return net.minecraft.client.CameraType.FIRST_PERSON;
		}
	}
	
	public static void updateCameraZoom(float smoothing)
	{
		// If the zoom has changed sufficiently, update it
		if(Math.abs(zoomProgress - lastZoomProgress) > 0.0001F)
		{
			float actualZoomProgress = lastZoomProgress + (zoomProgress - lastZoomProgress) * smoothing;
			float botchedZoomProgress = zoomProgress > 0.8F ? 1F : 0F;
			double zoomLevel = botchedZoomProgress * lastZoomLevel + (1 - botchedZoomProgress);
			float FOVZoomLevel = actualZoomProgress * lastFOVZoomLevel + (1 - actualZoomProgress);
			if(Math.abs(zoomLevel - 1F) < 0.01F)
				zoomLevel = 1.0D;
			
			float zoomToApply = Math.max(FOVZoomLevel, (float)zoomLevel);
			minecraft.options.fov().set((int)((originalFOV * 40 + 70) / zoomToApply));
		}
	}
	
	public static boolean flipControlMode()
	{
		if(controlModeSwitchTimer > 0)
			return false;
		controlModeMouse = !controlModeMouse;
		controlModeSwitchTimer = 40;
		return true;
	}
	
	public static void reloadModels(boolean reloadSkins)
	{
		for(InfoType type : InfoType.infoTypes.values())
		{
			type.reloadModel();
		}
		if(reloadSkins)
			proxy.forceReload();
	}
	
	public static Minecraft minecraft = Minecraft.getInstance();
	
	/**
	 * Gets the team class from an ID
	 */
	public static Team getTeam(int spawnerTeamID)
	{
		if(teamInfo == null)
			return null;
		else return teamInfo.getTeam(spawnerTeamID);
	}
	
	public static boolean isCurrentMap(String map)
	{
		return !(teamInfo == null || teamInfo.mapShortName == null) && teamInfo.mapShortName.equals(map);
	}
	
	public static ParticleOptions getParticleOptions(String s)
	{
		ParticleOptions options = null;

		if(s.equals("hugeexplosion")) options = ParticleTypes.EXPLOSION_EMITTER;
		else if(s.equals("largeexplode")) options = ParticleTypes.EXPLOSION;
		else if(s.equals("explode")) options = ParticleTypes.EXPLOSION;
		else if(s.equals("fireworksSpark")) options = ParticleTypes.FIREWORK;
		else if(s.equals("bubble")) options = ParticleTypes.BUBBLE;
		else if(s.equals("splash")) options = ParticleTypes.SPLASH;
		else if(s.equals("wake")) options = ParticleTypes.UNDERWATER;
		else if(s.equals("drop")) options = ParticleTypes.FALLING_WATER;
		else if(s.equals("dripWater")) options = ParticleTypes.DRIPPING_WATER;
		else if(s.equals("dripLava")) options = ParticleTypes.DRIPPING_LAVA;
		else if(s.equals("crit")) options = ParticleTypes.CRIT;
		else if(s.equals("magicCrit")) options = ParticleTypes.ENCHANTED_HIT;
		else if(s.equals("smoke")) options = ParticleTypes.SMOKE;
		else if(s.equals("largesmoke")) options = ParticleTypes.LARGE_SMOKE;
		else if(s.equals("witchMagic")) options = ParticleTypes.WITCH;
		else if(s.equals("angryVillager")) options = ParticleTypes.ANGRY_VILLAGER;
		else if(s.equals("happyVillager")) options = ParticleTypes.HAPPY_VILLAGER;
		else if(s.equals("note")) options = ParticleTypes.NOTE;
		else if(s.equals("portal")) options = ParticleTypes.PORTAL;
		else if(s.equals("enchantmenttable")) options = ParticleTypes.ENCHANT;
		else if(s.equals("flame")) options = ParticleTypes.FLAME;
		else if(s.equals("lava")) options = ParticleTypes.LAVA;
		else if(s.equals("cloud")) options = ParticleTypes.CLOUD;
		else if(s.equals("snowballpoof")) options = ParticleTypes.ITEM_SNOWBALL;
		else if(s.equals("snowshovel")) options = ParticleTypes.SNOWFLAKE;
		else if(s.equals("slime")) options = ParticleTypes.ITEM_SLIME;
		else if(s.equals("heart")) options = ParticleTypes.HEART;
		return options;
	}
	
	@Deprecated
	public static net.minecraft.client.particle.Particle getParticle(String s, Level w, double x, double y, double z)
	{
		ParticleOptions options = getParticleOptions(s);
		if(options != null)
			w.addParticle(options, x, y, z, 0D, 0D, 0D);
		return null;
	}
	
	public static GunAnimations getGunAnimations(LivingEntity living, InteractionHand hand)
	{
		GunAnimations animations;
		if(hand == InteractionHand.OFF_HAND)
		{
			if(FlansModClient.gunAnimationsLeft.containsKey(living))
				animations = FlansModClient.gunAnimationsLeft.get(living);
			else
			{
				animations = new GunAnimations();
				FlansModClient.gunAnimationsLeft.put(living, animations);
			}
		}
		else
		{
			if(FlansModClient.gunAnimationsRight.containsKey(living))
				animations = FlansModClient.gunAnimationsRight.get(living);
			else
			{
				animations = new GunAnimations();
				FlansModClient.gunAnimationsRight.put(living, animations);
			}
		}
		return animations;
	}
	
	public static void addHitMarker()
	{
		hitMarkerTime = 20;
	}
	
	/**
	 * Handle flashlight block light override
	 */
	public static void updateFlashlights(Minecraft mc)
	{
		// Handle lighting from flashlights and glowing bullets
		if(FlansMod.ticker % lightOverrideRefreshRate == 0 && mc.level != null)
		{
			// Check graphics setting and adjust refresh rate
			lightOverrideRefreshRate = mc.options.graphicsPreset().get() == net.minecraft.client.GraphicsPreset.FAST ? 20 : 10;
			
			// Reset old light values
			blockLightOverrides.forEach(blockPos -> mc.level.getLightEngine().checkBlock(blockPos));
			// Clear the list
			blockLightOverrides.clear();
			
			//Find all flashlights
			for(Player player : mc.level.players())
			{
				ItemStack currentHeldItem = player.getMainHandItem();
				if(currentHeldItem.getItem() instanceof ItemGun)
				{
					GunType type = ((ItemGun)currentHeldItem.getItem()).GetType();
					AttachmentType grip = type.getGrip(currentHeldItem);
					if(grip != null && grip.flashlight)
					{
						for(int i = 0; i < 2; i++)
						{
							HitResult ray = player.pick(grip.flashlightRange / 2F * (i + 1), 1F, false);
							if(ray != null && ray.getType() == HitResult.Type.BLOCK)
							{
								int x = ((net.minecraft.world.phys.BlockHitResult)ray).getBlockPos().getX();
								int y = ((net.minecraft.world.phys.BlockHitResult)ray).getBlockPos().getY();
								int z = ((net.minecraft.world.phys.BlockHitResult)ray).getBlockPos().getZ();
								Direction side = ((net.minecraft.world.phys.BlockHitResult)ray).getDirection();
								switch(side)
								{
									case DOWN:
										y--;
										break;
									case UP:
										y++;
										break;
									case NORTH:
										z--;
										break;
									case SOUTH:
										z++;
										break;
									case WEST:
										x--;
										break;
									case EAST:
										x++;
										break;
								}
								BlockPos blockPos = new BlockPos(x, y, z);
								blockLightOverrides.add(blockPos);
								lightBlock(mc, blockPos, 12);
							}
						}
					}
				}
			}
			
			for(Entity entity : mc.level.entitiesForRendering())
			{
				if(entity instanceof EntityBullet)
				{
					EntityBullet bullet = (EntityBullet)entity;
					if(bullet.isAlive() && bullet.getFiredShot().getBulletType().hasLight)
					{
						int x = Mth.floor(bullet.getX());
						int y = Mth.floor(bullet.getY());
						int z = Mth.floor(bullet.getZ());
						BlockPos blockPos = new BlockPos(x, y, z);
						blockLightOverrides.add(blockPos);
						lightBlock(mc, blockPos, 15);
					}
				}
				else if(entity instanceof EntityMecha)
				{
					EntityMecha mecha = (EntityMecha)entity;
					int x = Mth.floor(mecha.getX());
					int y = Mth.floor(mecha.getY());
					int z = Mth.floor(mecha.getZ());
					if(mecha.lightLevel() > 0)
					{
						BlockPos blockPos = new BlockPos(x, y, z);
						blockLightOverrides.add(blockPos);
						lightBlock(mc, blockPos, mecha.lightLevel());
					}
					if(mecha.forceDark())
					{
						for(int i = -3; i <= 3; i++)
						{
							for(int j = -3; j <= 3; j++)
							{
								for(int k = -3; k <= 3; k++)
								{
									int xd = i + x;
									int yd = j + y;
									int zd = k + z;
									BlockPos blockPos = new BlockPos(xd, yd, zd);
									blockLightOverrides.add(blockPos);
									mc.level.getLightEngine().checkBlock(blockPos);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private static void lightBlock(Minecraft mc, BlockPos blockPos, int lightValue)
	{
		mc.level.getLightEngine().checkBlock(blockPos);
		BlockPos
			.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))
			.forEach(posToUpdate ->
			{
				if(!posToUpdate.equals(blockPos))
				{
					mc.level.getLightEngine().checkBlock(posToUpdate);
				}
			});
	}
}
