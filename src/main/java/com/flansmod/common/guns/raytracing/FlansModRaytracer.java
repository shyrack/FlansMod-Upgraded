package com.flansmod.common.guns.raytracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.EntityAAGun;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShotHandler;
import com.flansmod.common.teams.Team;
import com.flansmod.common.vector.Vector3f;

public class FlansModRaytracer
{
	
	public static List<BulletHit> Raytrace(Level world, Entity playerToIgnore, boolean canHitSelf, Entity entityToIgnore, Vector3f origin, Vector3f motion, int pingOfShooter, Float gunPenetration)
	{
		//Create a list for all bullet hits
		List<BulletHit> hits = new ArrayList<>();
		
		float speed = motion.length();
		
		//Iterate over all entities
		for(Entity obj : world.getEntities((Entity)null, new net.minecraft.world.phys.AABB(origin.x - 1000, origin.y - 1000, origin.z - 1000, origin.x + 1000, origin.y + 1000, origin.z + 1000), entity -> true))
		{
			boolean shouldDoNormalHitDetect = true;
			//Get driveables
			if(obj instanceof EntityDriveable)
			{
				EntityDriveable driveable = (EntityDriveable)obj;
				shouldDoNormalHitDetect = false;
				
				if(driveable.isDead() || driveable.isPartOfThis(playerToIgnore))
					continue;
				
				//If this bullet is within the driveable's detection range
				if(driveable.distanceToSqr(origin.x, origin.y, origin.z) <= (driveable.getDriveableType().bulletDetectionRadius + speed) * (driveable.getDriveableType().bulletDetectionRadius + speed))
				{
					//Raytrace the bullet
					ArrayList<BulletHit> driveableHits = driveable.attackFromBullet(origin, motion);
					hits.addAll(driveableHits);
				}
			}
			//Get players
			else if(obj instanceof Player)
			{
				Player player = (Player)obj;
				PlayerData data = PlayerHandler.getPlayerData(player);
				shouldDoNormalHitDetect = false;
				if(data != null)
				{
					if(player.isRemoved() || data.team == Team.spectators)
					{
						continue;
					}
					if(player == playerToIgnore && !canHitSelf)
						continue;
					int snapshotToTry = pingOfShooter / 50;
					if(snapshotToTry >= data.snapshots.length)
						snapshotToTry = data.snapshots.length - 1;
					
					PlayerSnapshot snapshot = data.snapshots[snapshotToTry];
					if(snapshot == null)
						snapshot = data.snapshots[0];
					
					//DEBUG
					//snapshot = new PlayerSnapshot(player);
					
					//Check one last time for a null snapshot. If this is the case, fall back to normal hit detection
					if(snapshot == null)
						shouldDoNormalHitDetect = true;
					else
					{
						//Raytrace
						ArrayList<BulletHit> playerHits = snapshot.raytrace(origin, motion);
						hits.addAll(playerHits);
					}
				}
			}
			
			if(shouldDoNormalHitDetect)
			{
				Entity entity = obj;
				if(entity != entityToIgnore && entity != playerToIgnore
						&& !entity.isRemoved()
						&& (entity instanceof LivingEntity || entity instanceof EntityAAGun || entity instanceof EntityGrenade))
				{
					java.util.Optional<Vec3> intercept = entity.getBoundingBox().clip(origin.toVec3(), new Vec3(origin.x + motion.x, origin.y + motion.y, origin.z + motion.z));
					if(intercept.isPresent())
					{
						Vec3 mop = intercept.get();
						boolean hit = true;
						
						if(hit)
						{
							Vec3 hitPoint = new Vec3(mop.x - origin.x, mop.y - origin.y, mop.z - origin.z);
							if (FlansMod.DEBUG)
								((net.minecraft.server.level.ServerLevel)world).addFreshEntity(new EntityDebugDot(world, new Vector3f(mop), 1000, 1.0f, 0f, 0f));
							
							float hitLambda = 1F;
							if(motion.x != 0F)
								hitLambda = (float) (hitPoint.x / motion.x);
							else if(motion.y != 0F)
								hitLambda = (float) (hitPoint.y / motion.y);
							else if(motion.z != 0F)
								hitLambda = (float) (hitPoint.z / motion.z);
							if(hitLambda < 0)
								hitLambda = -hitLambda;
							
							hits.add(new EntityHit(entity, hitLambda));
							//raytraceBlock(world, mop, motion, hits);
						}
					}
				}
			}
		}
		
		Vec3 mot = new Vector3f(motion).toVec3();
		mot = mot.normalize();
		mot = mot.scale(0.5d);
		hits = raytraceBlock(world, origin.toVec3(), new Vec3(0, 0, 0), motion, mot, hits, gunPenetration, null);
		
		//We hit something
		if(!hits.isEmpty())
		{
			//Sort the hits according to the intercept position
			Collections.sort(hits);
		}
		
		
		return hits;
	}
	
