package com.flansmod.common.driveables;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.network.PacketDriveableControl;
import com.flansmod.common.network.PacketDriveableGUI;
import com.flansmod.common.network.PacketPlaneControl;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.vector.Matrix4f;
import com.flansmod.common.vector.Vector3f;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public class EntityPlane extends EntityDriveable
{
	/**
	 * The flap positions, used for rendering and for controlling the plane rotations
	 */
	public float flapsYaw, flapsPitchLeft, flapsPitchRight;
	/**
	 * Position of looping engine sound
	 */
	public int soundPosition;
	/**
	 * The angle of the propeller for the renderer
	 */
	public float propAngle;
	/**
	 * Weapon delays
	 */
	public int bombDelay, gunDelay;
	/**
	 * Despawn timer
	 */
	public int ticksSinceUsed = 0;
	/**
	 * Mostly aesthetic model variables. Gear actually has a variable hitbox
	 */
	public boolean varGear = true, varDoor = false, varWing = false;
	/**
	 * Delayer for gear, door and wing buttons
	 */
	public int toggleTimer = 0;
	/**
	 * Current plane mode
	 */
	public EnumPlaneMode mode;
	
		public EntityPlane(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityPlane(Level world)
	{
		this(ModEntities.PLANE, world);
		this.world = level();
	}
	
	public EntityPlane(Level world, double x, double y, double z, PlaneType type, DriveableData data)
	{
		super(ModEntities.PLANE, world, type, data);
		this.world = level();
		setPos(x, y, z);
		xo = x;
		yo = y;
		zo = z;
		initType(type, true, false);
	}
	
	public EntityPlane(Level world, double x, double y, double z, Player placer, PlaneType type,
					   DriveableData data)
	{
		this(world, x, y, z, type, data);
		rotateYaw(placer.getYRot() + 90F);
		rotatePitch(type.restingPitch);
		//The spawned orientation is also the "previous" orientation, so the
		//first-tick checkForCollisions sweep does not raytrace from identity
		//through the ground
		prevAxes = axes.clone();
	}
	
	@Override
	public void initType(DriveableType type, boolean firstSpawn, boolean clientSide)
	{
		super.initType(type, firstSpawn, clientSide);
		mode = (((PlaneType)type).mode == EnumPlaneMode.HELI ? EnumPlaneMode.HELI : EnumPlaneMode.PLANE);
	}
	
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output)
	{
		super.addAdditionalSaveData(output);
		CompoundTag tags = new CompoundTag();
		tags.putBoolean("VarGear", varGear);
		tags.putBoolean("VarDoor", varDoor);
		tags.putBoolean("VarWing", varWing);
		output.store("FlanDataPlane", CompoundTag.CODEC, tags);
	}
	
	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input)
	{
		super.readAdditionalSaveData(input);
		CompoundTag tags = input.read("FlanDataPlane", CompoundTag.CODEC).orElse(new CompoundTag());
		varGear = tags.getBooleanOr("VarGear", false);
		varDoor = tags.getBooleanOr("VarDoor", false);
		varWing = tags.getBooleanOr("VarWing", false);
	}
	
	/**
	 * Called with the movement of the mouse. Used in controlling vehicles if need be.
	 *
	 * @param deltaY
	 * @param deltaX
	 */
	@Override
	public void onMouseMoved(double deltaX, double deltaY)
	{
		if(!world.isClientSide())
			return;
		if(!FlansMod.proxy.mouseControlEnabled())
			return;
		
		float sensitivity = 0.02F;
		
		flapsPitchLeft -= sensitivity * deltaY;
		flapsPitchRight -= sensitivity * deltaY;
		
		flapsPitchLeft -= sensitivity * deltaX;
		flapsPitchRight += sensitivity * deltaX;
	}
	
	@Override
	public void setPositionRotationAndMotion(double x, double y, double z, float yaw, float pitch, float roll,
											 double motX, double motY, double motZ, float velYaw, float velPitch,
											 float velRoll, float throttle, float steeringYaw)
	{
		super.setPositionRotationAndMotion(x, y, z, yaw, pitch, roll, motX, motY, motZ, velYaw, velPitch, velRoll,
				throttle, steeringYaw);
		flapsYaw = steeringYaw;
	}
	
	@Override
	public InteractionResult interact(Player entityplayer, InteractionHand hand, Vec3 pos)
	{
		if(isRemoved())
			return InteractionResult.PASS;
		if(world.isClientSide())
			return InteractionResult.PASS;
		
		//If they are using a repair tool, don't put them in
		ItemStack currentItem = entityplayer.getMainHandItem();
		if(currentItem.getItem() instanceof ItemTool && ((ItemTool)currentItem.getItem()).type.healDriveables)
			return InteractionResult.PASS;
		
		PlaneType type = this.getPlaneType();
		//Check each seat in order to see if the player can sit in it
		for(int i = 0; i <= type.numPassengers; i++)
		{
			if(getSeat(i).processInitialInteract(entityplayer, hand))
			{
				if(i == 0)
				{
					bombDelay = type.planeBombDelay;
					FlansMod.proxy.doTutorialStuff(entityplayer, this);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
	
	public boolean serverHandleKeyPress(int key, Player player)
	{
		return super.serverHandleKeyPress(key, player);
	}
	
	@Override
	public boolean pressKey(int key, Player player, boolean isOnEvent)
	{
		PlaneType type = this.getPlaneType();
		//Send keys which require server side updates to the server
		boolean canThrust = ((getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof Player
				&& ((Player)getSeat(0).getControllingPassenger()).getAbilities().instabuild)
				|| getDriveableData().fuelInTank > 0) && hasWorkingProp();
		switch(key)
		{
			case 0: //Accelerate : Increase the throttle, up to 1.
			{
				if(canThrust || throttle < 0F)
				{
					throttle += 0.002F;
					if(throttle > 1F)
						throttle = 1F;
				}
				return true;
			}
			case 1: //Decelerate : Decrease the throttle, down to -1, or 0 if the plane cannot reverse
			{
				if(canThrust || throttle > 0F)
				{
					throttle -= 0.005F;
					if(throttle < -1F)
						throttle = -1F;
					if(throttle < 0F && type.maxNegativeThrottle == 0F)
						throttle = 0F;
				}
				return true;
			}
			case 2: //Left : Yaw the flaps left
			{
				flapsYaw -= 1F;
				return true;
			}
			case 3: //Right : Yaw the flaps right
			{
				flapsYaw += 1F;
				return true;
			}
			case 4: //Up : Pitch the flaps up
			{
				flapsPitchLeft += 1F;
				flapsPitchRight += 1F;
				return true;
			}
			case 5: //Down : Pitch the flaps down
			{
				flapsPitchLeft -= 1F;
				flapsPitchRight -= 1F;
				return true;
			}
			case 7: //Inventory : Check to see if this plane allows in-flight inventory editing or if the plane is on the ground
			{
				if(world.isClientSide() && (type.invInflight || (Math.abs(throttle) < 0.1F && onGround())))
				{
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableGUI(PacketDriveableGUI.MENU));
				}
				return true;
			}
			case 10: //Change control mode
			{
				FlansMod.proxy.changeControlMode((Player)getSeat(0).getControllingPassenger());
				return true;
			}
			case 11: //Roll left
			{
				flapsPitchLeft += 1F;
				flapsPitchRight -= 1F;
				return true;
			}
			case 12: //Roll right
			{
				flapsPitchLeft -= 1F;
				flapsPitchRight += 1F;
				return true;
			}
			case 13: // Gear
			{
				if(toggleTimer <= 0)
				{
					varGear = !varGear;
					player.sendSystemMessage(Component.literal("Landing gear " + (varGear ? "down" : "up")));
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 14: // Door
			{
				if(toggleTimer <= 0)
				{
					varDoor = !varDoor;
					if(type.hasDoor)
						player.sendSystemMessage(Component.literal("Doors " + (varDoor ? "open" : "closed")));
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 15: // Wing
			{
				if(toggleTimer <= 0)
				{
					if(type.hasWing)
					{
						varWing = !varWing;
						player.sendSystemMessage(Component.literal("Switching mode"));
					}
					if(type.mode == EnumPlaneMode.VTOL)
					{
						if(mode == EnumPlaneMode.HELI)
							mode = EnumPlaneMode.PLANE;
						else mode = EnumPlaneMode.HELI;
						player.sendSystemMessage(Component.literal(
								mode == EnumPlaneMode.HELI ? "Entering hover mode" : "Entering plane mode"));
					}
					toggleTimer = 10;
					FlansMod.getPacketHandler().sendToServer(new PacketDriveableControl(this));
				}
				return true;
			}
			case 16: // Trim Button
			{
				axes.setAngles(axes.getYaw(), 0, 0);
				return true;
			}
			default:
			{
				return super.pressKey(key, player, isOnEvent);
			}
		}
	}
	
	@Override
	public void updateKeyHeldState(int key, boolean held)
	{
		super.updateKeyHeldState(key, held);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(!readyForUpdates)
		{
			return;
		}
		
		//Get plane type
		PlaneType type = getPlaneType();
		DriveableData data = getDriveableData();
		if(type == null)
		{
			FlansMod.log.warn("Plane type null. Not ticking plane");
			return;
		}
		
		//Work out if this is the client side and the player is driving
		boolean thePlayerIsDrivingThis =
				world.isClientSide() && getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof Player
						&& FlansMod.proxy.isThePlayer((Player)getSeat(0).getControllingPassenger());
		
		//Despawning
		ticksSinceUsed++;
		if(!world.isClientSide() && getSeat(0).getControllingPassenger() != null)
			ticksSinceUsed = 0;
		if(!world.isClientSide() && TeamsManager.planeLife > 0 && ticksSinceUsed > TeamsManager.planeLife * 20)
		{
			setDead();
		}
		
		//Shooting, inventories, etc.
		//Decrement bomb and gun timers
		if(bombDelay > 0)
			bombDelay--;
		if(gunDelay > 0)
			gunDelay--;
		if(toggleTimer > 0)
			toggleTimer--;
		
		//Aesthetics
		//Rotate the propellers
		if(hasEnoughFuel())
		{
			propAngle += (Math.pow(throttle, 0.4)) * 1.5;
		}
		
		//Return the flaps to their resting position
		flapsYaw *= 0.9F;
		flapsPitchLeft *= 0.9F;
		flapsPitchRight *= 0.9F;
		
		//Limit flap angles
		if(flapsYaw > 20)
			flapsYaw = 20;
		if(flapsYaw < -20)
			flapsYaw = -20;
		if(flapsPitchRight > 20)
			flapsPitchRight = 20;
		if(flapsPitchRight < -20)
			flapsPitchRight = -20;
		if(flapsPitchLeft > 20)
			flapsPitchLeft = 20;
		if(flapsPitchLeft < -20)
			flapsPitchLeft = -20;
		
		//Player is not driving this. Update its position from server update packets 
		if(world.isClientSide() && !thePlayerIsDrivingThis)
		{
			//The driveable is currently moving towards its server position. Continue doing so.
			if(serverPositionTransitionTicker > 0)
			{
				moveTowardServerPosition();
			}
			//If the driveable is at its server position and does not have the next update, it should just simulate itself as a server side plane would, so continue
		}
		
		//Movement
		//The client only simulates plane physics for the local player's plane.
		//Every other plane follows the server through the entity tracker;
		//without this the client and server simulations drift apart (slightly
		//different settle trajectories amplify through the wheel springs) and
		//the plane the player SEES flies around on its own. Driven planes keep
		//client-side prediction.
		if(!world.isClientSide() || thePlayerIsDrivingThis)
		{
		
		//Throttle handling
		//Without a player, default to 0
		//With a player default to 0.5 for helicopters (hover speed)
		//And default to the range 0.25 ~ 0.5 for planes (taxi speed ~ take off speed)
		float throttlePull = 0.99F;
		if(getSeat(0) != null && getSeat(0).getControllingPassenger() != null && mode == EnumPlaneMode.HELI &&
				canThrust())
			throttle = (throttle - 0.5F) * throttlePull + 0.5F;
		
		//Get the speed of the plane
		float lastTickSpeed = (float)getSpeedXYZ();
		
		//Alter angles
		//Count the number of working propellers
		int heliPropsWorking = 0;
		int heliProps = 0;
		for(Propeller prop : type.heliPropellers)
			if(isPartIntact(prop.planePart))
				heliPropsWorking++;
		heliProps = type.heliPropellers.size();
		
		int propsWorking = 0;
		int props = 0;
		for(Propeller prop : type.propellers)
			if(isPartIntact(prop.planePart))
				propsWorking++;
		props = type.propellers.size();
		
		double motionX = getDeltaMovement().x;
		double motionY = getDeltaMovement().y;
		double motionZ = getDeltaMovement().z;
		
		PlaneForces.Result forces = PlaneForces.apply(
				throttle, flapsYaw, flapsPitchLeft, flapsPitchRight,
				mode, lastTickSpeed, getSpeedXZ(),
				motionX, motionY, motionZ,
				isPartIntact(EnumDriveablePart.tail),
				isPartIntact(EnumDriveablePart.leftWing),
				isPartIntact(EnumDriveablePart.rightWing),
				isPartIntact(EnumDriveablePart.blades),
				canThrust(),
				heliPropsWorking, heliProps, propsWorking, props,
				type,
				data.engine == null ? 0 : data.engine.engineSpeed,
				data.engine == null ? 0F : data.engine.fuelConsumption,
				axes);
		
		if(world.isClientSide() && !FlansMod.proxy.mouseControlEnabled())
		{
			//axes.rotateGlobalRoll(-axes.getRoll() * 0.1F);
		}
		
		motionX = forces.motionX;
		motionY = forces.motionY;
		motionZ = forces.motionZ;
		
		data.fuelInTank -= forces.fuelDrain;
		
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null && world != null)
			{
				wheel.xo = wheel.getX();
				wheel.yo = wheel.getY();
				wheel.zo = wheel.getZ();
			}
		}
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null && world != null)
				if(type.floatOnWater && world.containsAnyLiquid(wheel.getBoundingBox()))
				{
					motionY += type.buoyancy;
				}
		}
		
		//Move the wheels first
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null)
			{
				wheel.yo = wheel.getY();
				wheel.move(MoverType.SELF, new Vec3(motionX, motionY, motionZ));
			}
		}
		
		//Wheels are passengers, so their move() has no block collision and they
		//would sink through the ground to their spring targets. Clamp each
		//wheel to the terrain height so the ground can actually support the
		//plane (the reaction term below reads this clamping and lifts the
		//plane). 0.125 is tuned so the plane rests at the height the pack
		//model expects (the model wheels are drawn slightly lower than the
		//entity wheel axle points).
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null)
			{
				int groundTop = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
						(int)Math.floor(wheel.getX()), (int)Math.floor(wheel.getZ()));
				double minWheelY = groundTop + 0.125D;
				if(wheel.getY() < minWheelY)
					wheel.setPos(wheel.getX(), minWheelY, wheel.getZ());
			}
		}
		
		//A wheel that moved less than the commanded motion was blocked by the
		//ground: the plane is resting on its wheels
		boolean planeSupported = false;
		for(EntityWheel wheel : wheels)
		{
			if(wheel != null && ((wheel.getY() - wheel.yo) - (motionY)) > 0.001F)
			{
				planeSupported = true;
				break;
			}
		}
		//A parked, wheel-supported plane must stay put: the tail-dragger
		//geometry leaves a permanent dAngle between the tail wheel and its
		//target, and the unguarded angular spring kept re-orienting the plane,
		//pushing it sideways across the ground. Both the angular correction and
		//horizontal spring pushes are therefore suppressed while resting with
		//the throttle at zero.
		boolean parkedOnWheels = planeSupported && throttle == 0F;
		
		//Update wheels
		//The plane's actual displacement for this tick, measured so the delta
		//movement can be written back after the spring moves below
		double planeStartX = getX();
		double planeStartY = getY();
		double planeStartZ = getZ();
		for(int i = 0; i < 2; i++)
		{
			Vector3f amountToMoveCar = new Vector3f((float)motionX / 2F, (float)motionY / 2F, (float)motionZ / 2F);
			Vector3f springCorrection = new Vector3f();
			
			for(EntityWheel wheel : wheels)
			{
				if(wheel == null)
					continue;
				
				//Hacky way of forcing the car to step up blocks
				setOnGround(true);
				wheel.setOnGround(true);
				
				//Update angles
				wheel.setYRot(axes.getYaw());
				
				//Pull wheels towards car
				Vector3f targetWheelPos = axes.findLocalVectorGlobally(
						getPlaneType().wheelPositions[wheel.getExpectedWheelID()].position);
				Vector3f currentWheelPos = new Vector3f((float)(wheel.getX() - getX()), (float)(wheel.getY() - getY()), (float)(wheel.getZ() - getZ()));
				
				float targetWheelLength = targetWheelPos.length();
				float currentWheelLength = currentWheelPos.length();
				
				if(currentWheelLength > targetWheelLength * 3.0d)
				{
					// Make wheels break?
					//this.attackPart(EnumDriveablePart.backLeftWheel, source, damage);
				}
				
				float dLength = targetWheelLength - currentWheelLength;
				float dAngle = (targetWheelPos.lengthSquared() < 1E-6F || currentWheelPos.lengthSquared() < 1E-6F)
						? 0F : Vector3f.angle(targetWheelPos, currentWheelPos);
				
				{
					//Now Lerp by wheelSpringStrength and work out the new positions		
					float newLength = currentWheelLength + dLength * type.wheelSpringStrength;
					Vector3f rotateAround = Vector3f.cross(targetWheelPos, currentWheelPos, null);
					
					Vector3f newWheelPos;
					if(rotateAround.lengthSquared() > 1E-6F && newLength > 1E-6F)
					{
						rotateAround.normalise();
						
						Matrix4f mat = new Matrix4f();
						mat.m00 = currentWheelPos.x;
						mat.m10 = currentWheelPos.y;
						mat.m20 = currentWheelPos.z;
						mat.rotate(dAngle * type.wheelSpringStrength, rotateAround);
						
						if(!parkedOnWheels)
							axes.rotateGlobal(-dAngle * type.wheelSpringStrength, rotateAround);
						
						newWheelPos = new Vector3f(mat.m00, mat.m10, mat.m20);
						newWheelPos.normalise().scale(newLength);
					}
					else
					{
						//Wheel is in line with its target; adjust the length only
						newWheelPos = new Vector3f(targetWheelPos);
						newWheelPos.normalise().scale(newLength);
					}
					
					//The proportion of the spring adjustment that is applied to the wheel. 1 - this is applied to the plane
					float wheelProportion = 0.75F;
					
					//wheel.getDeltaMovement().x = (newWheelPos.x - currentWheelPos.x) * wheelProportion;
					//wheel.getDeltaMovement().y = (newWheelPos.y - currentWheelPos.y) * wheelProportion;
					//wheel.getDeltaMovement().z = (newWheelPos.z - currentWheelPos.z) * wheelProportion;
					
					Vector3f amountToMoveWheel = new Vector3f();
					
					amountToMoveWheel.x = (newWheelPos.x - currentWheelPos.x) * (1F - wheelProportion);
					amountToMoveWheel.y = (newWheelPos.y - currentWheelPos.y) * (1F - wheelProportion);
					amountToMoveWheel.z = (newWheelPos.z - currentWheelPos.z) * (1F - wheelProportion);
					
					springCorrection.x -= (newWheelPos.x - currentWheelPos.x) * (1F - wheelProportion);
					springCorrection.y -= (newWheelPos.y - currentWheelPos.y) * (1F - wheelProportion);
					springCorrection.z -= (newWheelPos.z - currentWheelPos.z) * (1F - wheelProportion);
					
					//The difference between how much the wheel moved and how much it was meant to move. i.e. the reaction force from the block
					//amountToMoveCar.x += ((wheel.getX() - wheel.xo) - (motionX)) * 0.616F / wheels.length;
					amountToMoveCar.y += ((wheel.getY() - wheel.yo) - (motionY)) * 0.5F / wheels.length;
					//amountToMoveCar.z += ((wheel.getZ() - wheel.zo) - (motionZ)) * 0.0616F / wheels.length;
					
					if(amountToMoveWheel.lengthSquared() >= 32f * 32f)
					{
						FlansMod.log.warn("Wheel tried to move " + amountToMoveWheel.length() + " in a single frame, capping at 32 blocks");
						amountToMoveWheel.normalise();
						amountToMoveWheel.scale(32f);
					}
					
					wheel.move(MoverType.SELF, new Vec3(amountToMoveWheel.x, amountToMoveWheel.y, amountToMoveWheel.z));
				}
			}
			
			//A parked plane must not move at all: zero the horizontal spring
			//pushes and apply ground friction to its own horizontal motion on
			//the ground. With the throttle cut in the air (right after
			//placement), damp the spring pushes hard so the dangling gear does
			//not drag the plane sideways; the plane's own motion is untouched
			//there, so gliding is unaffected.
			if(parkedOnWheels)
			{
				springCorrection.x = 0F;
				springCorrection.z = 0F;
				amountToMoveCar.x *= 0.0625F;
				amountToMoveCar.z *= 0.0625F;
			}
			else if(throttle == 0F)
			{
				springCorrection.x *= 0.03125F;
				springCorrection.z *= 0.03125F;
			}
			amountToMoveCar.x += springCorrection.x;
			amountToMoveCar.y += springCorrection.y;
			amountToMoveCar.z += springCorrection.z;
			
			move(MoverType.SELF, new Vec3(amountToMoveCar.x, amountToMoveCar.y, amountToMoveCar.z));
			
		}
		
		//Write the plane's actual displacement for this tick back into the delta
		//movement. The wheel springs support (and, on a bounce, launch) the plane
		//through move() without touching the velocity, so writing the raw
		//gravity-fed motion back left a parked plane believing it was falling at
		//~1.9 m/tick. checkForCollisions then damaged parts by that phantom speed
		//and exploded the plane when they died - the "plane jumps when placed" bug.
		//While parked, the vertical velocity is also damped so placement/settle
		//transients (the wheel clamp kicking the plane up right after spawning)
		//cannot bounce the plane around; the steady-state support comes from the
		//reaction term, not from this velocity, so the resting stance is unchanged.
		double actualMotionX = getX() - planeStartX;
		double actualMotionY = getY() - planeStartY;
		double actualMotionZ = getZ() - planeStartZ;
		if(parkedOnWheels)
			actualMotionY *= 0.25D;
		setDeltaMovement(actualMotionX, actualMotionY, actualMotionZ);
		
		checkForCollisions();
		
		}
		
		//Sounds
		//Starting sound
		if(throttle > 0.01F && throttle < 0.2F && soundPosition == 0 && hasEnoughFuel())
		{
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, 0, type.startSound, false);
			soundPosition = type.startSoundLength;
		}
		//Flying sound
		if(throttle > 0.2F && soundPosition == 0 && hasEnoughFuel())
		{
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, 0, type.engineSound, false);
			soundPosition = type.engineSoundLength;
		}
		
		//Sound decrementer
		if(soundPosition > 0)
			soundPosition--;
		
		for(EntitySeat seat : getSeats())
		{
			if(seat != null)
				seat.updatePosition();
		}
		
		//Calculate movement on the client and then send position, rotation etc to the server
		if(serverPosX != getX() || serverPosY != getY() || serverPosZ != getZ() || serverYaw != axes.getYaw())
		{
			if(thePlayerIsDrivingThis)
			{
				FlansMod.getPacketHandler().sendToServer(new PacketPlaneControl(this));
				serverPosX = getX();
				serverPosY = getY();
				serverPosZ = getZ();
				serverYaw = axes.getYaw();
			}
		}
		
		PostUpdate();
		
		logPhysicsTick("Plane", throttle, onGround());
	}
	
	public boolean canThrust()
	{
		return (getSeat(0) != null && getSeat(0).getControllingPassenger() instanceof Player
				&& ((Player)getSeat(0).getControllingPassenger()).getAbilities().instabuild) ||
				driveableData.fuelInTank > 0;
	}
	
	@Override
	public boolean gearDown()
	{
		return varGear;
	}
	
	private boolean hasWorkingProp()
	{
		PlaneType type = getPlaneType();
		if(type.mode == EnumPlaneMode.HELI || type.mode == EnumPlaneMode.VTOL)
			for(Propeller prop : type.heliPropellers)
				if(isPartIntact(prop.planePart))
					return true;
		if(type.mode == EnumPlaneMode.PLANE || type.mode == EnumPlaneMode.VTOL)
			for(Propeller prop : type.propellers)
				if(isPartIntact(prop.planePart))
					return true;
		return false;
	}
	
	public boolean attackEntityFrom(DamageSource damagesource, float i, boolean doDamage)
	{
		if(world.isClientSide() || isRemoved())
			return true;
		
		PlaneType type = PlaneType.getPlane(driveableType);
		
		if(damagesource.getMsgId().equals("player") && damagesource.getEntity() != null && damagesource.getEntity().onGround()
				&& (getSeat(0) == null || getSeat(0).getControllingPassenger() == null))
		{
			ItemStack planeStack = new ItemStack(type.item);
			CompoundTag tags = new CompoundTag();
			driveableData.writeToNBT(tags);
			planeStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tags));
			spawnAtLocation((ServerLevel)world, planeStack, 0.5F);
			setDead();
		}
		return true;
	}
	
	@Override
	public boolean canHitPart(EnumDriveablePart part)
	{
		return varGear || (part != EnumDriveablePart.coreWheel && part != EnumDriveablePart.leftWingWheel &&
				part != EnumDriveablePart.rightWingWheel && part != EnumDriveablePart.tailWheel);
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float i)
	{
		return attackEntityFrom(damagesource, i, true);
	}
	
	public PlaneType getPlaneType()
	{
		return PlaneType.getPlane(driveableType);
	}
	
	@Override
	protected void dropItemsOnPartDeath(Vector3f midpoint, DriveablePart part)
	{
	}
	
	@Override
	public String getBombInventoryName()
	{
		return "Bombs";
	}
	
	@Override
	public String getMissileInventoryName()
	{
		return "Missiles";
	}
	
	@Override
	public boolean hasMouseControlMode()
	{
		return true;
	}
}
