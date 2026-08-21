package com.flansmod.common.guns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.flansmod.client.handlers.FlansModResourceHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.network.PacketMGFire;
import com.flansmod.common.network.PacketMGMount;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.EntityGunItem;
import com.flansmod.common.teams.Team;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.vector.Vector3f;

public class EntityMG extends Entity
{
	protected Level world;
	
	public int blockX, blockY, blockZ;
	public int direction;
	public GunType type;
	public ItemStack ammo = ItemStack.EMPTY.copy();
	public int reloadTimer;
	public int soundDelay;
	public float shootDelay;
	public static List<EntityMG> mgs = new ArrayList<>();
	public Player gunner;
	//Server side
	public boolean isShooting;
	//Client side
	public boolean wasShooting = false;
	
	public int ticksSinceUsed = 0;
	
	private static final EntityDataAccessor<String> MG_TYPE = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Integer> DIRECTION = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BLOCK_X = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BLOCK_Y = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> BLOCK_Z = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<ItemStack> AMMO = SynchedEntityData.defineId(EntityMG.class, EntityDataSerializers.ITEM_STACK);
	
		public EntityMG(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityMG(Level world)
	{
		this(ModEntities.MG, world);
		this.world = level();

	}
	
	public EntityMG(Level world, int x, int y, int z, int dir, GunType gunType)
	{
		this(ModEntities.MG, world);
		this.world = level();

		blockX = x;
		blockY = y;
		blockZ = z;
		xo = x + 0.5D;
		yo = y;
		zo = z + 0.5D;
		setPos(x + 0.5D, y, z + 0.5D);
		direction = dir;
		setYRot(0);
		setXRot(-60);
		type = gunType;
		this.entityData.set(MG_TYPE, type.shortName);
		this.entityData.set(DIRECTION, direction);
		this.entityData.set(BLOCK_X, blockX);
		this.entityData.set(BLOCK_Y, blockY);
		this.entityData.set(BLOCK_Z, blockZ);
		this.entityData.set(AMMO, ammo);
		mgs.add(this);
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(MG_TYPE, "");
		builder.define(DIRECTION, 0);
		builder.define(BLOCK_X, 0);
		builder.define(BLOCK_Y, 0);
		builder.define(BLOCK_Z, 0);
		builder.define(AMMO, ItemStack.EMPTY);
	}
	
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		if(key == MG_TYPE)
			type = GunType.getGun(entityData.get(MG_TYPE));
		else if(key == DIRECTION)
			direction = entityData.get(DIRECTION);
		else if(key == BLOCK_X)
			blockX = entityData.get(BLOCK_X);
		else if(key == BLOCK_Y)
			blockY = entityData.get(BLOCK_Y);
		else if(key == BLOCK_Z)
			blockZ = entityData.get(BLOCK_Z);
		else if(key == AMMO)
			ammo = entityData.get(AMMO);
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
		
		setPos(blockX + 0.5f, blockY, blockZ + 0.5f);
		
		ticksSinceUsed++;
		if(TeamsManager.mgLife > 0 && ticksSinceUsed > TeamsManager.mgLife * 20)
		{
			discard();
		}
		if(world.getBlockState(new BlockPos(blockX, blockY - 1, blockZ)).getBlock() == Blocks.AIR)
		{
			if(!world.isClientSide())
			{
				discard();
			}
		}
		yRotO = getYRot();
		xRotO = getXRot();
		if(gunner != null)
		{
			ticksSinceUsed = 0;
			setYRot(gunner.getYRot() - direction * 90);
			for(; getYRot() < -180; setYRot(getYRot() + 360))
			{
			}
			for(; getYRot() > 180; setYRot(getYRot() - 360))
			{
			}
			setXRot(gunner.getXRot());
			// Keep it within reasonable angles
			if(getYRot() > type.sideViewLimit)
			{
				setYRot(type.sideViewLimit);
				yRotO = type.sideViewLimit;
			}
			if(getYRot() < -type.sideViewLimit)
			{
				setYRot(-type.sideViewLimit);
				yRotO = -type.sideViewLimit;
			}
			
			// Keep user standing behind the gun
			float angle = direction * 90F + getYRot();
			double dX = (type.standBackDist * Math.sin(angle * 3.1415926535F / 180F));
			double dZ = -(type.standBackDist * Math.cos(angle * 3.1415926535F / 180F));
			gunner.setPos((blockX + 0.5D + dX), blockY - 1.0d, (blockZ + 0.5D + dZ));
		}
		else
		{
			setXRot(getXRot() - 1);
		}
		
		if(getXRot() < -type.topViewLimit)
			setXRot(-type.topViewLimit);
		if(getXRot() > type.bottomViewLimit)
			setXRot(type.bottomViewLimit);
		
		if(shootDelay > 0)
			shootDelay--;
		
		// Decrement the reload timer and reload
		if(reloadTimer > 0)
			reloadTimer--;
		if(!ammo.isEmpty() && ammo.getDamageValue() == ammo.getMaxDamage())
		{
			ammo = ItemStack.EMPTY.copy();
			// Scrap metal output?
		}
		if(ammo.isEmpty() && gunner != null)
		{
			int slot = findAmmo(gunner);
			if(slot >= 0)
			{
				ammo = gunner.getInventory().getItem(slot);
				if(!gunner.getAbilities().instabuild)
					gunner.getInventory().setItem(slot, ItemStack.EMPTY.copy());
				reloadTimer = type.reloadTime;
				PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.reloadSound, false);
			}
		}
		if(world.isClientSide() && gunner != null && gunner instanceof Player && FlansMod.proxy.isThePlayer((Player)gunner) && type.mode == EnumFireMode.FULLAUTO)
		{
			//Send a packet!
			checkForShooting();
		}
		if(!world.isClientSide() && isShooting)
		{
			fire();
		}
		if(soundDelay > 0)
			soundDelay--;
	}
	
	private void checkForShooting()
	{
		if(FlansMod.proxy.mouseLeftDown() && !wasShooting && !FlansMod.proxy.isScreenOpen())
		{
			FlansMod.getPacketHandler().sendToServer(new PacketMGFire(true));
			wasShooting = true;
		}
		else if(!FlansMod.proxy.mouseLeftDown() && wasShooting)
		{
			FlansMod.getPacketHandler().sendToServer(new PacketMGFire(false));
			wasShooting = false;
		}
	}
	
	//Server side setter to be called upon receiving a packet
	public void mouseHeld(boolean held)
	{
		isShooting = held;
	}
	
	private void fire()
	{
		if(gunner == null || !gunner.isAlive())
			isShooting = false;
		// Check for ammo / reloading
		if(ammo.isEmpty() || reloadTimer > 0 || shootDelay > 0)
		{
			return;
		}
		// Fire
		BulletType bullet = BulletType.getBullet(ammo.getItem());
		ShootBulletHandler handler = isExtraBullet ->
		{
			if(gunner != null && !gunner.getAbilities().instabuild)
				ammo.hurtAndBreak(1, gunner, InteractionHand.MAIN_HAND);
		};
		shootDelay = type.shootDelay;
		ItemShootable shootableItem = (ItemShootable)ammo.getItem();
		ShootableType shootableType = shootableItem.type;
		
		Vector3f position = new Vector3f(blockX + 0.5D, blockY + type.pivotHeight, blockZ + 0.5D);
		FireableGun gun = new FireableGun(type, type.damage, type.bulletSpread, type.bulletSpeed, type.spreadPattern);
		FiredShot shot = new FiredShot(gun, bullet, this, (ServerPlayer) gunner);

		Double radianYaw = Math.toRadians(direction * 90F + getYRot());
		Double radianPitch = Math.toRadians(getXRot());
		Vector3f shootingDirection = new Vector3f(-Math.sin(radianYaw), -Math.sin(radianPitch), Math.cos(radianYaw));
		ShotHandler.fireGun(world, shot, type.numBullets*shootableType.numBullets, position, shootingDirection, handler);
		
		if(soundDelay <= 0)
		{
			soundDelay = type.shootSoundLength;
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.shootSound, type.distortSound);
		}
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damagesource, float i)
	{
		if(damagesource.getMsgId().equals("player"))
		{
			Entity player = damagesource.getEntity();
			if(player == gunner)
			{
				// Player left clicked on the gun
				if(type.mode == EnumFireMode.FULLAUTO)
					return true;
				
				fire();
			}
			else if(gunner != null)
			{
				return gunner.hurtServer(level, damagesource, i);
			}
			else if(TeamsManager.canBreakGuns)
			{
				discard();
			}
		}
		return true;
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 pos)
	{
		// Player right clicked on gun
		// Mount gun
		if(gunner != null && gunner != player)
		{
			return InteractionResult.SUCCESS;
		}
		if(!world.isClientSide())
		{
			//If this is the player currently using this MG, dismount
			if(gunner == player)
			{
				mountGun(player, false);
				FlansMod.getPacketHandler().sendToAllAround(new PacketMGMount(player, this, false), getX(), getY(), getZ(), FlansMod.driveableUpdateRange, GunUtil.getDimensionId(world));
				return InteractionResult.SUCCESS;
			}
			
			//If this person is already mounting a gun, dismount it first
			if(PlayerHandler.getPlayerData(player).mountingGun != null && !PlayerHandler.getPlayerData(player).mountingGun.isRemoved())
			{
				PlayerHandler.getPlayerData(player).mountingGun.mountGun(player, false);
				return InteractionResult.SUCCESS;
			}
			
			//Spectators can't mount guns
			if(TeamsManager.instance.currentRound != null && PlayerHandler.getPlayerData(player).team == Team.spectators)
				return InteractionResult.SUCCESS;
			
			//None of the above applied, so mount the gun
			mountGun(player, true);
			FlansMod.getPacketHandler().sendToAllAround(new PacketMGMount(player, this, true), getX(), getY(), getZ(), FlansMod.driveableUpdateRange, GunUtil.getDimensionId(world));
			if(ammo.isEmpty())
			{
				int slot = findAmmo(player);
				if(slot >= 0)
				{
					ammo = player.getInventory().getItem(slot).split(1);
					reloadTimer = type.reloadTime;
					playSound(FlansModResourceHandler.getSoundEvent(type.reloadSound), 1.0F, 1.0F / (random.nextFloat() * 0.4F + 0.8F));
				}
			}
			
		}
		return InteractionResult.SUCCESS;
	}
	
	public void mountGun(Player player, boolean mounting)
	{
		if(player == null)
			return;
		if(PlayerHandler.getPlayerData(player) == null)
			return;
		if(mounting)
		{
			gunner = player;
			PlayerHandler.getPlayerData(player).mountingGun = this;
		}
		else
		{
			PlayerHandler.getPlayerData(player).mountingGun = null;
			gunner = null;
		}
	}
	
	public int findAmmo(Player player)
	{
		for(int i = 0; i < player.getInventory().getContainerSize(); i++)
		{
			ItemStack stack = player.getInventory().getItem(i);
			if(type.isCorrectAmmo(stack))
			{
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public void remove(RemovalReason reason)
	{
		// Drop gun
		if(!world.isClientSide())
		{
			if(TeamsManager.weaponDrops == 2)
			{
				EntityGunItem gunEntity = new EntityGunItem(world, getX(), getY(), getZ(), new ItemStack(type.getItem()), Arrays.asList(ammo));
				((ServerLevel)world).addFreshEntity(gunEntity);
			}
			else if(TeamsManager.weaponDrops == 1)
			{
				spawnAtLocation((ServerLevel)world, new ItemStack(type.getItem()));
				// Drop ammo box
				if(!ammo.isEmpty())
					spawnAtLocation((ServerLevel)world, ammo, 0.5F);
			}
		}
		if(gunner != null && PlayerHandler.getPlayerData(gunner) != null)
			PlayerHandler.getPlayerData(gunner).mountingGun = null;
		
		super.remove(reason);
	}
	
	@Override
	public void addAdditionalSaveData(ValueOutput output)
	{
		if(type == null)
			return;
		output.putString("Type", type.shortName);
		output.store(ItemStack.MAP_CODEC, ammo);
		output.putInt("BlockX", blockX);
		output.putInt("BlockY", blockY);
		output.putInt("BlockZ", blockZ);
		output.putByte("Dir", (byte)direction);
	}
	
	@Override
	public void readAdditionalSaveData(ValueInput input)
	{
		type = GunType.getGun(input.getStringOr("Type", ""));
		entityData.set(MG_TYPE, input.getStringOr("Type", ""));
		blockX = input.getIntOr("BlockX", 0);
		blockY = input.getIntOr("BlockY", 0);
		blockZ = input.getIntOr("BlockZ", 0);
		direction = input.getByteOr("Dir", (byte)0);
		Optional<ItemStack> ammoOpt = input.read(ItemStack.MAP_CODEC);
		ammo = ammoOpt.orElse(ItemStack.EMPTY);
		entityData.set(AMMO, ammo);
	}
}
