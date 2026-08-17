package com.flansmod.common.driveables;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.gamerules.GameRules;

import com.flansmod.api.IControllable;
import com.flansmod.api.IExplodeable;
import com.flansmod.client.EntityCamera;
import com.flansmod.client.FlansModClient;
import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.client.handlers.KeyInputHandler;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.RotatedAxes;
import com.flansmod.common.driveables.DriveableType.ParticleEmitter;
import com.flansmod.common.driveables.mechas.ContainerMechaInventory;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.EnumSpreadPattern;
import com.flansmod.common.guns.FireableGun;
import com.flansmod.common.guns.FiredShot;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.InventoryHelper;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootBulletHandler;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.ShotHandler;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.BulletHit;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.DriveableHit;
import com.flansmod.common.network.PacketDriveableDamage;
import com.flansmod.common.network.PacketDriveableKey;
import com.flansmod.common.network.PacketDriveableKeyHeld;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.vector.Vector3f;

import static com.flansmod.common.util.BlockUtil.destroyBlock;

public abstract class EntityDriveable extends Entity implements IControllable, IExplodeable
{
	/** Ticks since last server update. Use to smoothly transition to new position */
	public int serverPositionTransitionTicker;
	/** Server side position, as synced by PacketVehicleControl packets */
	public double serverPosX, serverPosY, serverPosZ;
	/** Server side rotation, as synced by PacketVehicleControl packets */
	public double serverYaw, serverPitch, serverRoll;
	
	/** The driveable data which contains the inventory, the engine and the fuel */
	public DriveableData driveableData;
	/** The shortName of the driveable type, used to obtain said type */
	public String driveableType;
	
	/**
	 * The throttle, in the range -1, 1 is multiplied by the maxThrottle (or maxNegativeThrottle) from the plane type to
	 * obtain the thrust
	 */
	public float throttle;
	/** The wheels on this plane */
	public EntityWheel[] wheels;
	
	public boolean fuelling;
	/** Extra prevRotation field for smoothness in all 3 rotational axes */
	public float prevRotationRoll;
	/** Angular velocity */
	public Vector3f angularVelocity = new Vector3f(0F, 0F, 0F);
	
	/** Whether each mouse button is held */
	public boolean primaryShootHeld = false, secondaryShootHeld = false;
	
	/** Shoot delay variables */
	public float shootDelayPrimary, shootDelaySecondary;
	/** Minigun speed variables */
	public float minigunSpeedPrimary, minigunSpeedSecondary;
	/** Current gun variables for alternating weapons */
	public int currentPrimaryGunShootPointIndex, currentSecondaryGunShootPointIndex;
	
	/** Whether each mouse button is held */
	public boolean leftMouseHeld = false, rightMouseHeld = false;
	
	/** Angle of harvester aesthetic piece */
	public float harvesterAngle;
	
	public RotatedAxes prevAxes;
	public RotatedAxes axes;
	
	private EntitySeat[] seats;
	/** Until this is true, just look for seat and wheel connections */
	protected boolean readyForUpdates = false;
	
	private float yOffset;
	
	public com.flansmod.client.EntityCamera camera;
	
	private int[] emitterTimers;
	
	public int animCount = 0;
	public int animFrame = 0;
	
	// Gun recoil
	public boolean isRecoil = false;
	public float recoilPos = 0;
	public float lastRecoilPos = 0;
	public int recoilTimer = 0;
	
	/** Can't break the block with hardness greater than this value 
	 *  when collided */ 
	public float collisionForce = 30F;

	/** Damage factor of unbreakable block such as bedrock when collided */
	public float unbreakableBlockDamage = 100F;
	
	/** Step height for this driveable, set from the type */
	protected float stepHeight = 0F;
	
	/** The level this entity is in, mirrors the 1.12.2 world field */
	protected Level world;
	
