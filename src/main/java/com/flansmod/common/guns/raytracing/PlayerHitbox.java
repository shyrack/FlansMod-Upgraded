package com.flansmod.common.guns.raytracing;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.common.FlansMod;
import com.flansmod.common.RotatedAxes;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.FiredShot;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.raytracing.FlansModRaytracer.PlayerBulletHit;
import com.flansmod.common.teams.TeamsManager;
import com.flansmod.common.vector.Vector3f;

public class PlayerHitbox
{
	/** */
	public Player player;
	/**
	 * The angles of this box
	 */
	public RotatedAxes axes;
	/**
	 * The origin of rotation for this box
	 */
	public Vector3f rP;
	/**
	 * The lower left corner of this box
	 */
	public Vector3f o;
	/**
	 * The dimensions of this box
	 */
	public Vector3f d;
	/**
	 * The type of hitbox
	 */
	public EnumHitboxType type;
	
	public PlayerHitbox(Player player, RotatedAxes axes, Vector3f rotationPoint, Vector3f origin, Vector3f dimensions, EnumHitboxType type)
	{
		this.player = player;
		this.axes = axes;
		this.o = origin;
		this.d = dimensions;
		this.type = type;
		this.rP = rotationPoint;
	}
	
	public void renderHitbox(Level world, Vector3f pos)
	{
		
		//Vector3f boxOrigin = new Vector3f(pos.x + rP.x, pos.y + rP.y, pos.z + rP.z);
		//world.addFreshEntity(new EntityDebugAABB(world, boxOrigin, d, 2, 1F, 1F, 0F, axes.getYaw(), axes.getPitch(), axes.getRoll(), o));
		if(type != EnumHitboxType.RIGHTARM)
			return;
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
				for(int k = 0; k < 3; k++)
				{
					Vector3f point = new Vector3f(o.x + d.x * i / 2, o.y + d.y * j / 2, o.z + d.z * k / 2);
					point = axes.findLocalVectorGlobally(point);
					if(FlansMod.DEBUG && world.isClientSide())
						((net.minecraft.client.multiplayer.ClientLevel)world).addEntity(new EntityDebugDot(world, new Vector3f(pos.x + rP.x + point.x, pos.y + rP.y + point.y, pos.z + rP.z + point.z), 1, 0F, 1F, 0F));
				}
		
	}

	public PlayerBulletHit raytrace(Vector3f origin, Vector3f motion)
	{
		//Move to local coords for this hitbox, but don't modify the original "origin" vector
		origin = Vector3f.sub(origin, rP, null);
		origin = axes.findGlobalVectorLocally(origin);
		motion = axes.findGlobalVectorLocally(motion);
		
		//We now have an AABB starting at o and with dimensions d and our ray in the same coordinate system
		//We are looking for a point at which the ray enters the box, so we need only consider faces that the ray can see. Partition the space into 3 areas in each axis
		
		//X - axis and faces x = o.x and x = o.x + d.x
		if(motion.x != 0F)
		{
			if(origin.x < o.x) //Check face x = o.x
			{
				float intersectTime = (o.x - origin.x) / motion.x;
				float intersectY = origin.y + motion.y * intersectTime;
				float intersectZ = origin.z + motion.z * intersectTime;
				if(intersectY >= o.y && intersectY <= o.y + d.y && intersectZ >= o.z && intersectZ <= o.z + d.z)
					return new PlayerBulletHit(this, intersectTime);
			}
			else if(origin.x > o.x + d.x) //Check face x = o.x + d.x
			{
				float intersectTime = (o.x + d.x - origin.x) / motion.x;
				float intersectY = origin.y + motion.y * intersectTime;
				float intersectZ = origin.z + motion.z * intersectTime;
				if(intersectY >= o.y && intersectY <= o.y + d.y && intersectZ >= o.z && intersectZ <= o.z + d.z)
					return new PlayerBulletHit(this, intersectTime);
			}
		}
		
		//Z - axis and faces z = o.z and z = o.z + d.z
		if(motion.z != 0F)
		{
			if(origin.z < o.z) //Check face z = o.z
			{
				float intersectTime = (o.z - origin.z) / motion.z;
				float intersectX = origin.x + motion.x * intersectTime;
				float intersectY = origin.y + motion.y * intersectTime;
				if(intersectX >= o.x && intersectX <= o.x + d.x && intersectY >= o.y && intersectY <= o.y + d.y)
					return new PlayerBulletHit(this, intersectTime);
			}
			else if(origin.z > o.z + d.z) //Check face z = o.z + d.z
			{
				float intersectTime = (o.z + d.z - origin.z) / motion.z;
				float intersectX = origin.x + motion.x * intersectTime;
				float intersectY = origin.y + motion.y * intersectTime;
				if(intersectX >= o.x && intersectX <= o.x + d.x && intersectY >= o.y && intersectY <= o.y + d.y)
					return new PlayerBulletHit(this, intersectTime);
			}
		}
		
		//Y - axis and faces y = o.y and y = o.y + d.y
		if(motion.y != 0F)
		{
			if(origin.y < o.y) //Check face y = o.y
			{
				float intersectTime = (o.y - origin.y) / motion.y;
				float intersectX = origin.x + motion.x * intersectTime;
				float intersectZ = origin.z + motion.z * intersectTime;
				if(intersectX >= o.x && intersectX <= o.x + d.x && intersectZ >= o.z && intersectZ <= o.z + d.z)
					return new PlayerBulletHit(this, intersectTime);
			}
			else if(origin.y > o.y + d.y) //Check face x = o.x + d.x
			{
				float intersectTime = (o.y + d.y - origin.y) / motion.y;
				float intersectX = origin.x + motion.x * intersectTime;
				float intersectZ = origin.z + motion.z * intersectTime;
				if(intersectX >= o.x && intersectX <= o.x + d.x && intersectZ >= o.z && intersectZ <= o.z + d.z)
					return new PlayerBulletHit(this, intersectTime);
			}
		}

		return null;
	}

