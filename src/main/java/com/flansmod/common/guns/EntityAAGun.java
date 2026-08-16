package com.flansmod.common.guns;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.network.PacketAAGunAngles;
import com.flansmod.common.network.PacketMGFire;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.vector.Vector3f;

public class EntityAAGun extends Entity
{
	protected Level world;
	
	private int health;
	private int shootDelay;
	/**
	 * Gun angles
	 */
	public float gunYaw, gunPitch;
	/**
	 * Prev gun angles
	 */
	public float prevGunYaw, prevGunPitch;
	public float barrelRecoil[];
	public AAGunType type;
	public Entity towedByEntity;
	public ItemStack[] ammo; // One per barrel
	public int reloadTimer;
	public int currentBarrel; // For cycling through firing each barrel
	public boolean mouseHeld;
	public boolean wasShooting;
	
	//Sentry stuff
	/**
	 * Stops the sentry shooting whoever placed it or their teammates
	 */
	public Player placer = null;
	/**
	 * For getting the placer after a reload
	 */
	public String placerName = null;
	/**
	 * The sentry's current target
	 */
	public Entity target = null;
	/**
	 * How often to check for new targets
	 */
	public static final float targetAcquireInterval = 10;
	
	public int ticksSinceUsed = 0;
	
	private double motionX, motionY, motionZ;
	
	private static final EntityDataAccessor<String> AA_TYPE = SynchedEntityData.defineId(EntityAAGun.class, EntityDataSerializers.STRING);
	
		public EntityAAGun(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityAAGun(Level world)
	{
		this(ModEntities.AA_GUN, world);
		this.world = level();

		gunYaw = 0;
		gunPitch = 0;
		shootDelay = 0;
	}
	
	public EntityAAGun(Level world, AAGunType type1, double d, double d1, double d2, Player p)
	{
		this(world);
		placer = p;
		placerName = p == null ? null : p.getName().getString();
		type = type1;
		initType();
		this.entityData.set(AA_TYPE, type.shortName);
		setPos(d, d1, d2);
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(AA_TYPE, "");
	}
	
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		if(key == AA_TYPE)
		{
			type = AAGunType.getAAGun(entityData.get(AA_TYPE));
			initType();
		}
	}
	
	public void initType()
	{
		health = type.health;
		barrelRecoil = new float[type.numBarrels];
		ammo = new ItemStack[type.numBarrels];
		for(int i = 0; i < type.numBarrels; i++)
		{
			ammo[i] = ItemStack.EMPTY.copy();
		}
	}
	
	@Override
	public void playerTouch(Player par1EntityPlayer)
	{
		
	}
	
	@Override
	public void push(Entity entity)
	{
		//if(entity != riddenByEntity)
		//super.push(entity);
	}
	
	@Override
	public boolean isPushable()
	{
		return false;
	}
	
	public void setMouseHeld(boolean held)
	{
		mouseHeld = held;
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damagesource, float i)
	{
		if(damagesource.getMsgId().equals("player"))
		{
			Entity player = damagesource.getEntity();
			if(player.getVehicle() == this || player.getPassengers().contains(this))
			{
			}
			else if(!getPassengers().isEmpty())
			{
				return getPassengers().get(0).hurtServer(level, damagesource, i);
			}
			else if(TeamsManager.canBreakGuns)
			{
				discard();
			}
		}
		else
		{
			//setBeenAttacked();
			health -= i;
			if(!world.isClientSide() && health <= 0)
				discard();
		}
		return true;
	}
	
	public Vec3 rotate(double x, double y, double z)
	{
		double cosYaw = Math.cos(180F - gunYaw * 3.14159265F / 180F);
		double sinYaw = Math.sin(180F - gunYaw * 3.14159265F / 180F);
		double cosPitch = Math.cos(gunPitch * 3.14159265F / 180F);
		double sinPitch = Math.sin(gunPitch * 3.14159265F / 180F);
		
		double newX = x * cosYaw + (y * sinPitch + z * cosPitch) * sinYaw;
		double newY = y * cosPitch - z * sinPitch;
		double newZ = -x * sinYaw + (y * sinPitch + z * cosPitch) * cosYaw;
		
		return new Vec3(newX, newY, newZ);
	}
	