	private static final EntityDataAccessor<String> TYPE =
		SynchedEntityData.defineId(EntityDriveable.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> ENGINE =
		SynchedEntityData.defineId(EntityDriveable.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> FUEL =
		SynchedEntityData.defineId(EntityDriveable.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> PAINT =
		SynchedEntityData.defineId(EntityDriveable.class, EntityDataSerializers.INT);
	
	public EntityDriveable(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
		axes = new RotatedAxes();
		prevAxes = new RotatedAxes();
		yOffset = 6F / 16F;
	}
	
	public EntityDriveable(Level world)
	{
		this(ModEntities.PLANE, world);
	}
	
	public EntityDriveable(EntityType<?> type, Level world, DriveableType t, DriveableData d)
	{
		this(type, world);
		driveableType = t.shortName;
		driveableData = d;
		entityData.set(TYPE, driveableType);
		entityData.set(ENGINE, driveableData.engine != null ? driveableData.engine.shortName : "");
		entityData.set(FUEL, driveableData.fuelInTank);
		entityData.set(PAINT, driveableData.paintjobID);
	}

	@Override
	public float maxUpStep()
	{
		return stepHeight;
	}
	
	protected void initType(DriveableType type, boolean firstSpawn, boolean clientSide)
	{
		seats = new EntitySeat[type.numPassengers + 1];
		wheels = new EntityWheel[type.wheelPositions.length];
		if(!clientSide && firstSpawn)
		{
			for(int i = 0; i < type.numPassengers + 1; i++)
			{
				seats[i] = new EntitySeat(world, this, i);
				world.addFreshEntity(seats[i]);
				seats[i].startRiding(this);
			}
			
			for(int i = 0; i < wheels.length; i++)
			{
				wheels[i] = new EntityWheel(world, this, i);
				world.addFreshEntity(wheels[i]);
				wheels[i].startRiding(this);
			}
		}
		stepHeight = type.wheelStepHeight;
		yOffset = type.yOffset;
		
		emitterTimers = new int[type.emitters.size()];
		for(int i = 0; i < type.emitters.size(); i++)
		{
			emitterTimers[i] = random.nextInt(type.emitters.get(i).emitRate);
		}
		
		if(driveableData == null)
			driveableData = new DriveableData();
		driveableType = type.shortName;
		entityData.set(TYPE, driveableType);
		entityData.set(ENGINE, driveableData.engine != null ? driveableData.engine.shortName : "");
		entityData.set(FUEL, driveableData.fuelInTank);
		entityData.set(PAINT, driveableData.paintjobID);
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(TYPE, "");
		builder.define(ENGINE, "");
		builder.define(FUEL, 0F);
		builder.define(PAINT, 0);
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		CompoundTag tag = new CompoundTag();
		if(driveableData != null)
			driveableData.writeToNBT(tag);
		tag.putString("Type", driveableType == null ? "" : driveableType);
		tag.putFloat("RotationYaw", axes.getYaw());
		tag.putFloat("RotationPitch", axes.getPitch());
		tag.putFloat("RotationRoll", axes.getRoll());
		output.store("FlanData", CompoundTag.CODEC, tag);
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
		CompoundTag tag = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		driveableType = tag.getStringOr("Type", "");
		driveableData = new DriveableData(tag);
		entityData.set(TYPE, driveableType);
		entityData.set(ENGINE, driveableData.engine != null ? driveableData.engine.shortName : "");
		entityData.set(FUEL, driveableData.fuelInTank);
		entityData.set(PAINT, driveableData.paintjobID);
		initType(DriveableType.getDriveable(driveableType), false, false);
		
		yRotO = tag.getFloatOr("RotationYaw", 0F);
		xRotO = tag.getFloatOr("RotationPitch", 0F);
		prevRotationRoll = tag.getFloatOr("RotationRoll", 0F);
		axes = new RotatedAxes(yRotO, xRotO, prevRotationRoll);
	}
	
	/**
	 * Called with the movement of the mouse. Used in controlling vehicles if need be.
	 *
	 * @param deltaY
	 * @param deltaX
	 * @return if mouse movement was handled.
	 */
	@Override
	public abstract void onMouseMoved(int deltaX, int deltaY);
	
	public net.minecraft.world.entity.Entity getCamera()
	{
		return camera;
	}
	
	protected boolean canSit(int seat)
	{
		return getDriveableType().numPassengers >= seat && seats[seat].getControllingPassenger() == null;
	}
	
	public boolean isPushable()
	{
		return false;
	}
	
	/**
	 * Pass generic damage to the core
	 */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damagesource, float i)
	{
		return attackEntityFrom(damagesource, i);
	}
	
	public boolean attackEntityFrom(DamageSource damagesource, float i)
	{
		return world.isClientSide() || isRemoved() || attackPart(EnumDriveablePart.core, damagesource, i);
	}
	
	private void reportVehicleError()
	{
		FlansMod.log.warn("Vehicle error in " + this);
		FlansModClient.numVehicleExceptions++;
	}
	
	public void setDead()
	{
		if(world.isClientSide() && camera != null)
			camera.discard();
		
		if(seats != null)
		{
			for(EntitySeat seat : seats)
			{
				if(seat != null)
					seat.reallySetDead();
			}
		}
		if(wheels != null)
		{
			for(EntityWheel wheel : wheels)
			{
				if(wheel != null)
					wheel.reallySetDead();
			}
		}
		
		discard();
	}
	
	public boolean canBeCollidedWith(Entity other)
	{
		return !isRemoved();
	}
	
	@Override
	public void push(Entity entity)
	{
		if(!isPartOfThis(entity))
			super.push(entity);
	}
	
	public void setPositionRotationAndMotion(double x, double y, double z, float yaw, float pitch, float roll,
											 double motX, double motY, double motZ, float velYaw, float velPitch,
											 float velRoll, float throttle, float steeringYaw)
	{
		if(world.isClientSide())
		{
			serverPosX = x;
			serverPosY = y;
			serverPosZ = z;
			serverYaw = yaw;
			serverPitch = pitch;
			serverRoll = roll;
			serverPositionTransitionTicker = 5;
		}
		else
		{
			setPos(x, y, z);
			yRotO = yaw;
			xRotO = pitch;
			prevRotationRoll = roll;
			setRotation(yaw, pitch, roll);
		}
		// Set the motions regardless of side.
		setDeltaMovement(motX, motY, motZ);
		angularVelocity = new Vector3f(velYaw, velPitch, velRoll);
		this.throttle = throttle;
	}
	
	@Override
	public boolean serverHandleKeyPress(int key, Player player)
	{
		switch(key)
		{
			case 6:
				if(getSeat(0).getControllingPassenger() != null)
					getSeat(0).getControllingPassenger().stopRiding();
				return true;
			case 8:
				if(getDriveableType().modeSecondary == EnumFireMode.SEMIAUTO) // Secondary
				{
					shoot(true);
					return true;
				}
			case 9:
				if(getDriveableType().modePrimary == EnumFireMode.SEMIAUTO) // Primary
				{
					shoot(false);
					return true;
				}
		}
		return false;
	}
	
	@Override
	public boolean pressKey(int key, Player player, boolean isOnEvent)
	{
		switch(key)
		{
			case 6: //Exit
			{
				Minecraft mc = Minecraft.getInstance();
				mc.setCameraEntity(mc.player);
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableKey(key));
				return true;
			}
			case 8: //Drop bomb
			{
				if(isOnEvent)
				{
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableKey(key));
				}
				else if(!secondaryShootHeld)
				{
					updateKeyHeldState(8, true);
				}
				return true;
			}
			case 9: //Shoot bullet
			{
				if(isOnEvent)
				{
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableKey(key));
				}
				else if(!primaryShootHeld)
				{
					updateKeyHeldState(9, true);
				}
				return true;
			}
			case 18:
			{
				togglePerspective();
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	@Override
	public void updateKeyHeldState(int key, boolean held)
	{
		if(world.isClientSide())
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableKeyHeld(key, held));
		}
		switch(key)
		{
			case 9:
				primaryShootHeld = held;
				break;
			case 8:
				secondaryShootHeld = held;
				break;
		}
	}
	
	/**
	 * Shoot method called by pressing / holding shoot buttons
	 */
	public void shoot(boolean secondary)
	{
		DriveableType type = getDriveableType();
		List<ShootPoint> shootPoints = type.shootPoints(secondary);
		EnumWeaponType weaponType = type.weaponType(secondary);
		boolean driverIsLivingEntity = seats[0] != null
				&& seats[0].getControllingPassenger() instanceof LivingEntity;
		boolean gunHasDelayRemaining = getShootDelay(secondary) > 0;
		boolean gunHasShootPoints = !shootPoints.isEmpty();

		if(driverIsLivingEntity && !gunHasDelayRemaining && gunHasShootPoints)
		{
			// For alternating guns, move on to the next one
			if(type.alternate(secondary))
			{
				int nextShootPointIndex = (getCurrentShootPointIndex(secondary) + 1) % shootPoints.size();
				setCurrentShootPointIndex(nextShootPointIndex, secondary);
				shootFromPoint(type, shootPoints.get(nextShootPointIndex), nextShootPointIndex, secondary, weaponType);
			}
			else
			{
				for(int i = 0; i < shootPoints.size(); i++)
				{
					shootFromPoint(type, shootPoints.get(i), i, secondary, weaponType);
				}
			}
		}
	}
	
	public boolean driverIsCreative()
	{
		Player driver = getDriver();
		return driver != null && driver.isCreative();
	}
	
	public Player getDriver()
	{
		if(seats != null && seats[0] != null && seats[0].getControllingPassenger() instanceof Player)
		{
			return ((Player)seats[0].getControllingPassenger());
		}
		else
		{
			return null;
		}
	}
	
	private void shootFromPoint(
			DriveableType type,
			ShootPoint shootPoint,
			int shootPointIndex,
			boolean secondary,
			EnumWeaponType weaponType)
	{
		Vector3f gunVec = getOrigin(shootPoint);
		Vector3f lookVector = getLookVector(shootPoint);
		
		switch(weaponType)
		{
			case BOMB:
				dropBomb(type, secondary, weaponType, gunVec, lookVector);
				break;
			case MISSILE:
			case SHELL:
				fireShell(type, secondary, weaponType, gunVec, lookVector);
				break;
			case GUN:
				fireGun(type, shootPoint, shootPointIndex, secondary, gunVec, lookVector);
				break;
			case MINE:
			case NONE:
			default:
				break;
		}
		
		setShootDelay(type.shootDelay(secondary), secondary);
	}
	
