package com.shyrack.flansmodupgraded.client;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.actors.threadpool.Arrays;

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

@SideOnly(Side.CLIENT)
public class FlansModClient extends FlansMod
{
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
	public static HashMap<EntityLivingBase, GunAnimations> gunAnimationsRight = new HashMap<>(),
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
		if(minecraft.player == null || minecraft.world == null)
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
			minecraft.shutdown();
		}
		
		// Guns
		if(scopeTime > 0)
			scopeTime--;
		if(playerRecoil > 0)
			playerRecoil *= 0.8F;
		if(hitMarkerTime > 0)
			hitMarkerTime--;
		minecraft.player.rotationPitch -= playerRecoil;
		antiRecoil += playerRecoil;
		
		minecraft.player.rotationPitch += antiRecoil * 0.2F;
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
		ItemStack itemstackInHand = minecraft.player.inventory.getCurrentItem();
		Item itemInHand = itemstackInHand.getItem();
		if(currentScope != null)
		{
			// If we've opened a GUI page, or we switched weapons, close the current scope
			if(FMLClientHandler.instance().getClient().currentScreen != null
				|| !(itemInHand instanceof ItemGun)
				|| ((ItemGun)itemInHand).GetType().getCurrentScope(itemstackInHand) != currentScope)
			{
				currentScope = null;
				minecraft.gameSettings.fovSetting = originalFOV;
				minecraft.gameSettings.mouseSensitivity = originalMouseSensitivity;
				minecraft.gameSettings.thirdPersonView = originalThirdPerson;
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
		
		if(minecraft.getRenderViewEntity() == null ||
			minecraft.getRenderViewEntity().isDead)
		{
			minecraft.setRenderViewEntity(minecraft.player);
		}
	}
	
	public static void setScope(IScope scope)
	{
		GameSettings gameSettings = FMLClientHandler.instance().getClient().gameSettings;
		
		if(scopeTime <= 0 && FMLClientHandler.instance().getClient().currentScreen == null)
		{
			if(currentScope == null)
			{
				currentScope = scope;
				lastZoomLevel = scope.getZoomFactor();
				lastFOVZoomLevel = scope.getFOVFactor();
				float f = originalMouseSensitivity = gameSettings.mouseSensitivity;
				gameSettings.mouseSensitivity = f / (float)Math.sqrt(scope.getZoomFactor());
				originalThirdPerson = gameSettings.thirdPersonView;
				gameSettings.thirdPersonView = 0;
				originalFOV = gameSettings.fovSetting;
			}
			else
			{
				currentScope = null;
				gameSettings.mouseSensitivity = originalMouseSensitivity;
				gameSettings.thirdPersonView = originalThirdPerson;
				gameSettings.fovSetting = originalFOV;
			}
			scopeTime = 10;
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
			minecraft.gameSettings.fovSetting = (((originalFOV * 40 + 70) / zoomToApply) - 70) / 40;
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
	
	public static Minecraft minecraft = FMLClientHandler.instance().getClient();
	
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
	
	@SideOnly(Side.CLIENT)
	public static Particle getParticle(String s, World w, double x, double y, double z)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		int particleID = 0;
		int[] data = new int[0];
		
		if(s.equals("hugeexplosion")) particleID = EnumParticleTypes.EXPLOSION_HUGE.getParticleID();
		else if(s.equals("largeexplode")) particleID = EnumParticleTypes.EXPLOSION_LARGE.getParticleID();
		else if(s.equals("explode")) particleID = EnumParticleTypes.EXPLOSION_NORMAL.getParticleID();
		else if(s.equals("fireworksSpark")) particleID = EnumParticleTypes.FIREWORKS_SPARK.getParticleID();
		else if(s.equals("bubble")) particleID = EnumParticleTypes.WATER_BUBBLE.getParticleID();
		else if(s.equals("splash")) particleID = EnumParticleTypes.WATER_SPLASH.getParticleID();
		else if(s.equals("wake")) particleID = EnumParticleTypes.WATER_WAKE.getParticleID();
		else if(s.equals("drop")) particleID = EnumParticleTypes.WATER_DROP.getParticleID();
		else if(s.equals("suspended")) particleID = EnumParticleTypes.SUSPENDED.getParticleID();
		else if(s.equals("depthsuspend")) particleID = EnumParticleTypes.SUSPENDED_DEPTH.getParticleID();
		else if(s.equals("townaura")) particleID = EnumParticleTypes.TOWN_AURA.getParticleID();
		else if(s.equals("crit")) particleID = EnumParticleTypes.CRIT.getParticleID();
		else if(s.equals("magicCrit")) particleID = EnumParticleTypes.CRIT_MAGIC.getParticleID();
		else if(s.equals("smoke")) particleID = EnumParticleTypes.SMOKE_NORMAL.getParticleID();
		else if(s.equals("largesmoke")) particleID = EnumParticleTypes.SMOKE_LARGE.getParticleID();
		else if(s.equals("spell")) particleID = EnumParticleTypes.SPELL.getParticleID();
		else if(s.equals("instantSpell")) particleID = EnumParticleTypes.SPELL_INSTANT.getParticleID();
		else if(s.equals("mobSpell")) particleID = EnumParticleTypes.SPELL_MOB.getParticleID();
		else if(s.equals("mobSpellAmbient")) particleID = EnumParticleTypes.SPELL_MOB_AMBIENT.getParticleID();
		else if(s.equals("witchMagic")) particleID = EnumParticleTypes.SPELL_WITCH.getParticleID();
		else if(s.equals("dripWater")) particleID = EnumParticleTypes.DRIP_WATER.getParticleID();
		else if(s.equals("dripLava")) particleID = EnumParticleTypes.DRIP_LAVA.getParticleID();
		else if(s.equals("angryVillager")) particleID = EnumParticleTypes.VILLAGER_ANGRY.getParticleID();
		else if(s.equals("happyVillager")) particleID = EnumParticleTypes.VILLAGER_HAPPY.getParticleID();
		else if(s.equals("note")) particleID = EnumParticleTypes.NOTE.getParticleID();
		else if(s.equals("portal")) particleID = EnumParticleTypes.PORTAL.getParticleID();
		else if(s.equals("enchantmenttable")) particleID = EnumParticleTypes.ENCHANTMENT_TABLE.getParticleID();
		else if(s.equals("flame")) particleID = EnumParticleTypes.FLAME.getParticleID();
		else if(s.equals("lava")) particleID = EnumParticleTypes.LAVA.getParticleID();
		else if(s.equals("footstep")) particleID = EnumParticleTypes.FOOTSTEP.getParticleID();
		else if(s.equals("cloud")) particleID = EnumParticleTypes.CLOUD.getParticleID();
		else if(s.equals("reddust")) particleID = EnumParticleTypes.REDSTONE.getParticleID();
		else if(s.equals("snowballpoof")) particleID = EnumParticleTypes.SNOWBALL.getParticleID();
		else if(s.equals("snowshovel")) particleID = EnumParticleTypes.SNOW_SHOVEL.getParticleID();
		else if(s.equals("slime")) particleID = EnumParticleTypes.SLIME.getParticleID();
		else if(s.equals("heart")) particleID = EnumParticleTypes.HEART.getParticleID();
		else if(s.equals("barrier")) particleID = EnumParticleTypes.BARRIER.getParticleID();
		else if(s.contains("_"))
		{
			String[] split = s.split("_", 3);
			
			
			if(split[0].equals("iconcrack"))
			{
				data = new int[]{Item.getIdFromItem(InfoType.getRecipeElement(split[1], 1, 0).getItem())};
				particleID = EnumParticleTypes.ITEM_CRACK.getParticleID();
			}
			else
			{
				data = new int[]{
					Block.getIdFromBlock(Block.getBlockFromItem(InfoType.getRecipeElement(split[1], 1, 0).getItem()))};
				
				if(split[0].equals("blockcrack"))
				{
					particleID = EnumParticleTypes.BLOCK_CRACK.getParticleID();
				}
				else if(split[0].equals("blockdust"))
				{
					particleID = EnumParticleTypes.BLOCK_DUST.getParticleID();
				}
			}
		}
		
		return mc.effectRenderer.spawnEffectParticle(particleID, x, y, z, 0D, 0D, 0D, data);
	}
	
	public static GunAnimations getGunAnimations(EntityLivingBase living, EnumHand hand)
	{
		GunAnimations animations;
		if(hand == EnumHand.OFF_HAND)
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
		if(FlansMod.ticker % lightOverrideRefreshRate == 0 && mc.world != null)
		{
			// Check graphics setting and adjust refresh rate
			lightOverrideRefreshRate = mc.gameSettings.fancyGraphics ? 10 : 20;
			
			// Reset old light values
			blockLightOverrides.forEach(blockPos -> mc.world.checkLightFor(EnumSkyBlock.BLOCK, blockPos));
			// Clear the list
			blockLightOverrides.clear();
			
			//Find all flashlights
			for(EntityPlayer player : mc.world.playerEntities)
			{
				ItemStack currentHeldItem = player.getHeldItemMainhand();
				if(currentHeldItem.getItem() instanceof ItemGun)
				{
					GunType type = ((ItemGun)currentHeldItem.getItem()).GetType();
					AttachmentType grip = type.getGrip(currentHeldItem);
					if(grip != null && grip.flashlight)
					{
						for(int i = 0; i < 2; i++)
						{
							RayTraceResult ray = player.rayTrace(grip.flashlightRange / 2F * (i + 1), 1F);
							if(ray != null)
							{
								int x = ray.getBlockPos().getX();
								int y = ray.getBlockPos().getY();
								int z = ray.getBlockPos().getZ();
								EnumFacing side = ray.sideHit;
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
			
			for(Entity entity : mc.world.loadedEntityList)
			{
				if(entity instanceof EntityBullet)
				{
					EntityBullet bullet = (EntityBullet)entity;
					if(!bullet.isDead && bullet.getFiredShot().getBulletType().hasLight)
					{
						int x = MathHelper.floor(bullet.posX);
						int y = MathHelper.floor(bullet.posY);
						int z = MathHelper.floor(bullet.posZ);
						BlockPos blockPos = new BlockPos(x, y, z);
						blockLightOverrides.add(blockPos);
						lightBlock(mc, blockPos, 15);
					}
				}
				else if(entity instanceof EntityMecha)
				{
					EntityMecha mecha = (EntityMecha)entity;
					int x = MathHelper.floor(mecha.posX);
					int y = MathHelper.floor(mecha.posY);
					int z = MathHelper.floor(mecha.posZ);
					if(mecha.lightLevel() > 0)
					{
						BlockPos blockPos = new BlockPos(x, y, z);
						int lightLevel = Math
							.max(mc.world.getLightFor(EnumSkyBlock.BLOCK, blockPos), mecha.lightLevel());
						blockLightOverrides.add(blockPos);
						lightBlock(mc, blockPos, lightLevel);
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
									mc.world.setLightFor(EnumSkyBlock.SKY, blockPos,
										Math.abs(i) + Math.abs(j) + Math.abs(k));
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
		mc.world.setLightFor(EnumSkyBlock.BLOCK, blockPos, lightValue);
		Vec3i diffVec = new Vec3i(1, 1, 1);
		BlockPos
			.getAllInBox(blockPos.subtract(diffVec), blockPos.add(diffVec))
			.forEach(posToUpdate ->
			{
				if(!posToUpdate.equals(blockPos))
				{
					mc.world.checkLightFor(EnumSkyBlock.BLOCK, posToUpdate);
				}
			});
	}
}
