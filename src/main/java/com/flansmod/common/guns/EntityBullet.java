package com.flansmod.common.guns;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.driveables.EntityPlane;
import com.flansmod.common.driveables.EntityVehicle;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.guns.raytracing.FlansModRaytracer;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.BulletHit;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.vector.Vector3f;

public class EntityBullet extends EntityShootable
{
	private static final EntityDataAccessor<String> BULLET_TYPE = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> MOTION_X = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MOTION_Y = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MOTION_Z = SynchedEntityData.defineId(EntityBullet.class, EntityDataSerializers.FLOAT);
	
	private static int bulletLife = 600; // Kill bullets after 30 seconds
	public int ticksInAir;
	
	private FiredShot shot;
	/**
	 * For homing missiles
	 */
	public Entity lockedOnTo;
	
	private float currentPenetratingPower;

	private double motionX, motionY, motionZ;

	private boolean playedFlybySound;
	
	/**
	 * These values are used to store the UUIDs until the next entity update is performed. This prevents issues caused by the loading order
	 */
	private UUID playeruuid;
	private UUID shooteruuid;
	private boolean checkforuuids;

		public EntityBullet(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityBullet(Level world)
	{
		this(ModEntities.BULLET, world);
		this.world = level();

	}
	
	public EntityBullet(Level world, FiredShot shot, Vec3 origin, Vec3 direction)
	{
		this(world);
		ticksInAir = 0;
		this.shot = shot;
		this.entityData.set(BULLET_TYPE, shot.getBulletType().shortName);
		this.entityData.set(MOTION_X, (float)direction.x);
		this.entityData.set(MOTION_Y, (float)direction.y);
		this.entityData.set(MOTION_Z, (float)direction.z);
		
		setPos(origin.x, origin.y, origin.z);
		motionX = direction.x;
		motionY = direction.y;
		motionZ = direction.z;
		setArrowHeading(motionX, motionY, motionZ, shot.getFireableGun().getGunSpread() * shot.getBulletType().bulletSpread, shot.getFireableGun().getBulletSpeed());
		
		currentPenetratingPower = shot.getBulletType().penetratingPower;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(BULLET_TYPE, "");
		builder.define(MOTION_X, 0F);
		builder.define(MOTION_Y, 0F);
		builder.define(MOTION_Z, 0F);
	}
	
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		if(key == MOTION_X)
			motionX = entityData.get(MOTION_X);
		else if(key == MOTION_Y)
			motionY = entityData.get(MOTION_Y);
		else if(key == MOTION_Z)
			motionZ = entityData.get(MOTION_Z);
	}
	
	public void setArrowHeading(double d, double d1, double d2, float spread, float speed)
	{
		spread /= 5F;
		float f2 = Mth.sqrt((float)(d * d + d1 * d1 + d2 * d2));
		d /= f2;
		d1 /= f2;
		d2 /= f2;
		d *= speed;
		d1 *= speed;
		d2 *= speed;
		d += random.nextGaussian() * 0.005D * spread * speed;
		d1 += random.nextGaussian() * 0.005D * spread * speed;
		d2 += random.nextGaussian() * 0.005D * spread * speed;
		motionX = d;
		motionY = d1;
		motionZ = d2;
		float f3 = Mth.sqrt((float)(d * d + d2 * d2));
		setYRot((float)((Math.atan2(d, d2) * 180D) / 3.1415927410125732D));
		setXRot((float)((Math.atan2(d1, f3) * 180D) / 3.1415927410125732D));
		yRotO = getYRot();
		xRotO = getXRot();
		
		getLockOnTarget();
	}
	
