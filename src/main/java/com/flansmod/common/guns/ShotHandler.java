package com.flansmod.common.guns;

import static com.flansmod.common.util.BlockUtil.destroyBlock;
import static com.flansmod.common.guns.raytracing.FlansModRaytracer.Raytrace;

import java.util.List;
import java.util.Optional;

import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.common.FlansMod;
import com.flansmod.common.FlansModExplosion;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.BlockHit;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.BulletHit;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.DriveableHit;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.EntityHit;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.PlayerBulletHit;
import com.flansmod.common.network.PacketBlockHitEffect;
import com.flansmod.common.network.PacketBulletTrail;
import com.flansmod.common.network.PacketFlak;
import com.flansmod.common.network.PacketHitMarker;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.teams.TeamsRound;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.vector.Vector3f;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;

/**
 * Class containing a bunch of shooting related functions
 */
public class ShotHandler
{
	
	/**
	 * For any kind of shooting this method should be used. It handles everything including the differentiation between spawning a EntityBullet and performing a raytrace
	 * 
	 * @param world             Level where the shot is fired
	 * @param shot              FiredShot object, created using the guidelines
	 * @param bulletAmount      Number how many bullets should be fired
	 * @param rayTraceOrigin    Origin of the bullet
	 * @param shootingDirection Direction where the bullet will travel
	 */	
	public static void fireGun(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection)
	{
		fireGun(world, shot, bulletAmount, rayTraceOrigin, shootingDirection, ShootBulletHandler.instance);
	}
	
	/**
	 * For any kind of shooting this method should be used. It handles everything including the differentiation between spawning a EntityBullet and performing a raytrace
	 * 
	 * @param world             Level where the shot is fired
	 * @param shot              FiredShot object, created using the guidelines
	 * @param bulletAmount      Number how many bullets should be fired
	 * @param rayTraceOrigin    Origin of the bullet
	 * @param shootingDirection Direction where the bullet will travel
	 * @param handler           ShootBulletHandler which is called every time a shot is fired (bulletAmount times)
	 */
	public static void fireGun(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection, ShootBulletHandler handler)
	{
		if (shot.getFireableGun().getBulletSpeed() == 0f)
		{
			//Raytrace
			createMultipleShots(world, shot, bulletAmount, rayTraceOrigin, shootingDirection, handler);
		}
		else
		{
			//Spawn EntityBullet
			for(int i = 0; i < bulletAmount; i++)
			{
				((ServerLevel)world).addFreshEntity(new EntityBullet(world, shot, rayTraceOrigin.toVec3(), shootingDirection.toVec3()));
				handler.shooting(i < bulletAmount - 1);
			}
		}
	}
	
	private static void createMultipleShots(Level world, FiredShot shot, Integer bulletAmount, Vector3f rayTraceOrigin, Vector3f shootingDirection, ShootBulletHandler handler)
	{
		Float bulletspread = 0.0025f * shot.getFireableGun().getGunSpread() * shot.getBulletType().bulletSpread;
		for(int i = 0; i < bulletAmount; i++)
		{
			createShot(world, shot, bulletspread, rayTraceOrigin, new Vector3f(shootingDirection));
			handler.shooting(i < bulletAmount - 1);
		}
	}