	private static List<BulletHit> raytraceBlock(
			Level world,
			Vec3 posVec,
			Vec3 previousHit,
			Vector3f motion,
			Vec3 normalized_motion,
			List<BulletHit> hits,
			Float penetration,
			BlockPos oldPos)
	{
		//Ray trace the bullet by comparing its next position to its current position
		Vec3 nextPosVec = new Vec3(posVec.x + motion.x, posVec.y + motion.y, posVec.z + motion.z);

		HitResult hit = world.clip(new net.minecraft.world.level.ClipContext(posVec, nextPosVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
		
		if(hit != null && hit.getType() == HitResult.Type.BLOCK)
		{
			BlockHitResult blockHit = (BlockHitResult)hit;
			Vec3 hitVec = blockHit.getLocation().subtract(posVec);
			hitVec = hitVec.add(previousHit);
			
			BlockPos pos = blockHit.getBlockPos();
			BlockState blockState = world.getBlockState(blockHit.getBlockPos());
			
			if (!pos.equals(oldPos))
			{
				//Calculate the lambda value of the intercept
				float lambda = 1;
				//Try each co-ordinate one at a time.
				if(motion.x != 0)
					lambda = (float)(hitVec.x / motion.x);
				else if(motion.y != 0)
					lambda = (float)(hitVec.y / motion.y);
				else if(motion.z != 0)
					lambda = (float)(hitVec.z / motion.z);
			
				if(lambda < 0)
					lambda = -lambda;

				hits.add(new BlockHit(blockHit, lambda, blockState));
				penetration -= ShotHandler.getBlockPenetrationDecrease(blockState, pos, world);
			}
			
			if (penetration > 0)
			{
				hits = raytraceBlock(
						world,
						blockHit.getLocation().add(normalized_motion),
						hitVec.add(normalized_motion),
						motion,
						normalized_motion,
						hits,
						penetration,
						pos);
			}
		}
		return hits;
	}
	
	public static Vector3f GetPlayerMuzzlePosition(Player player, InteractionHand hand)
	{
		PlayerSnapshot snapshot = new PlayerSnapshot(player);
		
		ItemStack itemstack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();
		
		if(itemstack.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)itemstack.getItem()).GetType();
			AttachmentType barrelType = gunType.getBarrel(itemstack);
			
			return Vector3f.add(new Vector3f(player.getX(), player.getY(), player.getZ()), snapshot.GetMuzzleLocation(gunType, barrelType, hand), null);
		}
		
		return new Vector3f(player.getEyePosition(0.0f));
	}
	
	public static abstract class BulletHit implements Comparable<BulletHit>
	{
		/**
		 * The time along the ray that the intersection happened. Between 0 and 1
		 */
		public float intersectTime;
		
		public BulletHit(float f)
		{
			intersectTime = f;
		}
		
		@Override
		public int compareTo(BulletHit other)
		{
			if(intersectTime < other.intersectTime)
				return -1;
			else if(intersectTime > other.intersectTime)
				return 1;
			return 0;
		}
		
		public abstract Entity GetEntity();
	}
	
	public static class BlockHit extends BulletHit
	{
		private HitResult raytraceResult;
		private BlockState blockstate;
		
		public BlockHit(HitResult mop, Float f, BlockState blockstate)
		{
			super(f);
			raytraceResult = mop;
			this.blockstate = blockstate;
		}
		
		@Override
		public Entity GetEntity()
		{
			return null;
		}
		
		public BlockState getIBlockState()
		{
			return blockstate;
		}
		
		public HitResult getRayTraceResult()
		{
			return raytraceResult;
		}
	}
	
	public static class EntityHit extends BulletHit
	{
		public Entity entity;
		
		public EntityHit(Entity e, float f)
		{
			super(f);
			entity = e;
		}
		
		@Override
		public Entity GetEntity()
		{
			return entity;
		}
	}
	
	public static class DriveableHit extends BulletHit
	{
		public EntityDriveable driveable;
		public EnumDriveablePart part;
		
		public DriveableHit(EntityDriveable d, EnumDriveablePart p, float f)
		{
			super(f);
			part = p;
			driveable = d;
		}
		
		@Override
		public Entity GetEntity()
		{
			return driveable;
		}
	}
	
	/**
	 * Raytracing will return a load of these objects containing hit data. These will then be compared against each other and against any block hits
	 * The hit that occurs first along the path of the bullet is the one that is acted upon. Unless the bullet has penetration of course
	 */
	public static class PlayerBulletHit extends BulletHit
	{
		/**
		 * The hitbox hit
		 */
		public PlayerHitbox hitbox;
		
		public PlayerBulletHit(PlayerHitbox box, float f)
		{
			super(f);
			hitbox = box;
		}
		
		@Override
		public Entity GetEntity()
		{
			return hitbox.player;
		}
	}
	
}
