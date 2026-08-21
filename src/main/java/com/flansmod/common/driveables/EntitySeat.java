package com.flansmod.common.driveables;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import com.flansmod.api.IControllable;
import com.flansmod.client.FlansModClient;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.RotatedAxes;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.FireableGun;
import com.flansmod.common.guns.FiredShot;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.ShotHandler;
import com.flansmod.common.network.PacketDriveableGUI;
import com.flansmod.common.network.PacketDriveableKey;
import com.flansmod.common.network.PacketDriveableKeyHeld;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.network.PacketSeatUpdates;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.vector.Vector3f;


public class EntitySeat extends Entity implements IControllable
{
	private int driveableID;
	private int seatID;
	public EntityDriveable driveable;
	
	public float playerRoll, prevPlayerRoll;
	
	public Seat seatInfo;
	public RotatedAxes playerLooking;
	public RotatedAxes prevPlayerLooking;
	/**
	 * A set of axes used to calculate where the player is looking, x axis is the direction of looking, y is up
	 */
	public RotatedAxes looking;
	/**
	 * For smooth rendering
	 */
	public RotatedAxes prevLooking;
	/**
	 * Delay ticker for shooting guns
	 */
	public float gunDelay;
	/**
	 * Minigun speed
	 */
	public float minigunSpeed;
	/**
	 * Minigun angle for render
	 */
	public float minigunAngle;
	
	/**
	 * Sound delay ticker for looping sounds
	 */
	public int soundDelay;
	public int yawSoundDelay = 0;
	public int pitchSoundDelay = 0;
	
	public boolean playYawSound = false;
	public boolean playPitchSound = false;
	
	
	private double playerPosX, playerPosY, playerPosZ;
	private float playerYaw, playerPitch;
	/**
	 * For smoothness
	 */
	private double prevPlayerPosX, prevPlayerPosY, prevPlayerPosZ;
	private float prevPlayerYaw, prevPlayerPitch;
	private boolean shooting;
	/**
	 * Runtime-only flag (not synced or saved) marking that playerPos* hold a
	 * valid position. Server-side seats set this in the constructor and on
	 * mount; client-side seats set it lazily when their passenger first
	 * appears, so the local player is never positioned at (0,0,0).
	 */
	private boolean playerPosInitialised = false;
	
	private static final EntityDataAccessor<Integer> DRIVEABLE =
		SynchedEntityData.defineId(EntitySeat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SEAT =
		SynchedEntityData.defineId(EntitySeat.class, EntityDataSerializers.INT);
	
	/** The level this entity is in, mirrors the 1.12.2 world field */
	protected Level world;
	
	/**
	 * Default constructor for spawning client side Should not be called server side EVER
	 */
		public EntitySeat(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
		prevLooking = new RotatedAxes();
		looking = new RotatedAxes();
		playerLooking = new RotatedAxes();
		prevPlayerLooking = new RotatedAxes();
		//26.1.2 caches the dimensions from the EntityType in the constructor,
		//so the small seat hitbox must be applied explicitly
		refreshDimensions();
	}

	/**
	 * Seats are invisible helper entities, so they keep a small hitbox. The
	 * default 0.6x1.8 entity box stuck far out of the plane model and let
	 * players mount by right-clicking the air above the plane.
	 */
	@Override
	public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose)
	{
		return net.minecraft.world.entity.EntityDimensions.scalable(0.6F, 0.6F);
	}

public EntitySeat(Level world)
	{
		this(ModEntities.SEAT, world);
		this.world = level();

		prevLooking = new RotatedAxes();
		looking = new RotatedAxes();
		playerLooking = new RotatedAxes();
		prevPlayerLooking = new RotatedAxes();
	}
	
