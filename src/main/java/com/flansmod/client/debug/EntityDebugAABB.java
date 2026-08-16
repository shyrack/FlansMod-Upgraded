package com.flansmod.client.debug;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.flansmod.common.ModEntities;
import com.flansmod.common.vector.Vector3f;

public class EntityDebugAABB extends Entity
{
	protected Level world;

	public Vector3f vector;
	public int life;
	public float red = 1F, green = 1F, blue = 1F;
	public float rotationRoll;
	/**
	 * This is the offset after rotation
	 */
	public Vector3f offset;
	
	public EntityDebugAABB(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}
	
	public EntityDebugAABB(Level w, Vector3f u, Vector3f v, int i, float r, float g, float b, float yaw, float pitch, float roll, Vector3f offset)
	{
		super(ModEntities.DEBUG_AABB, w);
		this.world = level();
		setPos(u.x, u.y, u.z);
		setYRot(yaw);
		setXRot(pitch);
		rotationRoll = roll;
		vector = v;
		life = i;
		red = r;
		green = g;
		blue = b;
		this.offset = offset;
	}
	
	public EntityDebugAABB(Level w, Vector3f u, Vector3f v, int i, float r, float g, float b, float yaw, float pitch, float roll)
	{
		this(w, u, v, i, r, g, b, yaw, pitch, roll, new Vector3f());
	}
	
	public EntityDebugAABB(Level w, Vector3f u, Vector3f v, int i, float r, float g, float b)
	{
		this(w, u, v, i, r, g, b, 0F, 0F, 0F);
	}
	
	public EntityDebugAABB(Level w, Vector3f u, Vector3f v, int i)
	{
		this(w, u, v, i, 1F, 1F, 1F);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		life--;
		if(life <= 0)
			discard();
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		
	}
	
	@Override
	public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount)
	{
		return false;
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput nbttagcompound)
	{
		
	}
	
	@Override
	protected void addAdditionalSaveData(ValueOutput nbttagcompound)
	{
	
	}
	
}