	private static void createShot(Level world, FiredShot shot, Float bulletspread, Vector3f rayTraceOrigin, Vector3f shootingDirection)
	{
		randomizeVectorDirection(world, shootingDirection, bulletspread, shot.getFireableGun().getSpreadPattern());
		shootingDirection.scale(500.0f);
		
		Float penetrationPower = shot.getBulletType().penetratingPower;
		//first tries to get the player because the players vehicle is also ignored, or get the player independent shooter or null
		Entity ignore = shot.getPlayerOptional().isPresent() ? shot.getPlayerOptional().get() : shot.getShooterOptional().orElse(null);
		
		List<BulletHit> hits = Raytrace(world, ignore, false, null, rayTraceOrigin, shootingDirection, 0, penetrationPower);
		Vector3f previousHitPos = rayTraceOrigin;
		Vector3f finalhit = null;
		
		for (int i = 0;i<hits.size();i++)
		{
			BulletHit hit = hits.get(i);
			Vector3f shotVector = (Vector3f) new Vector3f(shootingDirection).scale(hit.intersectTime);
			Vector3f hitPos = Vector3f.add(rayTraceOrigin, shotVector, null);
			
			if(FlansMod.DEBUG)
			{
				if (hit instanceof BlockHit)
				{
					((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hitPos, 1000, 1.0f, 0f, 1.0f));
				}
				else
				{
					((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hitPos, 1000, 1.0f, 1.0f, 1.0f));
				}
				((ServerLevel)world).addFreshEntity(new EntityDebugVector(world, previousHitPos, Vector3f.sub(hitPos, previousHitPos, null), 1000, 0.5f, 0.5f, ((float)i/hits.size())));
			}
			previousHitPos = hitPos;
			
			penetrationPower = OnHit(world, hitPos, shootingDirection, shot, hit, penetrationPower);
			if (penetrationPower <= 0f) {
				onDetonate(world, shot, hitPos);
				finalhit = hitPos;
				break;
			}
		}
		
		if (finalhit == null)
		{
			finalhit = Vector3f.add(rayTraceOrigin, shootingDirection, null);
		}
		//Animation
		//TODO should this be send to all Players?
		FlansMod.packetHandler.sendToAllAround(new PacketBulletTrail(rayTraceOrigin, finalhit, 0.05f, 10f, 10f, shot.getBulletType().trailTexture), rayTraceOrigin.x, rayTraceOrigin.y, rayTraceOrigin.z, 500f, GunUtil.getDimensionId(world));
	}
	