	private void fireGun(DriveableType type, ShootPoint shootPoint, int shootPointIndex, boolean secondary,
						 Vector3f gunVec, Vector3f lookVector)
	{
		if(shootPoint.rootPos instanceof PilotGun)
		{
			PilotGun pilotGun = (PilotGun)shootPoint.rootPos;
			GunType gunType = pilotGun.type;
			ItemStack ammoItemStack = driveableData.ammo[getDriveableType().numPassengerGunners + shootPointIndex];
			Item ammoItem = ammoItemStack.getItem();
			boolean isAmmo = ammoItem instanceof ItemShootable;
			boolean isValidAmmoForGun = isAmmo && gunType.isCorrectAmmo(((ItemShootable) ammoItem).type);
			
			//TODO grenades wont work (currently no vehicle with this feature exists)
			if(isValidAmmoForGun && ((ItemShootable) ammoItem).type instanceof BulletType)
			{
				ShootableType ammoType = ((ItemShootable) ammoItem).type;
				BulletType bulletType = (BulletType) ammoType;
				FireableGun fireableGun = new FireableGun(
						gunType,
						gunType.damage,
						gunType.bulletSpread,
						gunType.bulletSpeed,
						gunType.spreadPattern);
				FiredShot shot = new FiredShot(fireableGun, bulletType, this, (ServerPlayer)getDriver());
				
				ShootBulletHandler handler = isExtraBullet ->
				{
					ammoItemStack.setDamageValue(ammoItemStack.getDamageValue() + 1);
					if(ammoItemStack.isEmpty())
					{
						driveableData.ammo[getDriveableType().numPassengerGunners + shootPointIndex] = ItemStack.EMPTY
								.copy();
					}
				};
				
				Vector3f gunVector = Vector3f.add(gunVec, new Vector3f((float)getX(), (float)getY(), (float)getZ()), null);
				
				ShotHandler.fireGun(world,
						shot,
						gunType.numBullets * bulletType.numBullets,
						gunVector,
						lookVector,
						handler);
				
				if(type.shootSound(secondary) != null)
				{
					PacketPlaySound.sendSoundPacket(gunVector.x,
							gunVector.y,
							gunVector.z,
							FlansMod.soundRange,
							0,
							type.shootSound(secondary),
							false);
				}
			}
		}
	}
	
	private void fireShell(DriveableType type, boolean secondary, EnumWeaponType weaponType, Vector3f gunVec,
						   Vector3f lookVector)
	{
		if(TeamsManager.shellsEnabled)
		{
			for(int i = driveableData.getMissileInventoryStart();
				i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++)
			{
				ItemStack shell = driveableData.getItem(i);
				if(shell != null && shell.getItem() instanceof ItemBullet && type.isValidAmmo(
					((ItemBullet)shell.getItem()).type, weaponType))
				{
					shootProjectile(i, gunVec, lookVector, type, secondary, (float)getSpeed() + 3f);
					break;
				}
			}
		}
	}
	
	private void dropBomb(DriveableType type, boolean secondary, EnumWeaponType weaponType, Vector3f gunVec,
						  Vector3f lookVector)
	{
		if(TeamsManager.bombsEnabled)
		{
			for(int i = driveableData.getBombInventoryStart();
				i < driveableData.getBombInventoryStart() + type.numBombSlots; i++)
			{
				ItemStack bomb = driveableData.getItem(i);
				if(bomb != null && bomb.getItem() instanceof ItemBullet && type.isValidAmmo(
					((ItemBullet)bomb.getItem()).type, weaponType))
				{
					shootProjectile(i, gunVec, lookVector, type, secondary, (float)getSpeed());
					break;
				}
			}
		}
	}
	
