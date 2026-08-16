package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.vector.Vector3f;

public class EntityAIMecha extends EntityMecha
{
	private Entity target;
	private float targetingRange = 20F;
	private int targetAcquireInterval = 40;
	
	private boolean usingLeft = false;
	
	public EntityAIMecha(Level world)
	{
		super(world);
		this.world = level();
	}
	
	public EntityAIMecha(Level world, double x, double y, double z, MechaType type, DriveableData data, CompoundTag tags)
	{
		super(world, x, y, z, type, data, tags);
		this.world = level();
	}
	
	@Override
	public void tick()
	{
		throttle = 1F;
		
		super.tick();
	}
	
	@Override
	public InteractionResult interact(Player entityplayer, InteractionHand hand, Vec3 pos)
	{
		return InteractionResult.PASS;
	}
	
	@Override
	protected void moveAI(Vector3f actualMotion)
	{
		MechaType type = getMechaType();
		DriveableData data = getDriveableData();
		
		//Acquire target
		if(target == null && (this.tickCount + this.getId()) % targetAcquireInterval == 0)
		{
			double distToCurrentTarget = 999D;
			for(Object obj : world.getEntities(this, getBoundingBox().inflate(targetingRange, targetingRange, targetingRange), entity -> true))
			{
				double distToPotentialTarget = this.distanceToSqr((Entity)obj);
				if(isBetterTarget(target, distToCurrentTarget, (Entity)obj, distToPotentialTarget))
				{
					target = (Entity)obj;
					distToCurrentTarget = distToPotentialTarget;
				}
			}
		}

		//And if we have line of sight, shoot it
		if(!world.isClientSide() && target != null)
		{
			Vec3 rightArmOrigin = usingLeft ? axes.findLocalVectorGlobally(getMechaType().leftArmOrigin).toVec3().add(getX(), getY(), getZ()) : axes.findLocalVectorGlobally(getMechaType().rightArmOrigin).toVec3().add(getX(), getY(), getZ());
			Vec3 targetOrigin = new Vec3(target.getX(), target.getY() + target.getEyeHeight() / 2D, target.getZ());
			
			double dX = targetOrigin.x - rightArmOrigin.x;
			double dY = targetOrigin.y - rightArmOrigin.y;
			double dZ = targetOrigin.z - rightArmOrigin.z;

			axes.setAngles((float)Math.atan2(dZ, dX) * 180F / 3.14159F, 0F, 0F);
			if(getSeat(0) != null)
			{
				getSeat(0).looking.setAngles(0F, -(float)Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180F / 3.14159F, 0F);
				getSeat(0).prevLooking.setAngles(0F, -(float)Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180F / 3.14159F, 0F);
			}
			
			HitResult hit = world.clip(new ClipContext(rightArmOrigin, targetOrigin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			
			{
				double blockHitX = hit == null ? 0 : hit.getLocation().x - rightArmOrigin.x;
				double blockHitY = hit == null ? 0 : hit.getLocation().y - rightArmOrigin.y;
				double blockHitZ = hit == null ? 0 : hit.getLocation().z - rightArmOrigin.z;
				
				//If the target is nearer than the block hit or there was no block
				if(hit == null || hit.getType() != HitResult.Type.BLOCK || dX * dX + dY * dY + dZ * dZ < blockHitX * blockHitX + blockHitY * blockHitY + blockHitZ * blockHitZ)
				{
					useItem(usingLeft);
					if(random.nextInt(5) == 0)
						usingLeft = !usingLeft;
				}
				//Otherwise, move closer
				else
				{
					//If we have a target, move towards it and look at it
					moveX = (float)(target.getX() - getX());
					moveZ = (float)(target.getZ() - getZ());
					
					float mag = (float)Math.sqrt(moveX * moveX + moveZ * moveZ);
					
					Vector3f intent = new Vector3f(moveX, 0, moveZ);
					
					if(Math.abs(intent.lengthSquared()) > 0.1)
					{
						intent.normalise();
						
						++legSwing;

						//intent = axes.findLocalVectorGlobally(intent);

						Vector3f intentOnLegAxes = legAxes.findGlobalVectorLocally(intent);
						float intentAngle = (float)Math.atan2(intent.z, intent.x) * 180F / 3.14159265F;
						float angleBetween = intentAngle - legAxes.getYaw();
						if(angleBetween > 180F) angleBetween -= 360F;
						if(angleBetween < -180F) angleBetween += 360F;

						float signBetween = Math.signum(angleBetween);
						angleBetween = Math.abs(angleBetween);
						
						if(angleBetween > 0.1)
						{
							legAxes.rotateGlobalYaw(Math.min(angleBetween, type.rotateSpeed) * signBetween);
						}
						
						intent.scale((type.moveSpeed * data.engine.engineSpeed * speedMultiplier()) * (4.3F / 20F));
						
						if(isPartIntact(EnumDriveablePart.hips))
						{
							//Move!
							Vector3f.add(actualMotion, intent, actualMotion);
						}
					}
				}
			}
		}
		
		
	}
	
	@Override
	protected boolean creative()
	{
		return false;
	}
	
	private boolean isBetterTarget(Entity currentTarget, double distToCurrentTarget, Entity potentialTarget, double distToPotentialTarget)
	{
		if(potentialTarget instanceof Player && distToPotentialTarget < distToCurrentTarget && distToPotentialTarget < targetingRange * targetingRange)
			return true;
		return false;
	}
	
	@Override
	public boolean hasFuel()
	{
		return true;
	}

	@Override
	public boolean hasEnoughFuel()
	{
		return true;
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damagesource, float i)
	{
		if(world.isClientSide() || isRemoved())
			return true;

		MechaType type = getMechaType();

		if(damagesource.getMsgId().equals("player") && damagesource.getEntity() != null && damagesource.getEntity().onGround() && (getSeat(0) == null || getSeat(0).getControllingPassenger() == null))
		{
			return false;
		}
		else return super.hurtServer(level, damagesource, i);
	}
}
