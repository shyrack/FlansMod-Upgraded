package com.flansmod.apocalypse.common.entity;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.guns.EntityDamageSourceFlan;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.network.PacketPlaySound;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntitySkullBoss extends Mob
{
	protected Level world;
    protected static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(EntitySkullBoss.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> LOOKING_AT_ENTITY = SynchedEntityData.defineId(EntitySkullBoss.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossInfo = (ServerBossEvent)new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
    private int timeInCurrentMode = 0;
    private UseAnim prevAction = UseAnim.IDLE;
    
    public enum UseAnim
    {
    	IDLE,
    	LAUGH,
    	SPAWN_DRONES,
    	SHOOT_TNT,
    }
    
    private static final int kLaughTicks = 80;
    private static final int kNumLaughs = 6;
    private static final float kLaughContributionOffset = 1.0f / ((float)kNumLaughs + 1);
    private static final float kLaughContributionLength = 2.0f * kLaughContributionOffset;
    
    // in degrees
    public float GetSpawnSpin(float partialTicks)
    {
    	if(GetCurrentAction() == UseAnim.SPAWN_DRONES)
    	{
    		float parametric = (float)(timeInCurrentMode + partialTicks) / (float)kLaughTicks;
    		float smoothstep =  parametric * parametric * (3 - 2 * parametric);

    		return smoothstep * 720f;    		
    	}
    	return 0.0f;
    }
    
    public float GetLaughFactor(float partialTicks)
    {
    	if(GetCurrentAction() == UseAnim.LAUGH || GetCurrentAction() == UseAnim.SHOOT_TNT)
    	{
    		float result = 0.0f;
    		float parametric = (float)(timeInCurrentMode + partialTicks) / (float)kLaughTicks;
    		for(int i = 0; i < kNumLaughs; i++)
    		{
    			if(kLaughContributionOffset * i <= parametric && parametric <= kLaughContributionOffset * (i + 2))
    			{
    				result += Math.sin(Math.PI * (parametric - kLaughContributionOffset * i) / kLaughContributionLength);
    			}
    		}
    		
    		return result;    		
    	}
    	return 0.0f;
    }
	
	public EntitySkullBoss(EntityType<? extends Mob> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntitySkullBoss(Level worldIn)
	{
		this(ModEntities.SKULL_BOSS, worldIn);
		this.world = level();

		setNoGravity(true);
		setPersistenceRequired();
		setNoAi(true);
		// TODO APOCALYPSE: ignoreFrustumCheck no longer exists
	}
	
	public static AttributeSupplier.Builder createAttributes()
	{
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1024.0D);
	}
	
    /**
     * Add the given player to the list of players tracking this entity. For instance, a player may track a boss in
     * order to view its associated boss bar.
     */
	@Override
    public void startSeenByPlayer(ServerPlayer player)
    {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    /**
     * Removes the given player from the list of players tracking this entity. See {@link Entity#addTrackingPlayer} for
     * more information on tracking.
     */
	@Override
    public void stopSeenByPlayer(ServerPlayer player)
    {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }
	
	private void SwitchAction(UseAnim action)
	{
		entityData.set(ACTION, (byte)action.ordinal());
		timeInCurrentMode = 0;
	}
	
	public UseAnim GetCurrentAction()
	{
		return UseAnim.values()[entityData.get(ACTION)];
	}
	
	@Override 
	public void tick()
	{
		super.tick();
		
		timeInCurrentMode++;
		this.fallDistance = 0f;
		
		UseAnim currentAction = GetCurrentAction();
		if(currentAction != prevAction)
		{
			// For clients, we just get a data update, so check here for a change
			timeInCurrentMode = 0;
			prevAction = currentAction;
		}
		
		if(!world.isClientSide()) 
		{
			float lerpSpeed = 0.1f;
			float targetYHeight = 180f + (float)Math.sin(tickCount / 200f) * 40f;
			this.setDeltaMovement(this.getDeltaMovement().x - this.getX() * lerpSpeed / 20f, this.getDeltaMovement().y, this.getDeltaMovement().z - this.getZ() * lerpSpeed / 20f);
			this.setDeltaMovement(this.getDeltaMovement().x, (targetYHeight - this.getY()) * lerpSpeed / 20f, this.getDeltaMovement().z);
			
			this.move(MoverType.SELF, this.getDeltaMovement());
									
			switch(currentAction)
			{
				case IDLE:
				{
					if(timeInCurrentMode >= 20)	// After 1s in idle, choose another mode
					{
						switch(random.nextInt(3))
						{
							case 1: SwitchAction(UseAnim.LAUGH); break;
							case 2: SwitchAction(UseAnim.SPAWN_DRONES); break;
							case 0: SwitchAction(UseAnim.SHOOT_TNT); break;
							
							default: SwitchAction(UseAnim.SHOOT_TNT); break;
						}
					}
					break;
				}
				case LAUGH:	
				{
					if(timeInCurrentMode == 2)
					{
						PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), "skullboss_laugh", false);
						
					}
					
					if(timeInCurrentMode % 5 == 0)
					{
						world.explode(this, getX() + random.nextGaussian() * 10d, getY() + random.nextGaussian() * 10d, getZ() + random.nextGaussian() * 10d, 10f, Level.ExplosionInteraction.NONE);
					}
					
					if(timeInCurrentMode >= kLaughTicks)
					{						
						SwitchAction(UseAnim.IDLE);
					}
					break;
				}
				case SPAWN_DRONES:
				{
					if(timeInCurrentMode == 2)
					{
						PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), "skullboss_spawn", false);
						
						EntitySkullDrone drone = new EntitySkullDrone(world);
						drone.setPos(getX(), getY() - 5f, getZ());
						ItemStack loadedGun = FlansModApocalypse.getLootGenerator().getRandomLoadedGun(new java.util.Random(), false);
						drone.setItemInHand(InteractionHand.MAIN_HAND, loadedGun);
						drone.setItem(0, ((ItemGun)loadedGun.getItem()).getBulletItemStack(loadedGun, 0).copy());
						
						int lookingAtID = entityData.get(LOOKING_AT_ENTITY);
						if(lookingAtID != 0)
						{
							Entity target = world.getEntity(lookingAtID);
							if(target != null)
								drone.SetTarget(target);
						}
						((ServerLevel)world).addFreshEntity(drone);
						
					}
					
					if(timeInCurrentMode >= kLaughTicks)
					{
						SwitchAction(UseAnim.IDLE);
					}
					break;
				}
				case SHOOT_TNT:
				{
					if(timeInCurrentMode % 20 == 0)
					{
						int lookingAtID = entityData.get(LOOKING_AT_ENTITY);
						if(lookingAtID != 0)
						{
							Entity target = world.getEntity(lookingAtID);
							if(target != null)
							{
								PrimedTnt tnt = new PrimedTnt(EntityType.TNT, world);
								Vec3 dPos = new Vec3(
										target.getX() - getX(),
										target.getY() - getY(),
										target.getZ() - getX());
								
								double distance = dPos.length();
								dPos = dPos.normalize();
								dPos = dPos.scale(2d);
								
								tnt.setNoGravity(true);
								tnt.setPos(getX() + dPos.x, getY() + dPos.y, getZ() + dPos.z);
								tnt.setDeltaMovement(
										(target.getX() - getX()) / 40d, 
										(target.getY() - getY()) / 40d, 
										(target.getZ() - getZ()) / 40d);
								((ServerLevel)world).addFreshEntity(tnt);
								
								PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), "fire.ignite", true);

							}
						}
					}
					
					if(timeInCurrentMode >= kLaughTicks)
					{
						SwitchAction(UseAnim.IDLE);
					}
					break;
				}
			}
		}
		
		int lookingAtID = entityData.get(LOOKING_AT_ENTITY);
		if(lookingAtID != 0)
		{
			Entity entity = world.getEntity(lookingAtID);
			if(entity == null || entity.isRemoved())
			{
				if(!world.isClientSide())
					entityData.set(LOOKING_AT_ENTITY, 0);
			}
			else if(!world.isClientSide())
			{
				double dX = entity.getX() - getX();
				double dY = entity.getY() - getY();
				double dZ = entity.getZ() - getZ();
				
				float targetYaw = (float)(Math.atan2(dZ, dX) * 180d / Math.PI);
				float targetPitch = (float)(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180d / Math.PI);
				
				float deltaYaw = targetYaw - getYRot();
				float deltaPitch = targetPitch - getXRot();
				
				while(deltaYaw > 180f)
					deltaYaw -= 360f;
				while(deltaYaw < -180f)
					deltaYaw += 360f;
				
				setYRot(getYRot() + deltaYaw / 20f);
				setXRot(getXRot() + deltaPitch / 20f);
			}
		}
		
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
	}
	
	public void SetTarget(Entity target)
	{
		entityData.set(LOOKING_AT_ENTITY, target == null ? 0 : target.getId());
	}
	
	@Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount)
    {
		if(source.is(DamageTypeTags.IS_EXPLOSION))
			return false;
		if(source.getEntity() instanceof EntitySkullDrone || 
				source instanceof EntityDamageSourceFlan && ((EntityDamageSourceFlan)source).getCausedPlayer() == null)
		{
			return false; 
		}
		// Hard cap because some Flan's Mod configs can get a bit out of hand
		if(amount > 99f)
			amount = 99f;
		
		switch(world.getDifficulty())
		{
			case HARD:
				amount *= 0.25f;
				break;
			case NORMAL:
				amount *= 0.5f;
				break;
			default:
			case EASY:
			case PEACEFUL:
				break;
		}
		
		super.hurtServer(level, source, amount);
		if(!world.isClientSide())
		{
			Entity sourceEntity = source.getEntity();
			if(sourceEntity != null)
				entityData.set(LOOKING_AT_ENTITY, sourceEntity.getId());
		}
		return true;
    }
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) 
	{
		super.defineSynchedData(builder);
		builder.define(ACTION, (byte)0);
		builder.define(LOOKING_AT_ENTITY, 0);
		
		PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), "skullboss_spawn", true);

	}

	@Override
	public void readAdditionalSaveData(ValueInput input) 
	{
		super.readAdditionalSaveData(input);
		entityData.set(ACTION, input.getByteOr("Action", (byte)0));
		entityData.set(LOOKING_AT_ENTITY, input.getIntOr("LookingAt", 0));
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) 
	{
		super.addAdditionalSaveData(output);
		output.putByte("Action", entityData.get(ACTION));
		output.putInt("LookingAt", entityData.get(LOOKING_AT_ENTITY));
	}
	
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean maybeBlock)
	{
		spawnAtLocation(level, new ItemStack(Items.GOLDEN_APPLE, random.nextInt(4) + 1));
		spawnAtLocation(level, new ItemStack(Items.TOTEM_OF_UNDYING));
		// Lots of gunpowder
		spawnAtLocation(level, new ItemStack(Items.GUNPOWDER, random.nextInt(32) + 1));
		spawnAtLocation(level, new ItemStack(Items.GUNPOWDER, random.nextInt(32) + 1));
		spawnAtLocation(level, new ItemStack(Items.GUNPOWDER, random.nextInt(32) + 1));
		spawnAtLocation(level, new ItemStack(FlansMod.gunpowderBlockItem, random.nextInt(4) + 1));
		spawnAtLocation(level, new ItemStack(FlansMod.gunpowderBlockItem, random.nextInt(4) + 1));
		spawnAtLocation(level, new ItemStack(FlansMod.gunpowderBlockItem, random.nextInt(4) + 1));
		
		if(FlansModApocalypse.nukraniumGauntlet != null)
		{
			ItemStack gauntlet = new ItemStack(FlansModApocalypse.nukraniumGauntlet);
			// TODO APOCALYPSE: 1.12.2 gave a 50% chance of a random enchantment; EnchantmentHelper.addRandomEnchantment no longer exists
			spawnAtLocation(level, gauntlet);
		}
	}
}