	/**
	 * Server side seat constructor
	 */
	public EntitySeat(Level world, EntityDriveable d, int id)
	{
		this(world);
		driveable = d;
		driveableID = d.getId();
		entityData.set(DRIVEABLE, driveableID);
		seatInfo = driveable.getDriveableType().seats[id];
		seatID = id;
		entityData.set(SEAT, seatID);
		setPos(d.getX(), d.getY(), d.getZ());
		playerPosX = prevPlayerPosX = getX();
		playerPosY = prevPlayerPosY = getY();
		playerPosZ = prevPlayerPosZ = getZ();
		playerPosInitialised = true;
		looking.setAngles((seatInfo.minYaw + seatInfo.maxYaw) / 2, 0F, 0F);
		prevLooking.setAngles((seatInfo.minYaw + seatInfo.maxYaw) / 2, 0F, 0F);
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DRIVEABLE, -1);
		builder.define(SEAT, -1);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(driveable == null)
		{
			driveableID = entityData.get(DRIVEABLE);
			seatID = entityData.get(SEAT);
			if(getVehicle() instanceof EntityDriveable)
			{
				driveable = (EntityDriveable)getVehicle();
				driveable.registerSeat(this);
			}
			else if(driveableID >= 0 && world.getEntity(driveableID) instanceof EntityDriveable)
			{
				driveable = (EntityDriveable)world.getEntity(driveableID);
				driveable.registerSeat(this);
			}
			if(driveable == null)
			{
				return;
			}
		}
		
		if(seatInfo == null && driveable != null && driveable.getDriveableType() != null && seatID >= 0
			&& seatID < driveable.getDriveableType().seats.length)
		{
			seatInfo = driveable.getDriveableType().seats[seatID];
		}
		
		// Update gun delay ticker
		if(gunDelay > 0)
			gunDelay--;
		// Update sound delay ticker
		if(soundDelay > 0)
			soundDelay--;
		if(yawSoundDelay > 0)
			yawSoundDelay--;
		if(pitchSoundDelay > 0)
			pitchSoundDelay--;
		