	public float hitByBullet(FiredShot shot, Float damage, Float penetratingPower)
	{
		BulletType bulletType = shot.getBulletType();
		if(bulletType.setEntitiesOnFire)
			player.igniteForTicks(20);
		for(MobEffectInstance effect : bulletType.hitEffects)
		{
			player.addEffect(effect);
		}
		float damageModifier = bulletType.penetratingPower < 0.1F ? penetratingPower / bulletType.penetratingPower : 1;
		
		switch(type)
		{
			case BODY: break;
			case HEAD: damageModifier *= 1.6F;
				break;
			case LEFTARM: damageModifier *= 0.6F;
				break;
			case RIGHTARM: damageModifier *= 0.6F;
				break;
			case LEFTITEM: break;
			case RIGHTITEM: break;
			default: break;
		}
		switch(type)
		{
			case BODY: case HEAD: case LEFTARM: case RIGHTARM:
		{
			//Calculate the hit damage
			float hitDamage = damage * shot.getBulletType().damageVsLiving * damageModifier;
			//Create a damage source object
			DamageSource damagesource = shot.getDamageSource(type.equals(EnumHitboxType.HEAD));
			
			//When the damage is 0 (such as with Nerf guns) the entityHurt Forge hook is not called, so this hacky thing is here
			if(!player.level().isClientSide() && hitDamage == 0 && TeamsManager.getInstance().currentRound != null)
				TeamsManager.getInstance().currentRound.gametype.playerAttacked((ServerPlayer)player, damagesource);
			
			//Attack the entity!
			player.hurtServer((ServerLevel)player.level(), damagesource, hitDamage);
			return penetratingPower - 1;
		}
			case RIGHTITEM:
			{
				ItemStack currentStack = player.getMainHandItem();
				if(currentStack != null && currentStack.getItem() instanceof ItemGun)
				{
					GunType gunType = ((ItemGun)currentStack.getItem()).GetType();
					//TODO : Shield damage
					return penetratingPower - gunType.shieldDamageAbsorption;
				}
				else return penetratingPower;
			}
			case LEFTITEM:
			{
				ItemStack currentStack = player.getOffhandItem();
				if(currentStack != null && currentStack.getItem() instanceof ItemGun)
				{
					GunType gunType = ((ItemGun)currentStack.getItem()).GetType();
					//TODO : Shield damage
					return penetratingPower - gunType.shieldDamageAbsorption;
				}
				else return penetratingPower;
			}
			default: return penetratingPower;
		}
	}
}
