package com.flansmod.common.guns;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.FlansModExplosion;
import com.flansmod.common.RotatedAxes;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.network.PacketFlak;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.util.BlockUtil;
import com.flansmod.common.vector.Vector3f;

public class EntityGrenade extends EntityShootable
{
	public GrenadeType type;
	
	private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> PLAYER_ID = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> THROWER_ID = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.FLOAT);
	
	/**
	 * Contains the player who is responsible for the thrown grenade
	 */
	private Optional<Player> player = Optional.empty();
	
	/**
	 * The Entity who has thrown the grenade
	 */
	private Optional<Entity> thrower = Optional.empty();
	
	/**
	 * This is to avoid players grenades teamkilling after they switch team
	 */
	public Team teamOfThrower;
	/**
	 * Yeah, I want my grenades to have fancy physics
	 */
	public RotatedAxes axes = new RotatedAxes();
	public Vector3f angularVelocity = new Vector3f(0F, 0F, 0F);
	public float prevRotationRoll = 0F;
	/**
	 * Set to the smoke amount when the grenade detonates and decremented every tick after that
	 */
	public int smokeTime = 0;
	/**
	 * Set to true when smoke grenade detonates
	 */
	public boolean smoking = false;
	/**
	 * Set to true when a sticky grenade sticks. Impedes further movement
	 */
	public boolean stuck = false;
	/**
	 * Stores the position of the block this grenade is stuck to. Used to determine when to unstick
	 */
	public int stuckToX, stuckToY, stuckToZ;
	/**
	 * Stop repeat detonations
	 */
	public boolean detonated = false;
	/**
	 * For deployable bags
	 */
	public int numUsesRemaining = 0;
	
	private double motionX, motionY, motionZ;
	
		public EntityGrenade(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityGrenade(Level w)
	{
		this(ModEntities.GRENADE, w);
		this.world = level();
	}
	
	/**
	 * General constructor. Example usecase: grenades spawned via console command
	 * 
	 * @param w             Level in which the grenade will spawn in
	 * @param pos           Position the grenade will spawn at
	 * @param g             GrenadeType of the grenade
	 * @param rotationPitch Pitch of the direction the grenade will fly
	 * @param rotationYaw   Yaw of the direction the grenade will fly
	 */
	public EntityGrenade(Level w, Vector3f pos, GrenadeType g, float rotationPitch, float rotationYaw)
	{
		this(w);
		setPos(pos.getX(), pos.getY(), pos.getZ());
		type = g;
		numUsesRemaining = type.numUses;
		this.entityData.set(TYPE, type.shortName);

		//Set the grenade to be facing the way the Pitch and Yaw variables define
		axes.setAngles(rotationYaw + 90F, g.spinWhenThrown ? rotationPitch : 0F, 0F);
		this.setYRot(g.spinWhenThrown ? rotationYaw + 90F : 0F);
		this.setXRot(rotationPitch);
		//Give the grenade velocity in the direction the player is looking
		float speed = 0.5F * type.throwSpeed;
		motionX = axes.getXAxis().x * speed;
		motionY = axes.getXAxis().y * speed;
		motionZ = axes.getXAxis().z * speed;
		this.entityData.set(YAW, axes.getYaw());
		this.entityData.set(PITCH, axes.getPitch());
		if(type.spinWhenThrown)
			angularVelocity = new Vector3f(0F, 0F, 10F);
		if(type.throwSound != null)
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.throwSound, true);
	}
	
	/**
	 * General constructor for entitys throwing grenades. This should not be used when a player throws the grenade
	 * 
	 * @param entity Entity throwing the grenade
	 * @param g      GrenadeType of the grenade
	 */
	public EntityGrenade(LivingEntity entity, GrenadeType g)
	{
		this(entity.level(), new Vector3f(entity.position().add(new Vec3(0, entity.getEyeHeight(), 0))), g, entity.getXRot(), entity.getYRot());
		this.thrower = Optional.of(entity);
	}
	
	/**
	 * When a player throws a grenade directly this constructor should be used
	 * 
	 * @param player Player throwing the grenade
	 * @param g      GrenadeType of the grenade
	 */
	public EntityGrenade(Player player, GrenadeType g)
	{
		this((LivingEntity)player, g);
		this.player = Optional.of(player);
	}
	
	/**
	 * Constructor for grenades thrown where a player and/or a entity can be associated with.
	 * E.g. mecha using a grenade launcher. In this case the 'entity' is the mecha and the 'player' the player controlling the mecha
	 * 
	 * @param w             Level in which the grenade will spawn in
	 * @param pos           Position the grenade will spawn at
	 * @param g             GrenadeType of the grenade
	 * @param rotationPitch Pitch of the direction the grenade will fly
	 * @param rotationYaw   Yaw of the direction the grenade will fly
	 * @param player        The player that is responsible for throwing the grenade
	 * @param entity        The entity throwing the grenade. Can be the same as 'player'
	 */
	public EntityGrenade(Level w, Vector3f pos, GrenadeType g, float rotationPitch, float rotationYaw, Optional<Player> player, Optional<Entity> entity)
	{
		this(w, pos, g, rotationPitch, rotationYaw);
		this.thrower = entity;
		this.player = player;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(TYPE, "");
		builder.define(PLAYER_ID, -1);
		builder.define(THROWER_ID, -1);
		builder.define(YAW, 0F);
		builder.define(PITCH, 0F);
	}
	
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		if(key == TYPE)
		{
			type = GrenadeType.getGrenade(entityData.get(TYPE));
			axes.setAngles(entityData.get(YAW), entityData.get(PITCH), 0F);
		}
		else if(key == PLAYER_ID)
		{
			Entity ent = world.getEntity(entityData.get(PLAYER_ID));
			player = ent instanceof Player ? Optional.of((Player) ent) : Optional.empty();
		}
		else if(key == THROWER_ID)
		{
			thrower = Optional.ofNullable(world.getEntity(entityData.get(THROWER_ID)));
		}
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		//Quiet despawning
		if(type == null || (type.despawnTime > 0 && tickCount > type.despawnTime))
		{
			detonated = true;
			discard();
			return;
		}
		
		double posX = getX();
		double posY = getY();
		double posZ = getZ();
		
		//Visuals
		if(world.isClientSide())
		{
			if(type.trailParticles)
			{
				double dX = (posX - xo) / 10;
				double dY = (posY - yo) / 10;
				double dZ = (posZ - zo) / 10;
				for(int i = 0; i < 10; i++)
				{
					Particle particle = FlansModClient.getParticle(type.trailParticleType, world, xo + dX * i, yo + dY * i, zo + dZ * i);
					// TODO: [1.12] Particles
					//if(particle != null && Minecraft.getInstance().options.fancyGraphics)
					//	particle.renderDistanceWeight = 100D;
					//world.addFreshEntity(particle);
				}
			}
			
			
		}
		
		//Smoke
		if(smoking)
		{
			//Send flak packet to spawn particles
			FlansMod.getPacketHandler().sendToAllAround(new PacketFlak(posX, posY, posZ, 50, type.smokeParticleType), posX, posY, posZ, 30, GunUtil.getDimensionId(world));
			//
			List<LivingEntity> list = world.getEntities(EntityTypeTest.forClass(LivingEntity.class), getBoundingBox().inflate(type.smokeRadius, type.smokeRadius, type.smokeRadius), entity -> true);
			for(LivingEntity entity : list)
			{
				if(entity.distanceToSqr(this) < type.smokeRadius * type.smokeRadius)
				{
					//Do some checks first
					boolean smokeThem = true;
					for(int i = 0; i < EquipmentSlot.values().length; i++)
					{
						//If any currently equipped item has smoke protection (gas masks), stop the effects
						ItemStack stack = entity.getItemBySlot(EquipmentSlot.values()[i]);
						if(stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemTeamArmour)
						{
							if(((ItemTeamArmour)stack.getItem()).type.smokeProtection)
								smokeThem = false;
						}
					}
					
					if(smokeThem)
						for(MobEffectInstance effect : type.smokeEffects)
							entity.addEffect(effect);
				}
			}
			
			smokeTime--;
			if(smokeTime == 0)
				discard();
		}
		
		//Detonation conditions
		if(!world.isClientSide())
		{
			if(tickCount > type.fuse && type.fuse > 0)
				detonate();
			//If this grenade has a proximity trigger, check for living entities within it's range
			if(type.livingProximityTrigger > 0 || type.driveableProximityTrigger > 0)
			{
				float checkRadius = Math.max(type.livingProximityTrigger, type.driveableProximityTrigger);
				List<Entity> list = world.getEntities(this, getBoundingBox().inflate(checkRadius, checkRadius, checkRadius), entity -> true);
				for(Object obj : list)
				{
					if(obj == thrower && tickCount < 10)
						continue;
					if(obj instanceof LivingEntity && distanceToSqr((Entity)obj) < type.livingProximityTrigger * type.livingProximityTrigger)
					{
						//If we are in a gametype and both thrower and triggerer are playing, check for friendly fire
						if(TeamsManager.getInstance() != null && TeamsManager.getInstance().currentRound != null && obj instanceof ServerPlayer && player.isPresent())
						{
							if(!TeamsManager.getInstance().currentRound.gametype.playerAttacked((ServerPlayer)obj, new EntityDamageSourceFlan(type.shortName, this, player.get(), type)))
								continue;
						}
						if(type.damageToTriggerer > 0)
							((LivingEntity)obj).hurt(getGrenadeDamage(), type.damageToTriggerer);
						detonate();
						break;
					}
					if(obj instanceof EntityDriveable && distanceToSqr((Entity)obj) < type.driveableProximityTrigger * type.driveableProximityTrigger)
					{
						if(type.damageToTriggerer > 0)
							((EntityDriveable)obj).hurt(getGrenadeDamage(), type.damageToTriggerer);
						detonate();
						break;
					}
				}
			}
		}
		
		//If the block we were stuck to is gone, unstick
		if(stuck && world.isEmptyBlock(new BlockPos(stuckToX, stuckToY, stuckToZ)))
			stuck = false;
		
		//Physics and motion (Don't move if stuck)
		if(!stuck && !type.stickToThrower)
		{
			yRotO = axes.getYaw();
			xRotO = axes.getPitch();
			prevRotationRoll = axes.getRoll();
			if(angularVelocity.lengthSquared() > 0.00000001F)
				axes.rotateLocal(angularVelocity.length(), angularVelocity.normalise(null));
			
			Vector3f posVec = new Vector3f(posX, posY, posZ);
			Vector3f motVec = new Vector3f(motionX, motionY, motionZ);
			Vector3f nextPosVec = Vector3f.add(posVec, motVec, null);
			
			//Raytrace the motion of this grenade
			HitResult hit = world.clip(new ClipContext(posVec.toVec3(), nextPosVec.toVec3(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			//If we hit block
			if(hit != null && hit.getType() == HitResult.Type.BLOCK)
			{
				BlockHitResult blockHit = (BlockHitResult)hit;
				//Get block material
				BlockState state = world.getBlockState(blockHit.getBlockPos());
				
				//If this grenade detonates on impact, do so
				if(type.explodeOnImpact)
					detonate();
					
					//If we hit glass and can break it, do so
				else if(type.breaksGlass && state.is(BlockTags.IMPERMEABLE) && TeamsManager.canBreakGlass)
				{
					if(!world.isClientSide())
					{
						ServerLevel worldServer = (ServerLevel)world;
						BlockUtil.destroyBlock(worldServer, blockHit.getBlockPos(), player.orElse(null), false);
					}
				}
				
				//If this grenade does not penetrate blocks, hit the block instead
				//The grenade cannot bounce if it detonated on impact, so hence the "else" condition
				else if(!type.penetratesBlocks)
				{
					Vector3f hitVec = new Vector3f(blockHit.getLocation());
					//Motion of the grenade pre-hit
					Vector3f preHitMotVec = Vector3f.sub(hitVec, posVec, null);
					//Motion of the grenade post-hit
					Vector3f postHitMotVec = Vector3f.sub(motVec, preHitMotVec, null);
					
					//Reflect postHitMotVec based on side hit
					Direction sideHit = blockHit.getDirection();
					switch(sideHit)
					{
						case UP:
						case DOWN:
							postHitMotVec.setY(-postHitMotVec.getY());
							break;
						case EAST:
						case WEST:
							postHitMotVec.setX(-postHitMotVec.getX());
							break;
						case NORTH:
						case SOUTH:
							postHitMotVec.setZ(-postHitMotVec.getZ());
							break;
						//TODO : Check the compass directions are correct
					}
					
					//Calculate the time interval spent post reflection
					float lambda = Math.abs(motVec.lengthSquared()) < 0.00000001F ? 1F : postHitMotVec.length() / motVec.length();
					//Scale the post hit motion by the bounciness of the grenade
					postHitMotVec.scale(type.bounciness / 2);
					
					//Move the grenade along the new path including reflection
					posX += preHitMotVec.x + postHitMotVec.x;
					posY += preHitMotVec.y + postHitMotVec.y;
					posZ += preHitMotVec.z + postHitMotVec.z;
					
					//Set the motion
					motionX = postHitMotVec.x / lambda;
					motionY = postHitMotVec.y / lambda;
					motionZ = postHitMotVec.z / lambda;
					
					//Reset the motion vector
					motVec = new Vector3f(motionX, motionY, motionZ);
					
					//Give it a random spin
					float randomSpinner = 90F;
					Vector3f.add(angularVelocity, new Vector3f(random.nextGaussian() * randomSpinner, random.nextGaussian() * randomSpinner, random.nextGaussian() * randomSpinner), angularVelocity);
					//Slow the spin based on the motion
					angularVelocity.scale(motVec.lengthSquared());
					
					//Play the bounce sound
					if(motVec.lengthSquared() > 0.01D)
						playSound(FlansModResourceHandler.getSoundEvent(type.bounceSound), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
					
					//If this grenade is sticky, stick it to the block
					if(type.sticky)
					{
						//Move the grenade to the point of contact
						posX = hitVec.x;
						posY = hitVec.y;
						posZ = hitVec.z;
						//Stop all motion of the grenade
						motionX = motionY = motionZ = 0;
						angularVelocity.set(0F, 0F, 0F);
						
						float yaw = axes.getYaw();
						
						switch(blockHit.getDirection())
						{
							case DOWN:
								axes.setAngles(yaw, 180F, 0F);
								break;
							case UP:
								axes.setAngles(yaw, 0F, 0F);
								break;
							case NORTH:
								axes.setAngles(270F, 90F, 0F);
								axes.rotateLocalYaw(yaw);
								break;
							case SOUTH:
								axes.setAngles(90F, 90F, 0F);
								axes.rotateLocalYaw(yaw);
								break;
							case WEST:
								axes.setAngles(180F, 90F, 0F);
								axes.rotateLocalYaw(yaw);
								break;
							case EAST:
								axes.setAngles(0F, 90F, 0F);
								axes.rotateLocalYaw(yaw);
								break;
						}
						
						//Set the stuck flag on
						stuck = true;
						stuckToX = blockHit.getBlockPos().getX();
						stuckToY = blockHit.getBlockPos().getY();
						stuckToZ = blockHit.getBlockPos().getZ();
					}
				}
			}
			//We didn't hit a block, continue as normal
			else
			{
				posX += motionX;
				posY += motionY;
				posZ += motionZ;
			}
			
			//Update the grenade position
			setPos(posX, posY, posZ);
		}
		
		if(type.stickToThrower)
		{
			if (!thrower.isPresent() || thrower.get().isRemoved() || !(thrower.get() instanceof LivingEntity))
			{
				discard();
			}
			else
			{
				LivingEntity entity = (LivingEntity) thrower.get();
				setPos(entity.getX(), entity.getY(), entity.getZ());
			}
		}
		
		//If throwing this grenade at an entity should hurt them, this bit checks for entities in the way and does so
		//(Don't attack entities when stuck to stuff)
		if(type.damageVsLiving > 0 && !stuck)
		{
			Vector3f motVec = new Vector3f(motionX, motionY, motionZ);
			List<Entity> list = world.getEntities(this, getBoundingBox(), entity -> true);
			for(Object obj : list)
			{
				if(obj == thrower && tickCount < 10 || motVec.lengthSquared() < 0.01D)
					continue;
				if(obj instanceof LivingEntity)
					((LivingEntity)obj).hurt(getGrenadeDamage(), type.damageVsLiving * motVec.lengthSquared() * 3);
			}
		}
		
		//Apply gravity
		motionY -= 9.81D / 400D * type.fallSpeed;
		
		//Temporary fire glitch fix
		if(world.isClientSide())
			extinguishFire();
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float f)
	{
		if(type.detonateWhenShot)
			detonate();
		return type.detonateWhenShot;
	}
	
	public void detonate()
	{
		//Do not detonate before grenade is primed
		if(tickCount < type.primeDelay)
			return;
		
		//Stop repeat detonations
		if(detonated)
			return;
		detonated = true;
		
		//Play detonate sound
		PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.detonateSound, true);
		
		//Explode
		if(!world.isClientSide() && type.explosionRadius > 0.1F)
		{
			new FlansModExplosion(world, this, player, type, getX(), getY(), getZ(), type.explosionRadius, type.fireRadius > 0, type.smokeRadius > 0, type.explosionBreaksBlocks);
		}
		
		//Make fire
		if(type.fireRadius > 0.1F)
		{
			for(float i = -type.fireRadius; i < type.fireRadius; i++)
			{
				for(float j = -type.fireRadius; j < type.fireRadius; j++)
				{
					for(float k = -type.fireRadius; k < type.fireRadius; k++)
					{
						int x = Mth.floor(i + getX());
						int y = Mth.floor(j + getY());
						int z = Mth.floor(k + getZ());
						if(i * i + j * j + k * k <= type.fireRadius * type.fireRadius && world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.AIR && random.nextBoolean())
						{
							{
								world.setBlock(new BlockPos(x, y, z), Blocks.FIRE.defaultBlockState(), 2);
								world.scheduleTick(new BlockPos(x, y, z), Blocks.FIRE, 0);
							}
						}
					}
				}
			}
		}
		
		//Make explosion particles
		if(world.isClientSide())
		{
			for(int i = 0; i < type.explodeParticles; i++)
			{
				world.addParticle(FlansMod.getParticleType(type.explodeParticleType), getX(), getY(), getZ(), random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
			}
		}
		
		//Drop item upon detonation, after explosions and whatnot
		if(!world.isClientSide() && type.dropItemOnDetonate != null)
		{
			ItemStack dropStack = InfoType.getRecipeElement(type.dropItemOnDetonate);
			spawnAtLocation((ServerLevel)world, dropStack, 1.0F);
		}
		
		//Start smoke counter
		if(type.smokeTime > 0)
		{
			smoking = true;
			smokeTime = type.smokeTime;
		}
		else
		{
			discard();
		}
	}
	
	private DamageSource getGrenadeDamage()
	{
		if (player.isPresent())
		{
			return new EntityDamageSourceFlan(type.shortName, this, player.get(), type).setProjectile();
		}
		return new EntityDamageSourceFlan(type.shortName, this, null, type).setProjectile();
	}
	
	@Override
	public void addAdditionalSaveData(ValueOutput output)
	{
		if(type == null)
			return;
		output.putString("Type", type.shortName);
		if(player.isPresent())
			output.putString("Player", player.get().getUUID().toString());
		output.putFloat("RotationYaw", axes.getYaw());
		output.putFloat("RotationPitch", axes.getPitch());
	}
	
	@Override
	public void readAdditionalSaveData(ValueInput input)
	{
		type = GrenadeType.getGrenade(input.getStringOr("Type", ""));
		entityData.set(TYPE, input.getStringOr("Type", ""));
		String playerUUID = input.getStringOr("Player", "");
		if(!playerUUID.isEmpty())
		{
			try
			{
				Entity ent = world.getEntity(java.util.UUID.fromString(playerUUID));
				player = ent instanceof Player ? Optional.of((Player) ent) : Optional.empty();
			}
			catch(IllegalArgumentException ignored)
			{
			}
		}
		setYRot(input.getFloatOr("RotationYaw", 0F));
		setXRot(input.getFloatOr("RotationPitch", 0F));
		axes.setAngles(getYRot(), getXRot(), 0F);
	}
	
	@Override
	public boolean isOnFire()
	{
		return false;
	}
	
	@Override
	public boolean isPickable()
	{
		return !isRemoved() && type.isDeployableBag;
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 pos)
	{
		// Player right clicked on grenade
		//For deployable bags, give player rewards
		if(type.isDeployableBag && !world.isClientSide())
		{
			boolean used = false;
			//Handle healing
			if(type.healAmount > 0 && player.getHealth() < player.getMaxHealth())
			{
				player.heal(type.healAmount);
				FlansMod.getPacketHandler().sendToAllAround(new PacketFlak(player.getX(), player.getY(), player.getZ(), 5, "heart"), player.getX(), player.getY(), player.getZ(), 50F, GunUtil.getDimensionId(world));
				used = true;
			}
			//Handle potion effects
			for(MobEffectInstance effect : type.potionEffects)
			{
				player.addEffect(effect);
				used = true;
			}
			//Handle ammo
			if(type.numClips > 0 && !player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() instanceof ItemGun)
			{
				GunType gun = ((ItemGun)player.getMainHandItem().getItem()).GetType();
				if(gun.ammo.size() > 0)
				{
					ShootableType bulletToGive = gun.ammo.get(0);
					int numToGive = Math.min(bulletToGive.maxStackSize, type.numClips * gun.numAmmoItemsInGun);
					if(player.getInventory().add(new ItemStack(bulletToGive.item, numToGive)))
					{
						used = true;
					}
				}
			}
			//If the bag is all used up, get rid of it
			if(used)
			{
				numUsesRemaining--;
				if(numUsesRemaining <= 0)
					discard();
			}
		}
		return InteractionResult.SUCCESS;
	}
}