		if(playYawSound && yawSoundDelay == 0 && seatInfo.traverseSounds)
		{
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), 50, 0, seatInfo.yawSound, false);
			yawSoundDelay = seatInfo.yawSoundLength;
		}
		
		if(playPitchSound && pitchSoundDelay == 0 && seatInfo.traverseSounds)
		{
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), 50, 0, seatInfo.pitchSound, false);
			pitchSoundDelay = seatInfo.pitchSoundLength;
		}
		
		Entity entityInThisSeat = getControllingPassenger();
		boolean isThePlayer =
				entityInThisSeat instanceof Player && FlansMod.proxy.isThePlayer((Player)entityInThisSeat);
		
		// Reset traverse sounds if player exits the vehicle
		if(!isThePlayer)
		{
			playYawSound = false;
			playPitchSound = false;
			yawSoundDelay = 0;
			pitchSoundDelay = 0;
		}
		
		// If on the client
		if(world.isClientSide())
		{
			if(isDriverSeat() && isThePlayer && FlansMod.proxy.mouseControlEnabled() && driveable.hasMouseControlMode())
			{
				looking = new RotatedAxes();
				playerLooking = new RotatedAxes();
			}
			
			if(entityInThisSeat instanceof Player && shooting)
			{
				pressKey(9, (Player)entityInThisSeat, false);
			}
		}
		else
		{

		}
		
		minigunSpeed *= 0.95F;
		minigunAngle += minigunSpeed;
	}
	
	private void updateSeatRotation() {
		
		if(playerLooking == null || seatInfo == null)
			return;
		
		Entity entityInThisSeat = getControllingPassenger();
		boolean isThePlayer =
				entityInThisSeat instanceof Player && FlansMod.proxy.isThePlayer((Player)entityInThisSeat);
		
		if (!isThePlayer)
			return;
		
		// Move the seat accordingly
		// Consider new Yaw and Yaw limiters
		SeatLookMath.LookUpdate update = SeatLookMath.updateLook(
				playerLooking.getYaw(), playerLooking.getPitch(),
				looking.getYaw(), looking.getPitch(),
				seatInfo.minYaw, seatInfo.maxYaw, seatInfo.minPitch, seatInfo.maxPitch,
				seatInfo.aimingSpeed.x, seatInfo.aimingSpeed.y,
				seatInfo.legacyAiming, seatInfo.yawBeforePitch, seatInfo.latePitch);
		
		float newYaw = update.newYaw;
		float newPitch = update.newPitch;
		int signDeltaX = update.signDeltaX;
		int signDeltaY = update.signDeltaY;
		
		if(looking.getYaw() != newYaw || looking.getPitch() != newPitch)
		{
			// Now set the new angles
			prevLooking = looking.clone();
			looking.setAngles(newYaw, newPitch, 0F);
			FlansMod.getPacketHandler().sendToServer(new PacketSeatUpdates(this));
		}
		
		playYawSound = signDeltaX != 0 && seatInfo.traverseSounds;
		
		if(signDeltaY != 0 && !seatInfo.yawBeforePitch && update.currentYawToMove < update.minYawToMove)
		{
			playPitchSound = true;
		}
		else playPitchSound = signDeltaY != 0 && seatInfo.yawBeforePitch && signDeltaX == 0;
	}
	
	/**
	 * Set the position to be that of the driveable plus the local position, rotated
	 */
	public void updatePosition()
	{
		if(driveable == null)
		{
			if(getVehicle() instanceof EntityDriveable)
			{
				driveable = (EntityDriveable)getVehicle();
			}
			else
			{
				return;
			}
		}
		
		if(seatInfo == null)
			seatInfo = driveable.getDriveableType().seats[seatID];
		
		if (world.isClientSide())
			updateSeatRotation();
		
		prevPlayerPosX = playerPosX;
		prevPlayerPosY = playerPosY;
		prevPlayerPosZ = playerPosZ;
		
		prevPlayerYaw = playerYaw;
		prevPlayerPitch = playerPitch;
		prevPlayerRoll = playerRoll;
		
		// Get the position of this seat on the driveable axes
		Vector3f localPosition = new Vector3f(seatInfo.x / 16F, seatInfo.y / 16F, seatInfo.z / 16F);
		
		// Rotate the offset vector by the turret yaw
		if(driveable != null && driveable.getSeat(0) != null && driveable.getSeat(0).looking != null)
		{
			RotatedAxes yawOnlyLooking = new RotatedAxes(driveable.getSeat(0).looking.getYaw(), 0F, 0F);
			Vector3f rotatedOffset = yawOnlyLooking.findLocalVectorGlobally(seatInfo.rotatedOffset);
			Vector3f.add(localPosition, new Vector3f(rotatedOffset.x, 0F, rotatedOffset.z), localPosition);
		}
		
		// Get the position of this seat globally, but positionally relative to the driveable
		Vector3f relativePosition = driveable.axes.findLocalVectorGlobally(localPosition);
		
		if(Math.abs(driveable.getX() + relativePosition.x - getX()) > 100d
		|| Math.abs(driveable.getY() + relativePosition.y - getY()) > 100d
		|| Math.abs(driveable.getZ() + relativePosition.z - getZ()) > 100d)
		{
			FlansMod.log.warn("Seat was made to move stupid distance in a frame, cancelling");
		}
		else
		{
			// Set the absol
			setPos(driveable.getX() + relativePosition.x, driveable.getY() + relativePosition.y,
					driveable.getZ() + relativePosition.z);
		}
		
		Entity entityInThisSeat = getControllingPassenger();
		
		if(entityInThisSeat != null)
		{
			DriveableType type = driveable.getDriveableType();
			Vec3 yOffset =
					driveable.axes.findLocalVectorGlobally(new Vector3f(0, entityInThisSeat.getEyeHeight() * 3 / 4, 0))
							.toVec3().subtract(0, entityInThisSeat.getEyeHeight(), 0);
			// driveable.rotate(0, riddenByEntity.getYOffset(), 0).toVec3();
			
			double x = getX() + yOffset.x;
			double y = getY() + yOffset.y;
			double z = getZ() + yOffset.z;
			
			if(!playerPosInitialised)
			{
				// Client-side lazy init: playerPos* default to 0, so snap the
				// player straight to the seat instead of teleporting them to
				// the world origin for one frame
				playerPosX = prevPlayerPosX = x;
				playerPosY = prevPlayerPosY = y;
				playerPosZ = prevPlayerPosZ = z;
				entityInThisSeat.setPos(x, y, z);
				entityInThisSeat.xOld = entityInThisSeat.xo = prevPlayerPosX;
				entityInThisSeat.yOld = entityInThisSeat.yo = prevPlayerPosY;
				entityInThisSeat.zOld = entityInThisSeat.zo = prevPlayerPosZ;
				playerPosInitialised = true;
			}
			else if((Math.abs(prevPlayerPosX - x) > 100d
			|| Math.abs(prevPlayerPosY - y) > 100d
			|| Math.abs(prevPlayerPosZ - z) > 100d)
			&& prevPlayerPosY > 0.00001d)
			{
				
				FlansMod.log.warn("Player was made to move stupid distance in a frame, cancelling");
			}
			else
			{
				// Set the absol
				entityInThisSeat.setPos(playerPosX, playerPosY, playerPosZ);
				playerPosX = x;
				playerPosY = y;
				playerPosZ = z;
				
				entityInThisSeat.xOld = entityInThisSeat.xo = prevPlayerPosX;
				entityInThisSeat.yOld = entityInThisSeat.yo = prevPlayerPosY;
				entityInThisSeat.zOld = entityInThisSeat.zo = prevPlayerPosZ;
			}
			
			// Calculate the local look axes globally
			if(playerLooking != null)
			{
				RotatedAxes globalLookAxes = driveable.axes.findLocalAxesGlobally(playerLooking);
				// Set the player's rotation based on this
				playerYaw = -90F + globalLookAxes.getYaw();
				playerPitch = globalLookAxes.getPitch();
				
				double dYaw = playerYaw - prevPlayerYaw;
				if(dYaw > 180)
					prevPlayerYaw += 360F;
				if(dYaw < -180)
					prevPlayerYaw -= 360F;
				
				if(entityInThisSeat instanceof Player)
				{
					entityInThisSeat.yRotO = prevPlayerYaw;
					entityInThisSeat.xRotO = prevPlayerPitch;
					
					entityInThisSeat.setYRot(playerYaw);
					entityInThisSeat.setXRot(playerPitch);
				}
				
				// If the entity is a player, roll its view accordingly
				if(world.isClientSide())
				{
					playerRoll = -globalLookAxes.getRoll();
				}
			}
		}
	}
	
	public net.minecraft.world.entity.Entity getCamera()
	{
		return driveable.getCamera();
	}
	
	public boolean canBeCollidedWith(Entity other)
	{
		return !isRemoved();
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
		CompoundTag tags = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		seatID = tags.getIntOr("Index", 0);
		entityData.set(SEAT, seatID);
		
		//The driveable may not be attached yet when the seat loads (the
		//passenger chain is restored after load), so resolve it lazily in
		//tick() rather than killing the seat here.
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		CompoundTag tags = new CompoundTag();
		tags.putString("DriveableType", driveable == null ? "" : driveable.getDriveableType().shortName);
		tags.putInt("Index", seatID);
		output.store("FlanData", CompoundTag.CODEC, tags);
	}
	
	@Override
	public void onMouseMoved(double deltaX, double deltaY)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(driveable == null)
			return;
		
		if(playerLooking == null || prevPlayerLooking == null)
			return;
		
		prevLooking = looking.clone();
		prevPlayerLooking = playerLooking.clone();
		
		// Driver seat should pass input to driveable
		if(isDriverSeat())
		{
			driveable.onMouseMoved(deltaX, deltaY);
		}
		// Other seats should look around, but also the driver seat if mouse control mode is disabled
		if(!isDriverSeat() || !FlansModClient.controlModeMouse || !driveable.hasMouseControlMode())
		{
			float lookSpeed = 4F;
			
			// Angle stuff for the player
			// Calculate the new pitch yaw while considering limiters
			float newPlayerYaw = (float)(playerLooking.getYaw() + deltaX / lookSpeed * (float)(double)mc.options.sensitivity().get());
			float newPlayerPitch = (float)(playerLooking.getPitch() - deltaY / lookSpeed * (float)(double)mc.options.sensitivity().get());
			
			newPlayerPitch = SeatLookMath.clampPitchToLimits(newPlayerPitch, seatInfo.minPitch, seatInfo.maxPitch);
			
			// Since the yaw limiters go from -360 to 360, we need to find a pair of yaw values and check them both
			newPlayerYaw = SeatLookMath.clampYawToLimits(newPlayerYaw, seatInfo.minYaw, seatInfo.maxYaw);
			
			// Now set the new angles
			playerLooking.setAngles(newPlayerYaw, newPlayerPitch, 0F);
			
		}
	}
	
	@Override
	public void updateKeyHeldState(int key, boolean held)
	{
		if(world.isClientSide() && driveable != null)
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableKeyHeld(key, held));
			
		}
		if(isDriverSeat())
		{
			driveable.updateKeyHeldState(key, held);
		}
		else if(key == 9)
		{
			shooting = held;
		}
	}
	
	@Override
	public boolean pressKey(int key, Player player, boolean isOnTick)
	{
		// Driver seat should pass input to driveable
		if(isDriverSeat() && driveable != null)
		{
			return driveable.pressKey(key, player, isOnTick);
		}
		
		if(world.isClientSide() && key == 7 && driveable != null)
		{
			FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MENU));
		}
		
		if(world.isClientSide())
		{
			if(driveable != null)
			{
				FlansMod.getPacketHandler().sendToServer(new PacketDriveableKey(key));
				//setting client side minigun speed for animation
				if(key == 9)
					minigunSpeed += 0.1F;
			}
		}
		return false;
	}
	
	@Override
	public boolean serverHandleKeyPress(int key, Player player)
	{
		switch (key)
		{
			case 9:
				// Get the gun from the plane type and the ammo from the data
				GunType gun = seatInfo.gunType;
				
				//setting server side minigun speed
				minigunSpeed += 0.15F;
				if(gun != null && gun.mode != EnumFireMode.MINIGUN || minigunSpeed > 2F)
				{
					if(gunDelay <= 0 && TeamsManager.bulletsEnabled && seatInfo.gunnerID < driveable.getDriveableData().ammo.length)
					{
						
						ItemStack bulletItemStack = driveable.getDriveableData().ammo[seatInfo.gunnerID];
						// Check that neither is null and that the bullet item is actually a bullet
						if(gun != null && bulletItemStack != null && bulletItemStack.getItem() instanceof ItemShootable)
						{
							ShootableType bullet = ((ItemShootable)bulletItemStack.getItem()).type;
							if(gun.isCorrectAmmo(bullet))
							{
								// Gun origin
								Vector3f gunOrigin = Vector3f.add(driveable.axes.findLocalVectorGlobally(seatInfo.gunOrigin), new Vector3f((float)driveable.getX(), (float)driveable.getY(), (float)driveable.getZ()), null);
								// Calculate the look axes globally
								Vector3f shootVec = driveable.axes.findLocalVectorGlobally(looking.getXAxis());
								// Calculate the origin of the bullets
								Vector3f yOffset = driveable.axes
										.findLocalVectorGlobally(new Vector3f(0F, (float)player.getEyeHeight(), 0F));
								
								FireableGun fireableGun = new FireableGun(gun, gun.damage, gun.bulletSpread, gun.bulletSpeed, gun.spreadPattern);
								//TODO unchecked cast, grenades wont work (currently no vehicle with this feature exists)
								FiredShot shot = new FiredShot(fireableGun, (BulletType) bullet, this, (ServerPlayer)getControllingPassenger());
								ShotHandler.fireGun(world, shot, gun.numBullets*bullet.numBullets, Vector3f.add(yOffset, new Vector3f(gunOrigin.x, gunOrigin.y, gunOrigin.z), null), shootVec);
								// Play the shoot sound
								if(soundDelay <= 0)
								{
									PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, 0,
											gun.shootSound, false);
									soundDelay = gun.shootSoundLength;
								}
								//use ammo (unless in creative)
								if(!((Player)getControllingPassenger()).getAbilities().instabuild)
								{
									// Get the bullet item damage and increment it
									int damage = bulletItemStack.getDamageValue();
									bulletItemStack.setDamageValue(damage + 1);
									// If the bullet item is completely damaged (empty)
									if(damage + 1 >= bulletItemStack.getMaxDamage())
									{
										//Set the damage to 0 and consume one ammo item
										bulletItemStack.setDamageValue(0);
										bulletItemStack.setCount(bulletItemStack.getCount()-1);
										if (bulletItemStack.getCount() <= 0)
											bulletItemStack = ItemStack.EMPTY.copy();
										
										driveable.getDriveableData().ammo[seatInfo.gunnerID] = bulletItemStack;
									}
								}
								// Reset the shoot delay
								gunDelay = gun.shootDelay;
							}
						}
					}
				}
				return true;
		}
		return false;
	}
	
	public boolean processInitialInteract(Player entityplayer,
										  InteractionHand hand) //interact : change back when Forge updates
	{
		if(isRemoved())
			return false;
		if(world.isClientSide())
		{
			//Consume the click client side so the vanilla engine does not
			//fall back to attacking the seat. The server mounts the player.
			return true;
		}
		if(driveable == null)
			return false;
		// If they are using a repair tool, don't put them in
		ItemStack currentItem = entityplayer.getMainHandItem();
		if(currentItem.getItem() instanceof ItemTool && ((ItemTool)currentItem.getItem()).type.healDriveables)
			return true;
		if(currentItem.getItem() instanceof LeadItem)
		{
			if(getControllingPassenger() instanceof Animal)
			{
				// Minecraft will handle dismounting the mob
				return true;
			}
			
			double checkRange = 10;
			List<Animal> nearbyAnimals = new java.util.ArrayList<>();
			for(Entity e : world.getEntities((Entity)null,
					new AABB(getX() - checkRange, getY() - checkRange, getZ() - checkRange, getX() + checkRange,
							getY() + checkRange, getZ() + checkRange), ent -> ent instanceof Animal))
			{
				nearbyAnimals.add((Animal)e);
			}
			for(Animal animal : nearbyAnimals)
			{
				if(animal.isLeashed() && animal.getLeashHolder() == entityplayer)
				{
					if(animal.startRiding(this))
					{
						looking.setAngles(-animal.getYRot(), animal.getXRot(), 0F);
						animal.dropLeash();
						playerPosX = prevPlayerPosX = animal.getX();
						playerPosY = prevPlayerPosY = animal.getY();
						playerPosZ = prevPlayerPosZ = animal.getZ();
					}
					else
					{
						FlansMod.log.warn("Failed to put pet in seat");
					}
				}
			}
			return true;
		}
		// Put them in the seat
		if(getControllingPassenger() == null && !driveable.getDriveableData().engine.isAIChip)
		{
			if(entityplayer.startRiding(this))
			{
				playerPosX = prevPlayerPosX = entityplayer.getX();
				playerPosY = prevPlayerPosY = entityplayer.getY();
				playerPosZ = prevPlayerPosZ = entityplayer.getZ();
				playerPosInitialised = true;
			}
			else
			{
				FlansMod.log.warn("Failed to mount seat");
			}
			return true;
		}
		return false;
	}
	
	@Override
	public Entity getControllingEntity()
	{
		return getControllingPassenger();
	}
	
	@Override
	public LivingEntity getControllingPassenger()
	{
		return getPassengers().isEmpty() ? null : (LivingEntity)getPassengers().get(0);
	}
	
	public boolean isDead()
	{
		return isRemoved();
	}
	
	public void reallySetDead()
	{
		discard();
	}
	
	public EntitySeat getSeat(LivingEntity living)
	{
		return this;
	}
  
	public boolean isDriverSeat()
	{
		return seatID == 0;
	}
	
	@Override
	public void rideTick()
	{
		if(driveable != null || getVehicle() != null)
			tick();
	}
	
	@Override
	protected void positionRider(Entity passenger, MoveFunction moveFunction)
	{
		// Seat and passenger positions are managed manually by updatePosition()
		// in the driveable tick; the vanilla passenger attachment point would
		// place the player 1.8 blocks above the seat every tick.
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 pos)
	{
		return processInitialInteract(player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}
	
	public ItemStack getPickedResult(HitResult target)
	{
		if(driveable == null)
			return ItemStack.EMPTY.copy();
		return driveable.getPickedResult(target);
	}
	
	@Override
	public float getPlayerRoll()
	{
		return playerRoll;
	}
	
	@Override
	public float getPrevPlayerRoll()
	{
		return prevPlayerRoll;
	}
	
	@Override
	public float getCameraDistance()
	{
		return driveable != null && seatID == 0 ? driveable.getDriveableType().cameraDistance * 2.0f : 5F;
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float f)
	{
		if(driveable == null)
			return false;
		return driveable.hurtOrSimulate(source, f);
	}
	
	public int getExpectedSeatID()
	{
		return seatID;
	}
	
	public float getMinigunSpeed()
	{
		return minigunSpeed;
	}
	
}