	/**
	 * Find the entity nearest to the missile's trajectory, anglewise
	 */
	private void getLockOnTarget()
	{
		BulletType type = shot.getBulletType();
		
		if(type.lockOnToPlanes || type.lockOnToVehicles || type.lockOnToMechas || type.lockOnToLivings || type.lockOnToPlayers)
		{
			Vector3f motionVec = new Vector3f(motionX, motionY, motionZ);
			Entity closestEntity = null;
			float closestAngle = type.maxLockOnAngle * 3.14159265F / 180F;
			
			Iterable<Entity> entities = world instanceof ServerLevel ? ((ServerLevel)world).getAllEntities() : world.getEntities(this, getBoundingBox().inflate(64D, 64D, 64D), entity -> true);
			for(Entity entity : entities)
			{
				if((type.lockOnToMechas && entity instanceof EntityMecha)
						|| (type.lockOnToVehicles && entity instanceof EntityVehicle)
						|| (type.lockOnToPlanes && entity instanceof EntityPlane)
						|| (type.lockOnToPlayers && entity instanceof Player)
						|| (type.lockOnToLivings && entity instanceof LivingEntity))
				{
					Vector3f relPosVec = new Vector3f(entity.getX() - getX(), entity.getY() - getY(), entity.getZ() - getZ());
					float angle = Math.abs(Vector3f.angle(motionVec, relPosVec));
					if(angle < closestAngle)
					{
						closestEntity = entity;
						closestAngle = angle;
					}
				}
			}
			
			if(closestEntity != null)
				lockedOnTo = closestEntity;
		}
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		try
		{
			//This checks if the shooter and/or player can be found. If they are loaded/online they will be included in the FiredShot data, if not this data will be deleted/ignored
			if (checkforuuids)
			{
				ServerPlayer player = null;
				Entity shooter = null;
				
				if (playeruuid != null)
				{
				for (Entity entity : ((ServerLevel)world).getAllEntities())
				{
					if (entity.getUUID().equals(playeruuid) && entity instanceof ServerPlayer)
					{
						player = (ServerPlayer)entity;
						break;
					}
				}
				playeruuid = null;
			}
			
			if (shooteruuid != null)
			{
				if (player != null && shooteruuid.equals(player.getUUID()))
				{
					shooter = player;
				}
				else
				{
					for (Entity entity : ((ServerLevel)world).getAllEntities())
					{
						if (entity.getUUID().equals(shooteruuid))
						{
							shooter = entity;
							break;
						}
					}
				}
				shooteruuid = null;
			}
			
			if (shooter != null)
			{
				shot = new FiredShot(shot.getFireableGun(), shot.getBulletType(), shooter, player);
			}
			
			checkforuuids = false;
			}
			
			BulletType type = this.getFiredShot().getBulletType();
			
			double posX = getX();
			double posY = getY();
			double posZ = getZ();
			
			// Movement dampening variables
			float drag = 0.99F;
			float gravity = 0.02F;
			// If the bullet is in water, spawn particles and increase the drag
			if(isInWater())
			{
				if (world.isClientSide())
				{
					for(int i = 0; i < 4; i++)
					{
						float bubbleMotion = 0.25F;
						world.addParticle(ParticleTypes.BUBBLE, posX - motionX * bubbleMotion,
							posY - motionY * bubbleMotion, posZ - motionZ * bubbleMotion, motionX, motionY, motionZ);
					}
				}
				drag = 0.8F;
			}
			
			motionX *= drag;
			motionY *= drag;
			motionZ *= drag;
			motionY -= gravity * type.fallSpeed;
			
			// Apply motion
			this.setPos(posX + motionX, posY + motionY, posZ + motionZ);
			
			// Recalculate the angles from the new motion
			float motionXZ = Mth.sqrt((float)(motionX * motionX + motionZ * motionZ));
			setYRot((float)((Math.atan2(motionX, motionZ) * 180D) / 3.1415927410125732D));
			setXRot((float)((Math.atan2(motionY, motionXZ) * 180D) / 3.1415927410125732D));
			// Reset the range of the angles
			for(; getXRot() - xRotO < -180F; xRotO -= 360F)
			{
			}
			for(; getXRot() - xRotO >= 180F; xRotO += 360F)
			{
			}
			for(; getYRot() - yRotO < -180F; yRotO -= 360F)
			{
			}
			for(; getYRot() - yRotO >= 180F; yRotO += 360F)
			{
			}
			setXRot(xRotO + (getXRot() - xRotO) * 0.2F);
			setYRot(yRotO + (getYRot() - yRotO) * 0.2F);
			
			
			if(world.isClientSide())
			{
				onUpdateClient();
				return;
			}
			
			
			if(FlansMod.DEBUG)
				((ServerLevel)world).addFreshEntity(new EntityDebugVector(world, new Vector3f(posX, posY, posZ),
						new Vector3f(motionX, motionY, motionZ), 20));
			
			// Check the fuse to see if the bullet should explode
			ticksInAir++;
			if(ticksInAir > type.fuse && type.fuse > 0 && !isRemoved())
			{
				discard();
			}
			
			if(tickCount > bulletLife)
			{
				discard();
			}
			
			if(isRemoved())
				return;
			
			Vector3f origin = new Vector3f(posX, posY, posZ);
			Vector3f motion = new Vector3f(motionX, motionY, motionZ);
			
			if(!world.isClientSide())
			{
				Entity ignore = shot.getPlayerOptional().isPresent() ? shot.getPlayerOptional().get() : shot.getShooterOptional().orElse(null);
				Integer ping = 0;
				
				List<BulletHit> hits = FlansModRaytracer.Raytrace(world, ignore, ticksInAir > 20, this, origin, motion, ping, 0f);
				
				// We hit something
				if(!hits.isEmpty())
				{
					for(BulletHit bulletHit : hits)
					{
						Vector3f hitPos = new Vector3f(origin.x + motion.x * bulletHit.intersectTime,
								origin.y + motion.y * bulletHit.intersectTime,
								origin.z + motion.z * bulletHit.intersectTime);

						currentPenetratingPower = ShotHandler.OnHit(world, hitPos, motion, shot, bulletHit, currentPenetratingPower);
						if (currentPenetratingPower <= 0f)
						{
							ShotHandler.onDetonate(world, shot, hitPos);
							discard();
							break;
						}
					}
				}
			}
			//TODO Client homing fix
			// Apply homing action
			if(lockedOnTo != null)
			{
				double dX = lockedOnTo.getX() - posX;
				double dY = lockedOnTo.getY() - posY;
				double dZ = lockedOnTo.getZ() - posZ;
				double dXYZ = dX * dX + dY * dY + dZ * dZ;
				
				Vector3f relPosVec = new Vector3f(dX, dY, dZ);
				float angle = Math.abs(Vector3f.angle(motion, relPosVec));
				
				double lockOnPull = (angle) * type.lockOnForce;
				
				lockOnPull = lockOnPull * lockOnPull;
				
				motionX *= 0.95f;
				motionY *= 0.95f;
				motionZ *= 0.95f;
				
				motionX += lockOnPull * dX / dXYZ;
				motionY += lockOnPull * dY / dXYZ;
				motionZ += lockOnPull * dZ / dXYZ;
			}
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			super.discard();
		}
	}
	