	public double getSpeed()
	{
		return Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().y * getDeltaMovement().y + getDeltaMovement().z * getDeltaMovement().z);
	}
	
	public Vector3f getOrigin(ShootPoint shootPoint)
	{
		DriveablePosition driveablePosition = shootPoint.rootPos;
		// Rotate the gun vector to global axes
		Vector3f localGunVec = new Vector3f(driveablePosition.position);
		
		if(driveablePosition.part == EnumDriveablePart.turret)
		{
			// Untranslate by the turret origin, to get the rotation about the right point
			Vector3f.sub(localGunVec, getDriveableType().turretOrigin, localGunVec);
			// Rotate by the turret angles
			localGunVec = seats[0].looking.findLocalVectorGlobally(localGunVec);
			// Translate by the turret origin
			Vector3f.add(localGunVec, getDriveableType().turretOrigin, localGunVec);
		}
		
		return rotate(localGunVec);
	}
	
	public Vector3f getLookVector(ShootPoint shootPoint)
	{
		return axes.getXAxis();
	}
	
	private void shootProjectile(final Integer slot, Vector3f gunVec, Vector3f lookVector, DriveableType type,
								 Boolean secondary, float speed)
	{
		ItemStack bullet = driveableData.getItem(slot);
		ItemBullet bulletItem = (ItemBullet)bullet.getItem();
		int damageMultiplier = secondary ? type.damageModifierSecondary : type.damageModifierPrimary;
		
		FireableGun fireableGun = new FireableGun(bulletItem.type,
			bulletItem.type.damageVsLiving * damageMultiplier,
			bulletItem.type.damageVsDriveable * damageMultiplier,
			bulletItem.type.bulletSpread,
			speed,
			EnumSpreadPattern.circle);
		FiredShot shot = new FiredShot(fireableGun, bulletItem.type, this, (ServerPlayer)getDriver());
		
		ShootBulletHandler handler = isExtraBullet ->
		{
			if(!driverIsCreative())
			{
				ItemStack bulletStack = driveableData.getItem(slot);
				bulletStack.setDamageValue(bulletStack.getDamageValue() + 1);
				if(bulletStack.getDamageValue() == bulletStack.getMaxDamage())
				{
					bulletStack.setDamageValue(0);
					bulletStack.setCount(bulletStack.getCount() - 1);
					if(bulletStack.getCount() == 0)
						bulletStack = ItemStack.EMPTY.copy();
				}
				driveableData.setItem(slot, bulletStack);
			}
		};
		
		Vector3f gunVector = Vector3f.add(gunVec, new Vector3f((float)getX(), (float)getY(), (float)getZ()), null);
		
		ShotHandler.fireGun(world, shot, bulletItem.type.numBullets, gunVector, lookVector, handler);
		
		if(type.shootSound(secondary) != null)
		{
			//TODO proper general sound implementation
			PacketPlaySound.sendSoundPacket(gunVector.x,
				gunVector.y,
				gunVector.z,
				FlansMod.soundRange,
				0,
				type.shootSound(secondary),
				false);
		}
		// Reset the shoot delay
		setShootDelay(type.shootDelay(secondary), secondary);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		// If we don't have the driveable data (i.e. client side spawn), grab it from the synched data
		if(driveableData == null || driveableType == null || driveableType.isEmpty())
		{
			loadClientData();
		}
		DriveableType type = getDriveableType();
		if(type == null)
			return;
		
		yRotO = axes.getYaw();
		xRotO = axes.getPitch();
		prevRotationRoll = axes.getRoll();
		
		// Do a full check of our passengers for wheels or seats
		for(Entity passenger : getPassengers())
		{
			if(passenger instanceof EntitySeat)
			{
				EntitySeat seat = (EntitySeat)passenger;
				if(seat.getExpectedSeatID() >= 0 && seats[seat.getExpectedSeatID()] != seat)
				{
					if(seats[seat.getExpectedSeatID()] != null)
					{
						FlansMod.log.error("Driveable already had a seat in place");
						seats[seat.getExpectedSeatID()].discard();
					}
					
					seats[seat.getExpectedSeatID()] = seat;
				}
			}
			else if(passenger instanceof EntityWheel)
			{
				EntityWheel wheel = (EntityWheel)passenger;
				if(wheel.getExpectedWheelID() >= 0 && wheels[wheel.getExpectedWheelID()] != wheel)
				{
					if(wheels[wheel.getExpectedWheelID()] != null)
					{
						FlansMod.log.error("Driveable already had a wheel in place");
						wheels[wheel.getExpectedWheelID()].discard();
					}
					wheels[wheel.getExpectedWheelID()] = wheel;
				}
			}
			else
			{
				FlansMod.log.warn("Entity " + passenger + " is riding a driveable core entity.");
			}
		}
		
		readyForUpdates = true;
		for(int i = 0; i < type.numPassengers; i++)
		{
			if(seats[i] == null)
			{
				readyForUpdates = false;
			}
		}
		for(int i = 0; i < type.wheelPositions.length; i++)
		{
			if(wheels[i] == null)
			{
				readyForUpdates = false;
			}
		}
		
		if(!readyForUpdates)
		{
			if(!world.isClientSide())
			{
				// Well heck, if it's bork, let's make new ones
				initType(type, true, false);
			}
			// If we end up stuck like this on a client, handle updates from server
			if(world.isClientSide())
			{
				// The driveable is currently moving towards its server position. Continue doing so.
				if(serverPositionTransitionTicker > 0)
				{
					moveTowardServerPosition();
				}
				// If the driveable is at its server position and does not have the next update, it should just simulate
				// itself as a server side driveable would, so continue
			}
			
			return;
		}
		
		// Reset weapon key held states if necessary
		if(world.isClientSide())
		{
			if(primaryShootHeld && !KeyInputHandler.primaryVehicleInteract.isDown())
			{
				primaryShootHeld = false;
				updateKeyHeldState(9, false);
			}
			if(secondaryShootHeld && !KeyInputHandler.secondaryVehicleInteract.isDown())
			{
				secondaryShootHeld = false;
				updateKeyHeldState(8, false);
			}
		}
		
		// Harvest stuff
		// Aesthetics
		if(hasEnoughFuel())
		{
			harvesterAngle += throttle / 5F;
		}
		// Actual harvesting
		if(type.harvestBlocks && type.health.get(EnumDriveablePart.harvester) != null)
		{
			CollisionBox box = type.health.get(EnumDriveablePart.harvester);
			for(float x = box.x; x <= box.x + box.w; x++)
			{
				for(float y = box.y; y <= box.y + box.h; y++)
				{
					for(float z = box.z; z <= box.z + box.d; z++)
					{
						Vector3f v = axes.findLocalVectorGlobally(new Vector3f(x, y, z));
						
						int blockX = (int)Math.round(getX() + v.x);
						int blockY = (int)Math.round(getY() + v.y);
						int blockZ = (int)Math.round(getZ() + v.z);
						BlockState block = world.getBlockState(new BlockPos(blockX, blockY, blockZ));
						
						if(type.materialsHarvested.contains(block.getBlock()) && block.getDestroySpeed(world,
							new BlockPos(blockX, blockY, blockZ)) >= 0F)
						{
							// Add the item stack to mecha inventory
							NonNullList<ItemStack> stacks = NonNullList.create();
							if(!world.isClientSide())
							{
								BlockPos pos = new BlockPos(blockX, blockY, blockZ);
								List<ItemStack> drops = Block.getDrops(world.getBlockState(pos), (ServerLevel)world, pos, null);
								stacks.addAll(drops);
							}
							for(ItemStack stack : stacks)
							{
								if(!InventoryHelper.add(driveableData, stack,
									driverIsCreative()) && !world.isClientSide() && ((ServerLevel)world).getGameRules().get(GameRules.BLOCK_DROPS))
								{
									world.addFreshEntity(
										new ItemEntity(world, blockX + 0.5F, blockY + 0.5F, blockZ + 0.5F,
											stack));
								}
							}
							// Destroy block
							if(!world.isClientSide())
							{
								ServerLevel worldServer = (ServerLevel)world;
								BlockPos pos = new BlockPos(blockX, blockY, blockZ);
								destroyBlock(worldServer, pos, getDriver(), false);
							}
						}
					}
				}
			}
		}
		
		//Gun recoil
		if(leftMouseHeld)
		{
			tryRecoil();
			setRecoilTimer();
		}
		lastRecoilPos = recoilPos;
		
		if(recoilPos > 180 - (180 / type.recoilTime))
		{
			recoilPos = 0;
			isRecoil = false;
		}
		
		if(isRecoil)
			recoilPos = recoilPos + (180 / type.recoilTime);
		
		if(recoilTimer >= 0)
			recoilTimer--;
		
		for(DriveablePart part : getDriveableData().parts.values())
		{
			if(part.box != null)
			{
				
				part.update(this);
				// Client side particles
				if(world.isClientSide())
				{
					if(part.onFire)
					{
						// Pick a random position within the bounding box and spawn a flame there
						Vector3f pos = getRandPosInBoundingBox(part);
						world.addParticle(ParticleTypes.FLAME, getX() + pos.x, getY() + pos.y, getZ() + pos.z, 0, 0, 0);
					}
					if(part.health > 0 && part.health < part.maxHealth / 2)
					{
						Vector3f pos = getRandPosInBoundingBox(part);
						world.addParticle(
							part.health < part.maxHealth / 4 ?
								ParticleTypes.LARGE_SMOKE :
								ParticleTypes.SMOKE, getX() + pos.x, getY() + pos.y, getZ() + pos.z, 0, 0,
							0);
					}
				}
				// Server side fire handling
				if(part.onFire)
				{
					// Rain can put out fire
					if(world.isRaining() && random.nextInt(40) == 0)
						part.onFire = false;
					// Also water blocks
					// Get the centre point of the part
					Vector3f pos = axes.findLocalVectorGlobally(
						new Vector3f(part.box.x + part.box.w / 2F, part.box.y + part.box.h / 2F,
							part.box.z + part.box.d / 2F));
					if(world.getFluidState(new BlockPos(Mth.floor(getX() + pos.x), Mth.floor(getY() + pos.y),
						Mth.floor(getZ() + pos.z))).is(FluidTags.WATER))
					{
						part.onFire = false;
					}
				}
				else
				{
					Vector3f pos = getPartLocalVectorGlobally(part);
					if(world.getFluidState(new BlockPos(Mth.floor(getX() + pos.x), Mth.floor(getY() + pos.y),
						Mth.floor(getZ() + pos.z))).is(FluidTags.LAVA))
					{
						part.onFire = true;
					}
				}
			}
		}
		
		for(int i = 0; i < type.emitters.size(); i++)
		{
			ParticleEmitter emitter = type.emitters.get(i);
			emitterTimers[i]--;
			boolean canEmit = false;
			DriveablePart part = getDriveableData().parts.get(EnumDriveablePart.getPart(emitter.part));
			float healthPercentage = (float)part.health / (float)part.maxHealth;
			if(isPartIntact(EnumDriveablePart.getPart(
				emitter.part)) && healthPercentage >= emitter.minHealth && healthPercentage <= emitter.maxHealth)
			{
				canEmit = true;
			}
			if(emitterTimers[i] <= 0)
			{
				if(throttle >= emitter.minThrottle && throttle <= emitter.maxThrottle && canEmit)
				{
					// Emit!
					Vector3f velocity = new Vector3f(0, 0, 0);
					Vector3f pos = new Vector3f(0, 0, 0);
					if(seats != null && seats[0] != null)
					{
						if(EnumDriveablePart.getPart(
							emitter.part) != EnumDriveablePart.turret && EnumDriveablePart.getPart(
							emitter.part) != EnumDriveablePart.head)
						{
							Vector3f localPosition = new Vector3f(
								emitter.origin.x + random.nextFloat() * emitter.extents.x - emitter.extents.x * 0.5f,
								emitter.origin.y + random.nextFloat() * emitter.extents.y - emitter.extents.y * 0.5f,
								emitter.origin.z + random.nextFloat() * emitter.extents.z - emitter.extents.z * 0.5f);
							
							pos = axes.findLocalVectorGlobally(localPosition);
							velocity = axes.findLocalVectorGlobally(emitter.velocity);
						}
						else if(EnumDriveablePart.getPart(
							emitter.part) == EnumDriveablePart.turret || EnumDriveablePart.getPart(
							emitter.part) != EnumDriveablePart.head)
						{
							
							Vector3f localPosition2 = new Vector3f(
								emitter.origin.x + random.nextFloat() * emitter.extents.x - emitter.extents.x * 0.5f,
								emitter.origin.y + random.nextFloat() * emitter.extents.y - emitter.extents.y * 0.5f,
								emitter.origin.z + random.nextFloat() * emitter.extents.z - emitter.extents.z * 0.5f);
							
							RotatedAxes yawOnlyLooking = new RotatedAxes(seats[0].looking.getYaw() + axes.getYaw(),
								axes.getPitch(), axes.getRoll());
							
							pos = yawOnlyLooking.findLocalVectorGlobally(localPosition2);
							velocity = yawOnlyLooking.findLocalVectorGlobally(emitter.velocity);
						}
						world.addParticle(emitter.effectType,
							getX() + pos.x, getY() + pos.y, getZ() + pos.z, velocity.x, velocity.y, velocity.z);
					}
				}
				emitterTimers[i] = emitter.emitRate;
			}
		}
		
		checkParts();
		
		yRotO = axes.getYaw();
		xRotO = axes.getPitch();
		prevRotationRoll = axes.getRoll();
		prevAxes = axes.clone();
		
		boolean canThrust = driverIsCreative() || driveableData.fuelInTank > 0;
		
		// If there's no player in the driveable or it cannot thrust, slow the plane and turn off mouse held actions
		if((getDriver() == null) ||
			!canThrust && getDriveableType().maxThrottle != 0 && getDriveableType().maxNegativeThrottle != 0)
		{
			throttle *= 0.98F;
			primaryShootHeld = secondaryShootHeld = false;
		}
		else if(getDriver() != null && getDriver() == getControllingPassenger())
		{
			reportVehicleError();
		}
		
		if(seats[0] != null && seats[0].getVehicle() == null)
		{
			rightMouseHeld = leftMouseHeld = false;
		}
		
		// Check if shooting
		if(shootDelayPrimary > 0)
			shootDelayPrimary--;
		if(shootDelaySecondary > 0)
			shootDelaySecondary--;
		if(!world.isClientSide())
		{
			if(primaryShootHeld && getDriveableType().modePrimary == EnumFireMode.FULLAUTO)
				shoot(false);
			if(secondaryShootHeld && getDriveableType().modeSecondary == EnumFireMode.FULLAUTO)
				shoot(true);
			minigunSpeedPrimary *= 0.9F;
			minigunSpeedSecondary *= 0.9F;
			if(primaryShootHeld && getDriveableType().modePrimary == EnumFireMode.MINIGUN)
			{
				minigunSpeedPrimary += 0.1F;
				if(minigunSpeedPrimary > 1F)
					shoot(false);
			}
			if(secondaryShootHeld && getDriveableType().modeSecondary == EnumFireMode.MINIGUN)
			{
				minigunSpeedSecondary += 0.1F;
				if(minigunSpeedSecondary > 1F)
					shoot(true);
			}
		}
		
		// Handle fuel
		
		int fuelMultiplier = 2;
		
		// The tank is currently full, so do nothing
		if(getDriveableData().fuelInTank >= type.fuelTankSize)
			return;
		
		// Look through the entire inventory for fuel cans, buildcraft fuel buckets and RedstoneFlux power sources
		for(int i = 0; i < getDriveableData().getContainerSize(); i++)
		{
			ItemStack stack = getDriveableData().getItem(i);
			if(stack == null || stack.isEmpty())
				continue;
			Item item = stack.getItem();
			// Check for Flan's Mod fuel items
			if(item instanceof ItemPart)
			{
				PartType part = ((ItemPart)item).type;
				// Check it is a fuel item
				if(part.category == EnumPartCategory.FUEL)
				{
					// Put 2 points of fuel
					getDriveableData().fuelInTank += fuelMultiplier;
					
					// Damage the fuel item to indicate being used up
					int damage = stack.getDamageValue();
					stack.setDamageValue(damage + 1);
					
					// If we have finished this fuel item
					if(damage >= stack.getMaxDamage())
					{
						// Reset the damage to 0
						stack.setDamageValue(0);
						// Consume one item
						stack.setCount(stack.getCount() - 1);
						// If we consumed the last one, destroy the stack
						if(stack.getCount() <= 0)
							getDriveableData().setItem(i, ItemStack.EMPTY.copy());
					}
					
					// We found a fuel item and consumed some, so we are done
					break;
				}
				
				// Check for Buildcraft oil and fuel buckets
				else if(FlansMod.hooks.BuildCraftLoaded && ItemStack.isSameItem(stack,
					FlansMod.hooks.BuildCraftOilBucket) &&
					getDriveableData().fuelInTank + 1000 * fuelMultiplier <= type.fuelTankSize)
				{
					getDriveableData().fuelInTank += 1000 * fuelMultiplier;
					getDriveableData().setItem(i, new ItemStack(Items.BUCKET));
				}
				else if(FlansMod.hooks.BuildCraftLoaded && ItemStack.isSameItem(stack,
					FlansMod.hooks.BuildCraftFuelBucket) &&
					getDriveableData().fuelInTank + 2000 * fuelMultiplier <= type.fuelTankSize)
				{
					getDriveableData().fuelInTank += 2000 * fuelMultiplier;
					getDriveableData().setItem(i, new ItemStack(Items.BUCKET));
				}
			}
		}
	}
	
	public void PostUpdate()
	{
		if(Double.isNaN(getX()) 
		|| Double.isNaN(getY())
		|| Double.isNaN(getZ())
		|| Float.isNaN(getYRot())
		|| Float.isNaN(getXRot())
		|| !axes.isValid())
		{
			FlansMod.log.error("Driveable went to NaNsville. Reverting one frame");
			setPos(xo, yo, zo);
			
			// Just reset the axes
			axes = new RotatedAxes();
			prevAxes = new RotatedAxes();
		}
	}
	
	private void loadClientData()
	{
		driveableType = entityData.get(TYPE);
		if(driveableType == null || driveableType.isEmpty())
			return;
		driveableData = new DriveableData();
		driveableData.type = driveableType;
		driveableData.engine = PartType.getPart(entityData.get(ENGINE));
		driveableData.fuelInTank = entityData.get(FUEL);
		driveableData.paintjobID = entityData.get(PAINT);
		DriveableType type = getDriveableType();
		if(type == null)
			return;
		driveableData.numBombs = type.numBombSlots;
		driveableData.numCargo = type.numCargoSlots;
		driveableData.numMissiles = type.numMissileSlots;
		driveableData.numGuns = type.ammoSlots();
		driveableData.ammo = new ItemStack[driveableData.numGuns];
		driveableData.bombs = new ItemStack[driveableData.numBombs];
		driveableData.missiles = new ItemStack[driveableData.numMissiles];
		driveableData.cargo = new ItemStack[driveableData.numCargo];
		for(int i = 0; i < driveableData.ammo.length; i++)
			driveableData.ammo[i] = ItemStack.EMPTY.copy();
		for(int i = 0; i < driveableData.bombs.length; i++)
			driveableData.bombs[i] = ItemStack.EMPTY.copy();
		for(int i = 0; i < driveableData.missiles.length; i++)
			driveableData.missiles[i] = ItemStack.EMPTY.copy();
		for(int i = 0; i < driveableData.cargo.length; i++)
			driveableData.cargo[i] = ItemStack.EMPTY.copy();
		for(EnumDriveablePart part : EnumDriveablePart.values())
		{
			driveableData.parts.put(part, new DriveablePart(part, type.health.get(part)));
		}
		initType(type, false, true);
		if(world.isClientSide() && camera == null)
		{
			camera = new EntityCamera(world, this);
			world.addFreshEntity(camera);
		}
	}
	
	public void tryRecoil()
	{
		int slot = -1;
		DriveableType type = getDriveableType();
		for(int i = driveableData.getMissileInventoryStart();
			i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++)
		{
			ItemStack shell = driveableData.getItem(i);
			if(shell != null && shell.getItem() instanceof ItemBullet &&
				type.isValidAmmo(((ItemBullet)shell.getItem()).type, EnumWeaponType.SHELL))
			{
				slot = i;
			}
		}
		
		if(recoilTimer <= 0 && slot != -1)
			isRecoil = true;
	}
	
	public void setRecoilTimer()
	{
		int slot = -1;
		DriveableType type = getDriveableType();
		for(int i = driveableData.getMissileInventoryStart();
			i < driveableData.getMissileInventoryStart() + type.numMissileSlots; i++)
		{
			ItemStack shell = driveableData.getItem(i);
			if(shell != null && shell.getItem() instanceof ItemBullet &&
				type.isValidAmmo(((ItemBullet)shell.getItem()).type, EnumWeaponType.SHELL))
			{
				slot = i;
			}
		}
		
		if(recoilTimer <= 0 && slot != -1)
			recoilTimer = getDriveableType().shootDelayPrimary;
	}
	
	private Vector3f getRandPosInBoundingBox(DriveablePart part)
	{
		// Pick a random position within the bounding box and spawn a flame there
		return axes.findLocalVectorGlobally(
			new Vector3f(part.box.x + random.nextFloat() * part.box.w,
				part.box.y + random.nextFloat() * part.box.h,
				part.box.z + random.nextFloat() * part.box.d));
	}
	
	protected void moveTowardServerPosition()
	{
		double x = getX() + (serverPosX - getX()) / serverPositionTransitionTicker;
		double y = getY() + (serverPosY - getY()) / serverPositionTransitionTicker;
		double z = getZ() + (serverPosZ - getZ()) / serverPositionTransitionTicker;
		double dYaw = Mth.wrapDegrees(serverYaw - axes.getYaw());
		double dPitch = Mth.wrapDegrees(serverPitch - axes.getPitch());
		double dRoll = Mth.wrapDegrees(serverRoll - axes.getRoll());
		float rotationYaw = (float)(axes.getYaw() + dYaw / serverPositionTransitionTicker);
		float rotationPitch = (float)(axes.getPitch() + dPitch / serverPositionTransitionTicker);
		float rotationRoll = (float)(axes.getRoll() + dRoll / serverPositionTransitionTicker);
		--serverPositionTransitionTicker;
		setPos(x, y, z);
		setRotation(rotationYaw, rotationPitch, rotationRoll);
	}
	
	public void checkForCollisions()
	{
		boolean crashInWater = false;
		double speed = getSpeedXYZ();
		for(DriveablePosition p : getDriveableType().collisionPoints)
		{
			if(driveableData.parts.get(p.part).dead)
				continue;
			Vector3f lastRelPos = prevAxes.findLocalVectorGlobally(p.position);
			Vec3 lastPos = new Vec3(xo + lastRelPos.x, yo + lastRelPos.y, zo + lastRelPos.z);
			
			Vector3f currentRelPos = axes.findLocalVectorGlobally(p.position);
			Vec3 currentPos = new Vec3(getX() + currentRelPos.x, getY() + currentRelPos.y, getZ() + currentRelPos.z);
			
			if(FlansMod.DEBUG && world.isClientSide())
			{
				world.addFreshEntity(new EntityDebugVector(world, new Vector3f(lastPos),
					Vector3f.sub(currentRelPos, lastRelPos, null), 10, 1F, 0F, 0F));
			}
			
			HitResult hit = world.clip(new ClipContext(lastPos, currentPos, ClipContext.Block.COLLIDER,
				crashInWater ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
			if(hit != null && hit.getType() == Type.BLOCK)
			{
				BlockPos pos = ((net.minecraft.world.phys.BlockHitResult)hit).getBlockPos();
				BlockState state = world.getBlockState(pos);
				
				float blockHardness = state.getDestroySpeed(world, pos);
				float damage = (float)speed;

				// unbreakable block
				if(blockHardness < 0F)
				{
					damage *= unbreakableBlockDamage * unbreakableBlockDamage;
				}
				else
				{
					damage *= blockHardness * blockHardness;
				}

				// Attack the part
				if(!attackPart(p.part, level().damageSources().inWall(), damage) 
					&& TeamsManager.driveablesBreakBlocks)
				{
					// And if it didn't die from the attack, break the block
					if(!world.isClientSide() && blockHardness <= collisionForce)
					{
						ServerLevel worldServer = (ServerLevel)world;
						destroyBlock(worldServer, pos, getDriver(), true);
					}
				}
				else
				{
					// The part died!
					world.explode(this, currentPos.x, currentPos.y, currentPos.z, 1F, false, Level.ExplosionInteraction.BLOCK);
				}
			}
			
		}
	}
	
	@Override
	public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource source)
	{
		if(distance <= 0)
			return false;
		int i = Mth.ceil(distance - 10F);
		
		if(i > 0)
			attackPart(EnumDriveablePart.core, level().damageSources().fall(), damageMultiplier * i / 5);
		return true;
	}
	
	/**
	 * Attack a certain part of a driveable and return whether it broke or not
	 */
	public boolean attackPart(EnumDriveablePart ep, DamageSource source, float damage)
	{
		DriveablePart part = driveableData.parts.get(ep);
		return part.attack(damage, source.is(DamageTypeTags.IS_FIRE));
	}
	
	/**
	 * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global
	 * coordinates
	 */
	public Vector3f rotate(Vector3f inVec)
	{
		return axes.findLocalVectorGlobally(inVec);
	}
	
	/**
	 * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global
	 * coordinates
	 */
	public Vector3f rotate(Vec3 inVec)
	{
		return rotate(inVec.x, inVec.y, inVec.z);
	}
	
	/**
	 * Takes a vector (such as the origin of a seat / gun) and translates it from local coordinates to global
	 * coordinates
	 */
	public Vector3f rotate(double x, double y, double z)
	{
		return rotate(new Vector3f((float)x, (float)y, (float)z));
	}
	
	/**
	 * Rotate the plane locally by some angle about the yaw axis
	 */
	public void rotateYaw(float rotateBy)
	{
		if(Math.abs(rotateBy) < 0.01F)
			return;
		axes.rotateLocalYaw(rotateBy);
		updatePrevAngles();
	}
	
	/**
	 * Rotate the plane locally by some angle about the pitch axis
	 */
	public void rotatePitch(float rotateBy)
	{
		if(Math.abs(rotateBy) < 0.01F)
			return;
		axes.rotateLocalPitch(rotateBy);
		updatePrevAngles();
	}
	
	/**
	 * Rotate the plane locally by some angle about the roll axis
	 */
	public void rotateRoll(float rotateBy)
	{
		if(Math.abs(rotateBy) < 0.01F)
			return;
		axes.rotateLocalRoll(rotateBy);
		updatePrevAngles();
	}
	
	public void updatePrevAngles()
	{
		// Correct angles that crossed the +/- 180 line, so that rendering doesnt make them swing 360 degrees in one tick.
		double dYaw = axes.getYaw() - yRotO;
		if(dYaw > 180)
			yRotO += 360F;
		if(dYaw < -180)
			yRotO -= 360F;
		
		double dPitch = axes.getPitch() - xRotO;
		if(dPitch > 180)
			xRotO += 360F;
		if(dPitch < -180)
			xRotO -= 360F;
		
		double dRoll = axes.getRoll() - prevRotationRoll;
		if(dRoll > 180)
			prevRotationRoll += 360F;
		if(dRoll < -180)
			prevRotationRoll -= 360F;
	}
	
	public void setRotation(float rotYaw, float rotPitch, float rotRoll)
	{
		axes.setAngles(rotYaw, rotPitch, rotRoll);
	}
	
	// Used to stop self collision
	public boolean isPartOfThis(Entity entity)
	{
		if(seats != null)
		{
			for(EntitySeat seat : seats)
			{
				if(seat == null)
					continue;
				if(entity == seat)
					return true;
				if(seat.getControllingPassenger() == entity)
					return true;
			}
		}
		if(wheels != null)
		{
			for(EntityWheel wheel : wheels)
			{
				if(entity == wheel)
					return true;
			}
		}
		return entity == this;
	}
	
	public DriveableType getDriveableType()
	{
		return DriveableType.getDriveable(driveableType);
	}
	
	public DriveableData getDriveableData()
	{
		return driveableData;
	}
	
	public boolean isDead()
	{
		return isRemoved();
	}
	
	@Override
	public Entity getControllingEntity()
	{
		if(seats != null && seats[0] != null)
			return seats[0].getControllingEntity();
		return null;
	}
	
	public ItemStack getPickedResult(HitResult target)
	{
		ItemStack stack = new ItemStack(getDriveableType().item);
		CompoundTag tags = new CompoundTag();
		driveableData.writeToNBT(tags);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tags));
		return stack;
	}
	
	
	public boolean hasFuel()
	{
		if(getDriver() == null)
			return false;
		return driverIsCreative() || driveableData.fuelInTank > 0;
	}
	
	public boolean hasEnoughFuel()
	{
		if(getDriver() == null)
			return false;
		return driverIsCreative() || driveableData.fuelInTank > driveableData.engine.fuelConsumption * throttle;
		
	}
	
	public double getSpeedXYZ()
	{
		return Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().y * getDeltaMovement().y + getDeltaMovement().z * getDeltaMovement().z);
	}
	
	public double getSpeedXZ()
	{
		return Math.sqrt(getDeltaMovement().x * getDeltaMovement().x + getDeltaMovement().z * getDeltaMovement().z);
	}
	
	/**
	 * To be overridden by vehicles to get alternate collision system
	 */
	public boolean landVehicle()
	{
		return false;
	}
	
	/**
	 * Overridden by planes for wheel parts
	 */
	public boolean gearDown()
	{
		return true;
	}
	
	/**
	 * Whether or not the plane is on the ground
	 */
	public boolean onGround()
	{
		// TODO: Replace with proper check based on wheels
		return super.onGround();
	}
	
	/**
	 * Attack method called by bullets hitting the plane. Does advanced raytracing to detect which part of the plane is
	 * hit
	 */
	public ArrayList<BulletHit> attackFromBullet(Vector3f origin, Vector3f motion)
	{
		// Make an array to contain the hits
		ArrayList<BulletHit> hits = new ArrayList<>();
		// Get the position of the bullet origin, relative to the centre of the plane, and then rotate the vectors onto local co-ordinates
		Vector3f relativePosVector = Vector3f.sub(origin, new Vector3f((float)getX(), (float)getY(), (float)getZ()), null);
		Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePosVector);
		Vector3f rotatedMotVector = axes.findGlobalVectorLocally(motion);
		// Check each part
		for(DriveablePart part : getDriveableData().parts.values())
		{
			// Ray trace the bullet
			DriveableHit hit = part.rayTrace(this, rotatedPosVector, rotatedMotVector);
			if(hit != null)
				hits.add(hit);
		}
		return hits;
	}
	
	/**
	 * Called if the bullet actually hit the part returned by the raytrace
	 *
	 * @param penetratingPower
	 */
	public float bulletHit(BulletType bulletType, float damage, DriveableHit hit, float penetratingPower)
	{
		DriveablePart part = getDriveableData().parts.get(hit.part);
		part.hitByBullet(bulletType, damage);
		
		// This is server side bsns
		if(!world.isClientSide())
		{
			checkParts();
			// If it hit, send a damage update packet
			FlansMod.getPacketHandler().sendToAllAround(new PacketDriveableDamage(this), getX(), getY(), getZ(), 100,
				0);
		}
		
		return penetratingPower - 5F;
	}
	
	/**
	 * A simple raytracer for the driveable. Called by tools
	 */
	public DriveablePart raytraceParts(Vector3f origin, Vector3f motion)
	{
		// Get the position of the bullet origin, relative to the centre of the plane, and then rotate the vectors onto local co-ordinates
		Vector3f relativePosVector = Vector3f.sub(origin, new Vector3f((float)getX(), (float)getY(), (float)getZ()), null);
		Vector3f rotatedPosVector = axes.findGlobalVectorLocally(relativePosVector);
		Vector3f rotatedMotVector = axes.findGlobalVectorLocally(motion);
		// Check each part
		for(DriveablePart part : getDriveableData().parts.values())
		{
			// Ray trace the bullet
			if(part.rayTrace(this, rotatedPosVector, rotatedMotVector) != null)
			{
				return part;
			}
		}
		return null;
	}
	
	/**
	 * For overriding for toggles such as gear up / down on planes
	 */
	public boolean canHitPart(EnumDriveablePart part)
	{
		return true;
	}
	
	/**
	 * Internal method for checking that all parts are ok, destroying broken ones, dropping items and making sure that
	 * child parts are destroyed when their parents are
	 */
	public void checkParts()
	{
		for(DriveablePart part : getDriveableData().parts.values())
		{
			if(part != null && !part.dead && part.health <= 0 && part.maxHealth > 0)
			{
				killPart(part);
			}
		}
		
		// If the core was destroyed, kill the driveable
		if(getDriveableData().parts.get(EnumDriveablePart.core).dead)
		{
			if(!world.isClientSide())
			{
				for(DriveablePart part : driveableData.parts.values())
				{
					if(part.health > 0 && !part.dead)
						killPart(part);
				}
			}
			setDead();
		}
		
	}
	
	/**
	 * Internal method for killing driveable parts
	 */
	private void killPart(DriveablePart part)
	{
		if(part.dead)
			return;
		part.health = 0;
		part.dead = true;
		
		// Drop items
		DriveableType type = getDriveableType();
		if(!world.isClientSide())
		{
			Vector3f pos = new Vector3f(0, 0, 0);
			
			// Get the midpoint of the part
			if(part.box != null)
				pos = getPartLocalVectorGlobally(part);
			
			ArrayList<ItemStack> drops = type.getItemsRequired(part, getDriveableData().engine);
			if(drops != null)
			{
				// Drop each item stack
				for(ItemStack stack : drops)
				{
					world.addFreshEntity(new ItemEntity(world, getX() + pos.x, getY() + pos.y, getZ() + pos.z, stack.copy()));
				}
			}
			dropItemsOnPartDeath(pos, part);
			
			// Inventory is in the core, so drop it if the core is broken
			if(part.type == EnumDriveablePart.core)
			{
				for(Player player : world.players())
				{
					if(player.containerMenu instanceof ContainerDriveableInventory)
					{
						if(((ContainerDriveableInventory)player.containerMenu).plane.getId() == getId())
						{
							if(player instanceof ServerPlayer)
								((ServerPlayer)player).closeContainer();
						}
					}
					else if(player.containerMenu instanceof ContainerMechaInventory)
					{
						if(((ContainerMechaInventory)player.containerMenu).mecha.getId() == getId())
						{
							if(player instanceof ServerPlayer)
								((ServerPlayer)player).closeContainer();
						}
					}
				}
				
				for(int i = 0; i < getDriveableData().getContainerSize(); i++)
				{
					ItemStack stack = getDriveableData().getItem(i);
					if(stack != null && !stack.isEmpty())
					{
						world.addFreshEntity(new ItemEntity(world, getX() + random.nextGaussian(), getY() + random.nextGaussian(),
							getZ() + random.nextGaussian(), stack));
					}
				}
			}
		}
		
		// Kill all child parts to stop things floating unconnected
		for(EnumDriveablePart child : part.type.getChildren())
		{
			killPart(getDriveableData().parts.get(child));
		}
	}
	
	private Vector3f getPartLocalVectorGlobally(DriveablePart part)
	{
		return axes.findLocalVectorGlobally(new Vector3f(
			part.box.x / 16F + part.box.w / 32F,
			part.box.y / 16F + part.box.h / 32F,
			part.box.z / 16F + part.box.d / 32F));
	}
	
	/**
	 * Method for planes, vehicles and whatnot to drop their own specific items if they wish
	 */
	protected abstract void dropItemsOnPartDeath(Vector3f midpoint, DriveablePart part);
	
	@Override
	public float getPlayerRoll()
	{
		return axes.getRoll();
	}
	
	@Override
	public float getPrevPlayerRoll()
	{
		return prevAxes.getRoll();
	}
	
	@Override
	public void explode()
	{
		
	}
	
	@Override
	public float getCameraDistance()
	{
		return getDriveableType().cameraDistance;
	}
	
	public boolean isPartIntact(EnumDriveablePart part)
	{
		DriveablePart thisPart = getDriveableData().parts.get(part);
		return thisPart.maxHealth == 0 || thisPart.health > 0;
	}
	
	public abstract boolean hasMouseControlMode();
	
	public abstract String getBombInventoryName();
	
	public abstract String getMissileInventoryName();
	
	public boolean rotateWithTurret(Seat seat)
	{
		return seat.part == EnumDriveablePart.turret;
	}
	
	@Override
	public Component getName()
	{
		DriveableType type = getDriveableType();
		return type == null ? Component.literal("") : Component.literal(type.name);
	}
	
	public boolean showInventory(int seat)
	{
		return seat != 0 || !FlansModClient.controlModeMouse;
	}
	
	public float getShootDelay(boolean secondary)
	{
		return secondary ? shootDelaySecondary : shootDelayPrimary;
	}
	
	public float getMinigunSpeed(boolean secondary)
	{
		return secondary ? minigunSpeedSecondary : minigunSpeedPrimary;
	}
	
	public int getCurrentShootPointIndex(boolean secondary)
	{
		return secondary ? currentSecondaryGunShootPointIndex : currentPrimaryGunShootPointIndex;
	}
	
	public void setShootDelay(float f, boolean secondary)
	{
		if(secondary)
			shootDelaySecondary = f;
		else shootDelayPrimary = f;
	}
	
	public void setMinigunSpeed(float f, boolean secondary)
	{
		if(secondary)
			minigunSpeedSecondary = f;
		else minigunSpeedPrimary = f;
	}
	
	public void setCurrentShootPointIndex(int i, boolean secondary)
	{
		if(secondary)
			currentSecondaryGunShootPointIndex = i;
		else currentPrimaryGunShootPointIndex = i;
	}
	
	
	@Override
	protected boolean canAddPassenger(Entity passenger)
	{
		if(passenger instanceof EntitySeat || passenger instanceof EntityWheel)
		{
			return getPassengers().size() <
				getDriveableType().numPassengers + getDriveableType().wheelPositions.length + 1;
		}
		return false;
	}
	
	@Override
	protected void addPassenger(Entity passenger)
	{
		super.addPassenger(passenger);
		if(world.isClientSide())
		{
			// We need to do some handling to work out which seat to get into. Or not?
		}
	}
	
	@Override
	protected void removePassenger(Entity passenger)
	{
		super.removePassenger(passenger);
	}
	
	public EntitySeat getSeat(LivingEntity passenger)
	{
		if(seats != null)
		{
			for(EntitySeat seat : seats)
			{
				if(seat.getControllingEntity() == passenger)
				{
					return seat;
				}
			}
		}
		return null;
	}
	
	public void registerSeat(EntitySeat seat)
	{
		if(seats == null)
			seats = new EntitySeat[seat.getExpectedSeatID() + 1];
		else if(seat.getExpectedSeatID() >= seats.length)
			seats = java.util.Arrays.copyOf(seats, seat.getExpectedSeatID() + 1);
		seats[seat.getExpectedSeatID()] = seat;
	}
	
	public void registerWheel(EntityWheel wheel)
	{
		if(wheels == null)
			wheels = new EntityWheel[wheel.getExpectedWheelID() + 1];
		else if(wheel.getExpectedWheelID() >= wheels.length)
			wheels = java.util.Arrays.copyOf(wheels, wheel.getExpectedWheelID() + 1);
		wheels[wheel.getExpectedWheelID()] = wheel;
	}
	
	public EntitySeat[] getSeats()
	{
		return seats;
	}
	
	public EntitySeat getSeat(int id)
	{
		if(seats == null)
			return null;
		if(id < 0 || id >= seats.length)
			return null;
		if(seats[id] == null)
		{
			for(Entity passenger : getPassengers())
			{
				if(passenger instanceof EntitySeat)
				{
					EntitySeat seat = (EntitySeat)passenger;
					if(seat.getExpectedSeatID() == id)
					{
						seats[id] = seat;
						seats[id].driveable = this;
						break;
					}
				}
			}
		}
		
		return seats[id];
	}
	
	public EntityWheel getWheel(int id)
	{
		if(wheels == null)
			return null;
		if(wheels[id] == null)
		{
			for(Entity passenger : getPassengers())
			{
				if(passenger instanceof EntityWheel)
				{
					EntityWheel wheel = (EntityWheel)passenger;
					if(wheel.getExpectedWheelID() == id)
					{
						wheels[id] = wheel;
						break;
					}
				}
			}
		}
		
		return wheels[id];
	}
	
	public void togglePerspective()
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON)
			mc.setCameraEntity((getCamera() == null ? mc.player : getCamera()));
		else mc.setCameraEntity(mc.player);
	}
}
