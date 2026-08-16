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
import com.flansmod.common.network.PacketDriveableKey;
import com.flansmod.common.network.PacketDriveableKeyHeld;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.network.PacketSeatUpdates;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.vector.Vector3f;

import static com.flansmod.common.PlayerHandler.floatingTickCount;

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
			if(entityInThisSeat instanceof ServerPlayer)
			{
				// Reset the floating tick count value for a player to avoid kicking them for flight detection
				try
				{
					floatingTickCount.setInt(((ServerPlayer)entityInThisSeat).connection, 0);
				}
				catch(IllegalAccessException e)
				{
					FlansMod.log.error("Failed to reset player's floating state.", e);
				}
			}
		}
		
		minigunSpeed *= 0.95F;
		minigunAngle += minigunSpeed;
	}
	
	private void updateSeatRotation() {
		
		Entity entityInThisSeat = getControllingPassenger();
		boolean isThePlayer =
				entityInThisSeat instanceof Player && FlansMod.proxy.isThePlayer((Player)entityInThisSeat);
		
		if (!isThePlayer)
			return;
		
		// Move the seat accordingly
		// Consider new Yaw and Yaw limiters
		
		float targetX = playerLooking.getYaw();
		
		float yawToMove = (targetX - looking.getYaw());
		while(yawToMove > 180F)
		{
			yawToMove -= 360F;
		}
		while(yawToMove <= -180F)
		{
			yawToMove += 360F;
		}
		
		float signDeltaX = 0;
		if(yawToMove > (seatInfo.aimingSpeed.x / 2) && !seatInfo.legacyAiming)
		{
			signDeltaX = 1;
		}
		else if(yawToMove < -(seatInfo.aimingSpeed.x / 2) && !seatInfo.legacyAiming)
		{
			signDeltaX = -1;
		}
		else
		{
			signDeltaX = 0;
		}
		
		
		// Calculate new yaw and consider yaw limiters
		float newYaw = 0f;
		
		if(seatInfo.legacyAiming || (signDeltaX == 0))
		{
			newYaw = playerLooking.getYaw();
		}
		else
		{
			newYaw = looking.getYaw() + signDeltaX * seatInfo.aimingSpeed.x;
		}
		// Since the yaw limiters go from -360 to 360, we need to find a pair of yaw values and check them both
		float otherNewYaw = newYaw - 360F;
		if(newYaw < 0)
			otherNewYaw = newYaw + 360F;
		if((!(newYaw >= seatInfo.minYaw) || !(newYaw <= seatInfo.maxYaw)) &&
				(!(otherNewYaw >= seatInfo.minYaw) || !(otherNewYaw <= seatInfo.maxYaw)))
		{
			float newYawDistFromRange =
					Math.min(Math.abs(newYaw - seatInfo.minYaw), Math.abs(newYaw - seatInfo.maxYaw));
			float otherNewYawDistFromRange =
					Math.min(Math.abs(otherNewYaw - seatInfo.minYaw), Math.abs(otherNewYaw - seatInfo.maxYaw));
			// If the newYaw is closer to the range than the otherNewYaw, move newYaw into the range
			if(newYawDistFromRange <= otherNewYawDistFromRange)
			{
				if(newYaw > seatInfo.maxYaw)
					newYaw = seatInfo.maxYaw;
				if(newYaw < seatInfo.minYaw)
					newYaw = seatInfo.minYaw;
			}
			// Else, the otherNewYaw is closer, so move it in
			else
			{
				if(otherNewYaw > seatInfo.maxYaw)
					otherNewYaw = seatInfo.maxYaw;
				if(otherNewYaw < seatInfo.minYaw)
					otherNewYaw = seatInfo.minYaw;
				// Then match up the newYaw with the otherNewYaw
				if(newYaw < 0)
					newYaw = otherNewYaw - 360F;
				else newYaw = otherNewYaw + 360F;
			}
		}
		
		// Calculate the new pitch and consider pitch limiters
		float targetY = playerLooking.getPitch();
		
		float pitchToMove = (targetY - looking.getPitch());
		while(pitchToMove > 180F)
		{
			pitchToMove -= 360F;
		}
		while(pitchToMove <= -180F)
		{
			pitchToMove += 360F;
		}
		
		float signDeltaY = 0;
		if(pitchToMove > (seatInfo.aimingSpeed.y / 2) && !seatInfo.legacyAiming)
		{
			signDeltaY = 1;
		}
		else if(pitchToMove < -(seatInfo.aimingSpeed.y / 2) && !seatInfo.legacyAiming)
		{
			signDeltaY = -1;
		}
		else
		{
			signDeltaY = 0;
		}
		
		float newPitch = 0f;
		
		
		// Pitches the gun at the last possible moment in order to reach target pitch at the same time as target yaw.
		float minYawToMove = 0f;
		
		float currentYawToMove = 0f;
		
		if(seatInfo.latePitch)
		{
			minYawToMove = ((float)Math
					.sqrt((pitchToMove / seatInfo.aimingSpeed.y) * (pitchToMove / seatInfo.aimingSpeed.y))) *
					seatInfo.aimingSpeed.x;
		}
		else
		{
			minYawToMove = 360f;
		}
		
		currentYawToMove = (float)Math.sqrt((yawToMove) * (yawToMove));
		
		if(seatInfo.legacyAiming || (signDeltaY == 0))
		{
			newPitch = playerLooking.getPitch();
		}
		else if(!seatInfo.yawBeforePitch && currentYawToMove < minYawToMove)
		{
			newPitch = looking.getPitch() + signDeltaY * seatInfo.aimingSpeed.y;
		}
		else if(seatInfo.yawBeforePitch && signDeltaX == 0)
		{
			newPitch = looking.getPitch() + signDeltaY * seatInfo.aimingSpeed.y;
		}
		else if(seatInfo.yawBeforePitch)
		{
			newPitch = looking.getPitch();
		}
		else
		{
			newPitch = looking.getPitch();
		}
		
		if(newPitch > -seatInfo.minPitch)
			newPitch = -seatInfo.minPitch;
		if(newPitch < -seatInfo.maxPitch)
			newPitch = -seatInfo.maxPitch;
		
		
		if(looking.getYaw() != newYaw || looking.getPitch() != newPitch)
		{
			// Now set the new angles
			prevLooking = looking.clone();
			looking.setAngles(newYaw, newPitch, 0F);
			FlansMod.getPacketHandler().sendToServer(new PacketSeatUpdates(this));
		}
		
		playYawSound = signDeltaX != 0 && seatInfo.traverseSounds;
		
		if(signDeltaY != 0 && !seatInfo.yawBeforePitch && currentYawToMove < minYawToMove)
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
			
			if((Math.abs(prevPlayerPosX - x) > 100d
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
	
	public LivingEntity getCamera()
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
		DriveableType type = DriveableType.getDriveable(tags.getStringOr("DriveableType", ""));
		seatID = tags.getIntOr("Index", 0);
		entityData.set(SEAT, seatID);
		
		if(type == null)
		{
			FlansMod.log.warn("Killing seat due to invalid type tag");
			reallySetDead();
			return;
		}
		
		seatInfo = type.seats[seatID];
		
		if(getVehicle() instanceof EntityDriveable)
		{
			driveable = (EntityDriveable)getVehicle();
			driveable.registerSeat(this);
			entityData.set(DRIVEABLE, driveable.getId());
		}
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
	public void onMouseMoved(int deltaX, int deltaY)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(driveable == null)
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
			float newPlayerYaw = playerLooking.getYaw() + deltaX / lookSpeed * (float)(double)mc.options.sensitivity().get();
			float newPlayerPitch = playerLooking.getPitch() - deltaY / lookSpeed * (float)(double)mc.options.sensitivity().get();
			
			if(newPlayerPitch > -seatInfo.minPitch)
				newPlayerPitch = -seatInfo.minPitch;
			if(newPlayerPitch < -seatInfo.maxPitch)
				newPlayerPitch = -seatInfo.maxPitch;

			// Since the yaw limiters go from -360 to 360, we need to find a pair of yaw values and check them both
			float otherNewPlayerYaw = newPlayerYaw - 360F;
			if(newPlayerYaw < 0)
				otherNewPlayerYaw = newPlayerYaw + 360F;
			if((newPlayerYaw >= seatInfo.minYaw && newPlayerYaw <= seatInfo.maxYaw) ||
					(otherNewPlayerYaw >= seatInfo.minYaw && otherNewPlayerYaw <= seatInfo.maxYaw))
			{
				//All is well
			}
			else
			{
				float newPlayerYawDistFromRange =
						Math.min(Math.abs(newPlayerYaw - seatInfo.minYaw), Math.abs(newPlayerYaw - seatInfo.maxYaw));
				float otherPlayerNewYawDistFromRange = Math.min(Math.abs(otherNewPlayerYaw - seatInfo.minYaw),
						Math.abs(otherNewPlayerYaw - seatInfo.maxYaw));
				// If the newYaw is closer to the range than the otherNewYaw, move newYaw into the range
				if(newPlayerYawDistFromRange <= otherPlayerNewYawDistFromRange)
				{
					if(newPlayerYaw > seatInfo.maxYaw)
						newPlayerYaw = seatInfo.maxYaw;
					if(newPlayerYaw < seatInfo.minYaw)
						newPlayerYaw = seatInfo.minYaw;
				}
				// Else, the otherNewYaw is closer, so move it in
				else
				{
					if(otherNewPlayerYaw > seatInfo.maxYaw)
						otherNewPlayerYaw = seatInfo.maxYaw;
					if(otherNewPlayerYaw < seatInfo.minYaw)
						otherNewPlayerYaw = seatInfo.minYaw;
					//Then match up the newYaw with the otherNewYaw
					if(newPlayerYaw < 0)
						newPlayerYaw = otherNewPlayerYaw - 360F;
					else newPlayerYaw = otherNewPlayerYaw + 360F;
				}
			}
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
			FlansMod.proxy.openDriveableMenu(player, world, driveable);
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
			return false;
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