	private void onUpdateClient()
	{
		// Particles
		if(shot.getBulletType().trailParticles)
		{
			spawnParticles();
		}
		
		if(!playedFlybySound && FlansMod.proxy.isWithinDistanceOfLocalPlayer(this, 5.0D))
		{
			playedFlybySound = true;
			FlansMod.proxy.playFlybySound(this, random);
		}
	}
	
	private void spawnParticles()
	{
		double dX = (getX() - xo) / 10;
		double dY = (getY() - yo) / 10;
		double dZ = (getZ() - zo) / 10;
		
		float spread = 0.1F;
		for(int i = 0; i < 10; i++)
		{
			FlansMod.proxy.spawnParticle(shot.getBulletType().trailParticleType, world,
					xo + dX * i + random.nextGaussian() * spread, yo + dY * i + random.nextGaussian() * spread,
					zo + dZ * i + random.nextGaussian() * spread);
			// TODO: [1.12] once again, render distance
			
			//if (particle != null && Minecraft.getInstance().options.fancyGraphics)
			//	particle.renderDistanceWeight = 100D;
			// world.addFreshEntity(particle);
		}
	}
	
	@Override
	public void addAdditionalSaveData(ValueOutput output)
	{
		if(shot == null)
			return;
		output.putString("type", shot.getBulletType().shortName);
		FireableGun gun = shot.getFireableGun();
		//this data will only be present and saved on the server side
		if (gun != null)
		{
			ValueOutput fireablegun = output.child("fireablegun");
			fireablegun.putInt("infotype", gun.getInfoType().shortName.hashCode());
			fireablegun.putFloat("spread", gun.getGunSpread());
			fireablegun.putFloat("speed", gun.getBulletSpeed());
			fireablegun.putFloat("damage", gun.getDamage());
			fireablegun.putFloat("vehicledamage", gun.getDamageAgainstVehicles());
		
			shot.getPlayerOptional().ifPresent((ServerPlayer player) -> 
			{
				output.putString("player", player.getUUID().toString());
			});
			
			shot.getShooterOptional().ifPresent((Entity shooter) -> 
			{
				output.putString("shooter", shooter.getUUID().toString());
			});
			
		}
	}
	
	@Override
	public void readAdditionalSaveData(ValueInput input)
	{
		FireableGun fireablegun = null;
		String shortName = input.getStringOr("type", "");
		BulletType type = BulletType.getBullet(shortName);
		this.entityData.set(BULLET_TYPE, shortName);
		
		java.util.Optional<ValueInput> gunOpt = input.child("fireablegun");
		if(gunOpt.isPresent())
		{
			ValueInput gun = gunOpt.get();
			fireablegun = new FireableGun(InfoType.getType(gun.getIntOr("infotype", 0)), gun.getFloatOr("damage", 0F), gun.getFloatOr("vehicledamage", 0F), gun.getFloatOr("spread", 0F), gun.getFloatOr("speed", 0F), EnumSpreadPattern.circle);
		}
		
		if(input.getString("player").isPresent())
		{
			try
			{
				playeruuid = UUID.fromString(input.getStringOr("player", ""));
				checkforuuids = true;
			}
			catch(IllegalArgumentException ignored)
			{
			}
		}

		if(input.getString("shooter").isPresent())
		{
			try
			{
				shooteruuid = UUID.fromString(input.getStringOr("shooter", ""));
				checkforuuids = true;
			}
			catch(IllegalArgumentException ignored)
			{
			}
		}
		
		shot = new FiredShot(fireablegun, type);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount)
	{
		return false;
	}
	
	@Override
	public boolean isOnFire()
	{
		return false;
	}
	
	@Override
	public boolean isPushable()
	{
		return false;
	}
	
	public FiredShot getFiredShot()
	{
		if (shot == null)
		{
			//we dont have this object, therefore we are on the client side and need to construct it
			shot = new FiredShot(null, BulletType.getBullet(this.entityData.get(BULLET_TYPE)));
		}
		return shot;
	}
}