	@Override
	public boolean isPickable()
	{
		return !isRemoved();
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		prevGunYaw = gunYaw;
		prevGunPitch = gunPitch;
		
		ticksSinceUsed++;
		if(TeamsManager.aaLife > 0 && ticksSinceUsed > TeamsManager.aaLife * 20)
		{
			discard();
		}
		
		if(getControllingPassenger() != null)
		{
			ticksSinceUsed = 0;
			gunYaw = getControllingPassenger().getYRot() - 90;
			gunPitch = getControllingPassenger().getXRot();
		}
		
		if(gunPitch > type.bottomViewLimit)
			gunPitch = type.bottomViewLimit;
		if(gunPitch < -type.topViewLimit)
			gunPitch = -type.topViewLimit;
		
		for(int i = 0; i < type.numBarrels; i++)
			barrelRecoil[i] *= 0.9F;
		
		if(shootDelay > 0)
			shootDelay--;
		
		// Sentry stuff
		if(isSentry())
		{
			if(target != null && target.isRemoved())
				target = null;
			//Find a new target if we don't currently have one
			if(target == null && tickCount % targetAcquireInterval == 0)
			{
				target = getValidTarget();
			}
			if(target != null)
			{
				double dX = target.getX() - getX();
				double dY = target.getY() - (getY() + 1.5F);
				double dZ = target.getZ() - getZ();
				
				double distanceToTarget = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
				
				if(distanceToTarget > type.targetRange)
					target = null;
				else
				{
					float newYaw = 180F + (float)Math.atan2(dZ, dX) * 180F / 3.14159F;
					float newPitch = -(float)Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180F / 3.14159F;
					
					float turnSpeed = 0.25F;
					
					gunYaw += (newYaw - gunYaw) * turnSpeed;
					gunPitch += (newPitch - gunPitch) * turnSpeed;
				}
			}
		}
		
		// apply gravity
		
		if(!onGround() && !world.isClientSide())
			motionY -= 9.8D / 400D;
		
		// update motion
		motionX *= 0.5;
		motionZ *= 0.5;
		move(MoverType.SELF, new Vec3(motionX, motionY, motionZ));
		
		if(world.isClientSide() && getControllingPassenger() != null && getControllingPassenger() == Minecraft.getInstance().player)
		{
			checkForShooting();
		}
		
		if(world.isClientSide())
		{
			return;
		}
		
		if(getControllingPassenger() != null && !getControllingPassenger().isAlive())
		{
			ejectPassengers();
		}
		
		// Decrement the reload timer and reload
		if(reloadTimer > 0)
			reloadTimer--;
			//If it is 0 or less, go ahead and reload
		else
		{
			for(int i = 0; i < type.numBarrels; i++)
			{
				if(ammo[i] != null && !ammo[i].isEmpty() && ammo[i].getDamageValue() == ammo[i].getMaxDamage())
				{
					ammo[i] = ItemStack.EMPTY.copy();
					// Scrap metal output?
				}
				if((ammo[i] == null || ammo[i].isEmpty()) && getControllingPassenger() != null && getControllingPassenger() instanceof Player)
				{
					int slot = findAmmo(((Player)getControllingPassenger()));
					if(slot >= 0)
					{
						ammo[i] = ((Player)getControllingPassenger()).getInventory().getItem(slot);
						if(!((Player)getControllingPassenger()).getAbilities().instabuild)
							((Player)getControllingPassenger()).getInventory().removeItem(slot, 1);
						reloadTimer = type.reloadTime;
						PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), 50, GunUtil.getDimensionId(world), type.reloadSound, true);
					}
				}
			}
		}
		
		if(!world.isClientSide() && reloadTimer <= 0 && shootDelay <= 0)
		{
			Boolean shootPlayer = mouseHeld && getControllingPassenger() instanceof ServerPlayer;
			
			if (target != null || shootPlayer)
			{
				ServerPlayer player = shootPlayer ? (ServerPlayer)getControllingPassenger() : null;
				
				for(int j = 0; j < type.numBarrels; j++)
				{
					int ammoSlot = j;
					if(type.shareAmmo)
						ammoSlot = 0;
					if(shootDelay <= 0 && ammo[ammoSlot] != null && !ammo[ammoSlot].isEmpty() && (!type.fireAlternately || type.fireAlternately && currentBarrel == j))
					{
						// Fire
						BulletType bullet = BulletType.getBullet(ammo[ammoSlot].getItem());
						if(shootPlayer)
						{
							if(!player.getAbilities().instabuild)
								ammo[ammoSlot].hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
						} else
						{
							ammo[ammoSlot].setDamageValue(ammo[ammoSlot].getDamageValue() + 1);
						}
						shootDelay = type.shootDelay;
						barrelRecoil[j] = type.recoil;
						
						Vec3 origin = rotate(type.barrelX[currentBarrel] / 16D - type.barrelZ[currentBarrel] / 16D,
								type.barrelY[currentBarrel] / 16D,
								type.barrelX[currentBarrel] / 16D + type.barrelZ[currentBarrel] / 16D).add(getX(), getY(), getZ());
						
						Double radianYaw = Math.toRadians(gunYaw + 90F);
						Double radianPitch = Math.toRadians(gunPitch);
						Vector3f shootingDirection = new Vector3f(-Math.sin(radianYaw), Math.cos(radianYaw)*-Math.sin(radianPitch), Math.cos(radianYaw)*Math.cos(radianPitch));
						
						FireableGun weapon = new FireableGun(type, (float)type.damage, (float)type.accuracy, (float)type.damage, EnumSpreadPattern.circle);
						FiredShot shot = new FiredShot(weapon, bullet, this, player);
						//TODO use Vec3
						ShotHandler.fireGun(world, shot, bullet.numBullets, new Vector3f(origin), shootingDirection);
						
						PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), 50, GunUtil.getDimensionId(world), type.shootSound, true);
					}
				}
				currentBarrel = (currentBarrel + 1) % type.numBarrels;
			}
		}
		if(!world.isClientSide())
		{
			FlansMod.getPacketHandler().sendToAllAround(new PacketAAGunAngles(this), getX(), getY(), getZ(), 50F, GunUtil.getDimensionId(world));
		}
	}
	
	public boolean isSentry()
	{
		return type.targetMobs || type.targetPlayers;
	}
	
	public Entity getValidTarget()
	{
		if(world.isClientSide())
			return null;
		if(placer == null && placerName != null)
			placer = getPlayerByName(placerName);
		for(Entity candidateEntity : world.getEntities(this, getBoundingBox().inflate(type.targetRange, type.targetRange, type.targetRange), entity -> true))
		{
			if((type.targetMobs && candidateEntity instanceof Monster) || (type.targetPlayers && candidateEntity instanceof Player))
			{
				//Check that this entity is actually in range and visible
				if(candidateEntity.distanceToSqr(this) < type.targetRange * type.targetRange)
				{
					if(candidateEntity instanceof Player)
					{
						if(candidateEntity == placer || candidateEntity.getName().getString().equals(placerName))
							continue;
						if(TeamsManager.enabled && TeamsManager.getInstance().currentRound != null && placer != null)
						{
							PlayerData placerData = PlayerHandler.getPlayerData(placer);
							PlayerData candidateData = PlayerHandler.getPlayerData((Player)candidateEntity);
							if(candidateData.team == Team.spectators || candidateData.team == null)
								continue;
							if(!TeamsManager.getInstance().currentRound.gametype.playerCanAttack((ServerPlayer)placer, placerData.team, (ServerPlayer)candidateEntity, candidateData.team))
								continue;
						}
					}
					return candidateEntity;
				}
			}
		}
		return null;
	}
	
	private Player getPlayerByName(String name)
	{
		if(world instanceof ServerLevel)
		{
			for(ServerPlayer player : ((ServerLevel)world).players())
			{
				if(player.getName().getString().equals(name))
					return player;
			}
		}
		return null;
	}
	
	private void checkForShooting()
	{
		//Send a packet!
		if(Minecraft.getInstance().mouseHandler.isLeftPressed() && !wasShooting && !FlansMod.proxy.isScreenOpen())
		{
			FlansMod.getPacketHandler().sendToServer(new PacketMGFire(true));
			wasShooting = true;
		}
		else if(!Minecraft.getInstance().mouseHandler.isLeftPressed() && wasShooting)
		{
			FlansMod.getPacketHandler().sendToServer(new PacketMGFire(false));
			wasShooting = false;
		}
	}
	
	@Override
	public void remove(RemovalReason reason)
	{
		super.remove(reason);
		// Drop gun
		if(world.isClientSide())
			return;
		spawnAtLocation((ServerLevel)world, new ItemStack(type.getItem()));
		// Drop ammo boxes
		for(ItemStack stack : ammo)
		{
			if(stack != null && !stack.isEmpty())
				spawnAtLocation((ServerLevel)world, stack, 0.5F);
		}
	}
	
	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction)
	{
		double x = type.gunnerX / 16D;
		double y = type.gunnerY / 16D;
		double z = type.gunnerZ / 16D;
		
		double cosYaw = Math.cos((-gunYaw / 180D) * 3.1415926535897931D);
		double sinYaw = Math.sin((-gunYaw / 180D) * 3.1415926535897931D);
		double cosPitch = Math.cos((gunPitch / 180D) * 3.1415926535897931D);
		double sinPitch = Math.sin((gunPitch / 180D) * 3.1415926535897931D);
		
		double x2 = x * cosYaw + z * sinYaw;
		double z2 = -x * sinYaw + z * cosYaw;
		
		moveFunction.accept(passenger, getX() + x2, getY() + y, getZ() + z2);
	}
	
	@Override
	public void addAdditionalSaveData(ValueOutput output)
	{
		if(type == null)
			return;
		output.putString("Type", type.shortName);
		output.putInt("Health", health);
		output.putFloat("RotationYaw", getYRot());
		output.putFloat("RotationPitch", getXRot());
		for(int i = 0; i < type.numBarrels; i++)
		{
			if(ammo[i] != null)
			{
				ValueOutput ammoOutput = output.child("Ammo " + i);
				ammoOutput.store(ItemStack.MAP_CODEC, ammo[i]);
			}
		}
		if (placer != null) {
			output.putString("Placer", placer.getName().getString());
		} else if (placerName != null) {
			output.putString("Placer", placerName);
		}
	}
	
	@Override
	public void readAdditionalSaveData(ValueInput input)
	{
		type = AAGunType.getAAGun(input.getStringOr("Type", ""));
		entityData.set(AA_TYPE, input.getStringOr("Type", ""));
		initType();
		health = input.getIntOr("Health", 0);
		setYRot(input.getFloatOr("RotationYaw", 0F));
		setXRot(input.getFloatOr("RotationPitch", 0F));
		for(int i = 0; i < type.numBarrels; i++)
		{
			ValueInput ammoInput = input.childOrEmpty("Ammo " + i);
			Optional<ItemStack> ammoOpt = ammoInput.read(ItemStack.MAP_CODEC);
			ammo[i] = ammoOpt.orElse(ItemStack.EMPTY);
		}
		placerName = input.getStringOr("Placer", "");
	}
	
	@Override
	public InteractionResult interact(Player entityplayer, InteractionHand hand, Vec3 pos) //interact : change back when Forge updates
	{
		// Player right clicked on gun
		// Mount gun
		if(getControllingPassenger() != null && (getControllingPassenger() instanceof Player) && getControllingPassenger() != entityplayer)
		{
			return InteractionResult.SUCCESS;
		}
		if(!world.isClientSide())
		{
			if(getControllingPassenger() == entityplayer)
			{
				entityplayer.stopRiding();
				return InteractionResult.SUCCESS;
			}
			if(!isSentry())
				entityplayer.startRiding(this);
			for(int i = 0; i < (type.shareAmmo ? 1 : type.numBarrels); i++)
			{
				if(ammo[i] == null  || ammo[i].isEmpty())
				{
					int slot = findAmmo(entityplayer);
					if(slot >= 0)
					{
						ammo[i] = entityplayer.getInventory().getItem(slot).copy();
						ammo[i].setCount(1);
						if(!entityplayer.getAbilities().instabuild)
							entityplayer.getInventory().removeItem(slot, 1);
						reloadTimer = type.reloadTime;
						world.playSound(null, getX(), getY(), getZ(), FlansModResourceHandler.getSoundEvent(type.reloadSound), SoundSource.PLAYERS, 1.0F, 1.0F / (random.nextFloat() * 0.4F + 0.8F));
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
	
	//TODO aa are accepting any ammo for any weapon
	public int findAmmo(Player player)
	{
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack stack = player.getInventory().getItem(i);
			if(type.isAmmo(stack))
			{
				return i;
			}
		}
		return -1;
	}
}
