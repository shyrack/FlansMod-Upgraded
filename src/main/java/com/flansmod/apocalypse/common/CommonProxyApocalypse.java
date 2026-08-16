package com.flansmod.apocalypse.common;

import java.util.HashMap;
import java.util.Set;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.flansmod.apocalypse.common.entity.EntityFakePlayer;
import com.flansmod.apocalypse.common.entity.EntityFlyByPlane;
import com.flansmod.apocalypse.common.entity.EntityNukeDrop;
import com.flansmod.apocalypse.common.entity.EntitySurvivor;
import com.flansmod.apocalypse.common.network.PacketApocalypseCountdown;
import com.flansmod.apocalypse.common.world.TeleporterApocalypse;
import com.flansmod.apocalypse.common.world.buildings.StructureAbandonedVillagePieces;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.parts.PartType;

public class CommonProxyApocalypse
{
	private int apocalypseCountdown = 0;
	/**
	 * The mecha that started all this
	 */
	private EntityMecha apocalypseMecha = null;
	public ApocalypseData data;
	
	private static HashMap<Player, BlockPos> deathPoints = new HashMap<>();
	private boolean dataLoaded = false;
	
	public void preInit()
	{
		data = new ApocalypseData();
		
		StructureAbandonedVillagePieces.registerVillagePieces();
		
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> itemPlaced(entity, world));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> playerDied(entity, damageSource));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> playerRespawned(newPlayer));
	}
	
	public void init()
	{
		FlansMod.getPacketHandler().registerPacket(PacketApocalypseCountdown.class);
	}
	
	public void postInit()
	{
		
	}
	
	private int tick = 0;
		
	/**
	 * Tick hook for server
	 */
	public void tick()
	{
		tick++;
		//Countdown to 0 and on 0, do apocalypse things
		if(getApocalypseCountdown() > 0)
		{
			if(apocalypseMecha == null || apocalypseMecha.isRemoved())
			{
				setApocalypseCountdown(0);
			}
			else
			{
				setApocalypseCountdown(getApocalypseCountdown() - 1);
				
				//Wiggle the apocalypse mecha
				apocalypseMecha.getSeat(0).prevLooking = apocalypseMecha.getSeat(0).looking.clone();
				apocalypseMecha.getSeat(0).looking.rotateGlobalYaw(apocalypseMecha.level().getRandom().nextFloat() * 10F);
				apocalypseMecha.getSeat(0).looking.rotateGlobalPitch((float)apocalypseMecha.level().getRandom().nextGaussian() * 3F);
				
				//Drop nukes
				if(getApocalypseCountdown() % 20 == 0)
				{
					Level world = apocalypseMecha.level();
					float range = 150F;
					if(!world.isClientSide())
						((ServerLevel)world).addFreshEntity(new EntityNukeDrop(world, apocalypseMecha.getX() + world.getRandom().nextGaussian() * range, 256, apocalypseMecha.getZ() + world.getRandom().nextGaussian() * range));
				}
				
				//Start the apocalypse
				if(getApocalypseCountdown() == 0)
				{
					FlansMod.log.info("The apocalypse has begun!");
					Player placer = apocalypseMecha.placer;
					
					switch(FlansModApocalypse.OPTION)
					{
						case DIM:
							for(int i = 0; i < placer.level().players().size(); i++)
								if(placer.level().players().get(i).level().dimension() == Level.OVERWORLD)
									sendPlayerToApocalypse(placer.level().players().get(i));
							break;
						case DIM_OPT_IN:
							break;
						case NEARBY:
							for(Object player : placer.level().players())
								if(((Entity)player).level().dimension() == Level.OVERWORLD && ((Entity)player).distanceToSqr(placer) < 50 * 50)
									sendPlayerToApocalypse((Player)player);
							break;
						case NEARBY_OPT_IN:
							break;
						case PLACER_ONLY:
							sendPlayerToApocalypse(placer);
							break;
					}
					apocalypseMecha.discard();
				}
			}
		}
		
		if(FlansMod.serverInstance == null)
			return;
		ServerLevel world = FlansMod.serverInstance.getLevel(FlansModApocalypse.APOCALYPSE_DIMENSION_KEY);
		if(world != null)
		{
			FlansModApocalypse.INSTANCE.UpdateBossFight(world);
			
			for(int i = 0; i < world.players().size(); i++)
			{
				Player player = world.players().get(i);
				
				if(world.getRandom().nextInt(5000) == 0)
				{
					java.util.Random rand = new java.util.Random(world.getRandom().nextLong());
					double dX = rand.nextFloat() - 0.5F;
					double dZ = rand.nextFloat() - 0.5F;
					double mag = Math.sqrt(dX * dX + dZ * dZ);
					dX /= mag;
					dZ /= mag;
					double dist = 200D;
					dX *= dist;
					dZ *= dist;
					
					PlaneType type = FlansModApocalypse.getLootGenerator().getRandomPlane(rand);
					if(type != null)
					{
						CompoundTag tags = new CompoundTag();
						tags.putString("Engine", FlansModApocalypse.getLootGenerator().getRandomEngine(type, rand).shortName);
						tags.putString("Type", type.shortName);
						for(EnumDriveablePart part : EnumDriveablePart.values())
						{
							tags.putInt(part.getShortName() + "_Health", type.health.get(part) == null ? 0 : type.health.get(part).health);
							tags.putBoolean(part.getShortName() + "_Fire", false);
						}
						DriveableData data = new DriveableData(tags);
						
						EntityFlyByPlane plane = new EntityFlyByPlane(world, player.getX() + dX, 120, player.getZ() + dZ, type, data);
						
						plane.throttle = 1F;
						world.addFreshEntity(plane);
						
						float yaw = 180F + (float)Math.atan2(dZ, dX) * 180F / 3.14159F;
						plane.getSeat(0).looking.setAngles(yaw, 0F, 0F);
						plane.getSeat(0).prevLooking.setAngles(yaw, 0F, 0F);
						plane.axes.setAngles(yaw, 0F, 0F);
						plane.prevAxes.setAngles(yaw, 0F, 0F);
						
						Entity pilot = new Skeleton(EntityType.SKELETON, world);
						pilot.setPos(plane.getX(), plane.getY(), plane.getZ());
						world.addFreshEntity(pilot);
						
						pilot.startRiding(plane.getSeat(0));
					}
				}
				
				if(world.getRandom().nextInt(FlansModApocalypse.WANDERING_SURVIVOR_RARITY) == 0 && world.getSkyDarken() > 4)
				{
					double angle = world.getRandom().nextFloat() * 3.14159F * 2F;
					double dist = 50D;
					double dX = Math.cos(angle) * dist;
					double dZ = Math.sin(angle) * dist;
					
					EntitySurvivor survivor = new EntitySurvivor(world);
					survivor.setPos(player.getX() + dX, world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int)(player.getX() + dX), (int)(player.getZ() + dZ)) + 1D, player.getZ() + dZ);
					
					world.addFreshEntity(survivor);
				}
			}
		}
		
		//Save/load apocalypse entry point data
		ServerLevel overworld = FlansMod.serverInstance.getLevel(Level.OVERWORLD);
		if(overworld != null)
		{
			if(!dataLoaded)
			{
				data.loadPerWorldData(overworld);
				dataLoaded = true;
			}
			if(tick % 100 == 0)
				data.savePerWorldData(overworld);
		}
	}
	
	private void sendPlayerToApocalypse(Player player)
	{
		ServerLevel serverLevel = (ServerLevel)player.level();
		//Make a copy of the player to hold their inventory and hang around until they get back
		EntityFakePlayer fakePlayer = new EntityFakePlayer(player.level(), player);
		serverLevel.addFreshEntity(fakePlayer);
		
		player.getInventory().clearContent();
		
		//Teleport them, making note of where they got in
		player.setPortalCooldown(10);
		data.entryPoints.put(player.getUUID(), new BlockPos(Mth.floor(apocalypseMecha.getX()), Mth.floor(apocalypseMecha.getY()), Mth.floor(apocalypseMecha.getZ())));
		BlockPos exitPoint = new BlockPos(Mth.floor(apocalypseMecha.getX()), 128, Mth.floor(apocalypseMecha.getZ()));
		ServerLevel apocLevel = serverLevel.getServer().getLevel(FlansModApocalypse.APOCALYPSE_DIMENSION_KEY);
		if(apocLevel == null)
			return;
		for(; apocLevel.isEmptyBlock(exitPoint); exitPoint = exitPoint.below())
		{
		}
		((ServerPlayer)player).teleport(new TeleportTransition(apocLevel, new Vec3(exitPoint.getX() + 0.5D, exitPoint.getY() + 1D, exitPoint.getZ() + 0.5D), Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
		
		giveStarterKit(player);
	}
	
	private void giveStarterKit(Player player)
	{
		player.getInventory().add(new ItemStack(Items.STONE_PICKAXE));
		player.getInventory().add(new ItemStack(Items.STONE_SHOVEL));
		player.getInventory().add(new ItemStack(Blocks.OAK_LOG, 8));
		player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 4));
	}
	
	public void itemPlaced(Entity entity, ServerLevel world)
	{
		if(!world.isClientSide() && entity instanceof EntityMecha && entity.level().dimension() == Level.OVERWORLD)
		{
			EntityMecha mecha = (EntityMecha)entity;
			PartType engine = mecha.getDriveableData().engine;
			if(engine != null && engine.isAIChip)
			{
				setApocalypseCountdown(FlansModApocalypse.apocalypseCountdownLength);
				apocalypseMecha = mecha;
				if(mecha.placer instanceof ServerPlayer)
					FlansMod.getPacketHandler().sendTo(new PacketApocalypseCountdown(getApocalypseCountdown()), (ServerPlayer)mecha.placer);
			}
		}
	}
	
	/**
	 * Take note of where the player died
	 */
	public void playerDied(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source)
	{
		if(entity.level().dimension() == FlansModApocalypse.APOCALYPSE_DIMENSION_KEY && entity instanceof Player)
		{
			Player player = (Player)entity;
			
			deathPoints.put(player, new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()), Mth.floor(player.getZ())));
		}
	}
	
	/**
	 * Respawn the player somewhere nearby where they died on the surface
	 */
	public void playerRespawned(ServerPlayer player)
	{
		if(player.level().dimension() == FlansModApocalypse.APOCALYPSE_DIMENSION_KEY)
		{
			BlockPos pos = deathPoints.get(player);
			if(pos != null)
			{
				float angle = player.level().getRandom().nextFloat() * 2F * 3.14159F;
				pos = pos.offset((int)(Math.cos(angle) * FlansModApocalypse.SPAWN_RADIUS), 128 - pos.getY(), (int)(Math.sin(angle) * FlansModApocalypse.SPAWN_RADIUS));
				if(Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY() + pos.getZ() * pos.getZ()) < 200d)
				{
					pos.offset((pos.getX() > 0 ? 100 : -100) - pos.getX(), 0, (pos.getZ() > 0 ? 100 : -100) - pos.getZ());
				}
				for(; player.level().isEmptyBlock(pos); pos = pos.below())
				{
					
				}
				player.teleportTo((ServerLevel)player.level(), pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, Set.of(), 0F, 0F, false);
			}
		}
	}
	
	public int getApocalypseCountdown()
	{
		return apocalypseCountdown;
	}
	
	private void setApocalypseCountdown(int apocalypseCountdown)
	{
		this.apocalypseCountdown = apocalypseCountdown;
	}
	
}