	/**
	 * @param world             Level which contains the location of the hit
	 * @param hit               The location of the hit
	 * @param shootingDirection The direction the shot was fired
	 * @param shot              The FiredShot instance of the shot
	 * @param bulletHit         BulletHit if the object hit
	 * @param penetratingPower  Power of the bullet to penetrate objects
	 * @return The remaining penetrationPower after hitting the object specified in the BulletHit
	 */
	public static Float OnHit(Level world, Vector3f hit, Vector3f shootingDirection, FiredShot shot, BulletHit bulletHit, Float penetratingPower)
	{
		Float damage = shot.getFireableGun().getDamage();
		
		BulletType bulletType = shot.getBulletType();
		if(bulletHit instanceof DriveableHit)
		{
			DriveableHit driveableHit = (DriveableHit)bulletHit;
			penetratingPower = driveableHit.driveable.bulletHit(bulletType, shot.getFireableGun().getDamageAgainstVehicles(), driveableHit, penetratingPower);
			if(FlansMod.DEBUG)
				((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hit, 1000, 0F, 0F, 1F));
			
			//Send hit marker, if player is present
			shot.getPlayerOptional().ifPresent((ServerPlayer player) ->
			{
				FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
			});
		}
		else if(bulletHit instanceof PlayerBulletHit)
		{
			PlayerBulletHit playerHit = (PlayerBulletHit)bulletHit;
			penetratingPower = playerHit.hitbox.hitByBullet(shot, damage, penetratingPower);
			if(FlansMod.DEBUG)
				((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hit, 1000, 1F, 0F, 0F));
			Optional<ServerPlayer> optionalPlayer = shot.getPlayerOptional();
			// Check teams
			if (optionalPlayer.isPresent())
			{
				ServerPlayer player = optionalPlayer.get();
				TeamsRound round;
				if(TeamsManager.getInstance() != null && (round = TeamsManager.getInstance().currentRound) != null)
				{
					Optional<Team> shooterTeam = round.getTeam(player);
					Optional<Team> victimTeam = round.getTeam(playerHit.hitbox.player);
					
					if (!shooterTeam.isPresent() || !victimTeam.isPresent() || !shooterTeam.get().equals(victimTeam.get()))
					{
						FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
					}
				}
				else // No teams mod, just add marker
				{
					FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
				}
			}
		}
		else if(bulletHit instanceof EntityHit)
		{
			EntityHit entityHit = (EntityHit)bulletHit;
			if(entityHit.entity != null)
			{
				if(entityHit.entity.hurtServer((ServerLevel)world, shot.getDamageSource(), damage * bulletType.damageVsLiving) && entityHit.entity instanceof LivingEntity)
				{
					LivingEntity living = (LivingEntity)entityHit.entity;
					for(MobEffectInstance effect : bulletType.hitEffects)
					{
						living.addEffect(effect);
					}
				}
				if(bulletType.setEntitiesOnFire)
					entityHit.entity.igniteForTicks(20);
				penetratingPower -= 1F;
				if(FlansMod.DEBUG)
					((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hit, 1000, 1F, 1F, 0F));
			}
			
			//Send hit marker, if player is present
			shot.getPlayerOptional().ifPresent((ServerPlayer player) ->
			{
				FlansMod.getPacketHandler().sendTo(new PacketHitMarker(), player);
			});
			
			
		}
		else if(bulletHit instanceof BlockHit)
		{
			BlockHit blockHit = (BlockHit)bulletHit;
			HitResult raytraceResult = blockHit.getRayTraceResult();
			//If the hit wasn't an entity hit, then it must've been a block hit
			BlockPos pos = ((BlockHitResult)raytraceResult).getBlockPos();
			if(FlansMod.DEBUG)
				((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, hit, 1000, 0F, 1F, 0F));
			
			BlockState state = blockHit.getIBlockState();
			//If the bullet breaks glass, and can do so according to FlansMod, do so.
			if(bulletType.breaksGlass && state.is(BlockTags.IMPERMEABLE))
			{
				if(TeamsManager.canBreakGlass)
				{
					ServerLevel worldServer = (ServerLevel)world;
					destroyBlock(worldServer, pos, shot.getPlayerOptional().orElse(null), false);
				}
			}
			
			penetratingPower -= getBlockPenetrationDecrease(state, pos, world);
			
			Direction faceing = ((BlockHitResult)blockHit.getRayTraceResult()).getDirection();
			Vector3f bulletDir = new Vector3f(shootingDirection);
			bulletDir.normalise();
			bulletDir.scale(0.5f);
			
			for (Player player : ((ServerLevel)world).players())
			{
				//Checks if the player is in a radius of 300 Blocks (300 squared = 90000)
				if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 90000)
				{
					FlansMod.getPacketHandler().sendTo(new PacketBlockHitEffect(hit, bulletDir, pos, faceing), (ServerPlayer) player);
				}
			}
			
			//play sound when bullet hits block
			PacketPlaySound.sendSoundPacket(hit.x, hit.y, hit.z, bulletType.hitSoundRange, GunUtil.getDimensionId(world), bulletType.hitSound, false);
		}
		if(penetratingPower <= 0F || (bulletType.explodeOnImpact
				//&& (bullet == null || bullet.ticksInAir > 1)
		))
		{
			return -1f;
		}
		return penetratingPower;
	}
	
	/**
	 * @param world       Level which contains the detonatePos
	 * @param shot        FiredShot instance of the shot
	 * @param detonatePos Location where the detonation should happen
	 */
	public static void onDetonate(Level world, FiredShot shot, Vector3f detonatePos)
	{
		BulletType bulletType = shot.getBulletType();
		
		if(bulletType.explosionRadius > 0)
		{
			new FlansModExplosion(world, shot.getShooterOptional().orElse(null), shot.getPlayerOptional(), bulletType,
					detonatePos.x, detonatePos.y, detonatePos.z, bulletType.explosionRadius, bulletType.fireRadius > 0, bulletType.flak > 0, bulletType.explosionBreaksBlocks);
		}
		if(bulletType.fireRadius > 0)
		{
			for(float i = -bulletType.fireRadius; i < bulletType.fireRadius; i++)
			{
				for(float k = -bulletType.fireRadius; k < bulletType.fireRadius; k++)
				{
					for(int j = -1; j < 1; j++)
					{
						if(world.getBlockState(new BlockPos((int)(detonatePos.x + i), (int)(detonatePos.y + j), (int)(detonatePos.z + k))).isAir())
						{
							world.setBlock(new BlockPos((int)(detonatePos.x + i), (int)(detonatePos.y + j), (int)(detonatePos.z + k)), Blocks.FIRE.defaultBlockState(), 2);
						}
					}
				}
			}
		}
		// Send flak packet
		if(bulletType.flak > 0)
		{
			FlansMod.getPacketHandler().sendToAllAround(new PacketFlak(detonatePos.x, detonatePos.y, detonatePos.z, bulletType.flak, bulletType.flakParticles), detonatePos.x, detonatePos.y, detonatePos.z, 200, GunUtil.getDimensionId(world));
		}
		// Drop item on hitting if bullet requires it
		if(bulletType.dropItemOnHit != null)
		{
			//TODO save ItemStack on load into the bulletType
			ItemStack dropStack = InfoType.getRecipeElement(bulletType.dropItemOnHit);
			
			if(dropStack != null && !dropStack.isEmpty())
			{
				ItemEntity entityitem = new ItemEntity(world, detonatePos.x, detonatePos.y, detonatePos.z, dropStack);
				entityitem.setDefaultPickUpDelay();
				((ServerLevel)world).addFreshEntity(entityitem);
			}
		}

	}
	
	public static void randomizeVectorDirection(Level world, Vector3f vector, Float spread, EnumSpreadPattern pattern)
	{
		Vector3f xAxis = Vector3f.cross(vector, new Vector3f(0f, 1f, 0f), null);
		xAxis.normalise();
		Vector3f yAxis = Vector3f.cross(vector, xAxis, null);
		yAxis.normalise();
		
		switch(pattern)
		{
			case circle:
			{
				float theta = (float)(world.getRandom().nextDouble() * Math.PI * 2.0f);
				float radius = (float)world.getRandom().nextDouble() * spread;
				float xComponent = radius * (float)Math.sin(theta);
				float yComponent = radius * (float)Math.cos(theta);
				
				xAxis.scale(xComponent);
				yAxis.scale(yComponent);
				
				Vector3f.add(vector, xAxis, vector);
				Vector3f.add(vector, yAxis, vector);
				
				break;
			}
			case cube:
			{
				vector.x += (float)world.getRandom().nextGaussian() * spread;
				vector.y += (float)world.getRandom().nextGaussian() * spread;
				vector.z += (float)world.getRandom().nextGaussian() * spread;
				break;
			}
			case horizontal:
			{
				float xComponent = spread * (world.getRandom().nextFloat() * 2f - 1f);
				
				xAxis.scale(xComponent);
				
				Vector3f.add(vector, xAxis, vector);
				
				break;
			}
			case vertical:
			{
				float yComponent = spread * (world.getRandom().nextFloat() * 2f - 1f);
				
				yAxis.scale(yComponent);
				
				Vector3f.add(vector, yAxis, vector);
				
				break;
			}
			case triangle:
			{
				// Random square, then fold the corners
				float xComponent = world.getRandom().nextFloat() * 2f - 1f;
				float yComponent = world.getRandom().nextFloat() * 2f - 1f;
				
				if(xComponent > 0f)
				{
					if(yComponent > 1.0f - xComponent * 2f)
					{
						yComponent = -yComponent;
						xComponent = 1f - xComponent;
					}
				}
				else
				{
					if(yComponent > xComponent * 2f + 1f)
					{
						yComponent = -yComponent;
						xComponent = -1f - xComponent;
					}
				}
				
				xComponent *= spread;
				yComponent *= spread;
				
				xAxis.scale(xComponent);
				yAxis.scale(yComponent);
				
				Vector3f.add(vector, xAxis, vector);
				Vector3f.add(vector, yAxis, vector);
				
				break;
			}
			default:
				break;
		}

	}
	
	public static float getBlockPenetrationDecrease(BlockState blockstate, BlockPos pos, Level world)
	{
		float hardness = blockstate.getDestroySpeed(world, pos) * 2;
		if (hardness < 0)
		{
			return 1000; // Some high value for invincible blocks
		}
		else
		{
			return Math.max(hardness, 1);
		}
	}
}
