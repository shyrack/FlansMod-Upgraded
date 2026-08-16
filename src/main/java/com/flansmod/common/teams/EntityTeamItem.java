package com.flansmod.common.teams;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.EntityItemCustomRender;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerHandler;

public class EntityTeamItem extends EntityItemCustomRender
{
	private static final EntityDataAccessor<Integer> X_COORD = SynchedEntityData.defineId(EntityTeamItem.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> Y_COORD = SynchedEntityData.defineId(EntityTeamItem.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> Z_COORD = SynchedEntityData.defineId(EntityTeamItem.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(EntityTeamItem.class, EntityDataSerializers.FLOAT);
	
	public TileEntitySpawner spawner;
	public double angle;
	public int xCoord, yCoord, zCoord;
	private int age;
	
	public EntityTeamItem(TileEntitySpawner te, int i)
	{
		super(te.getWorld(), te.getBlockPos().getX() + 0.5F, te.getBlockPos().getY() + 0.5F, te.getBlockPos().getZ() + 0.5F, te.stacksToSpawn.get(i).copy());
		this.world = level();
		te.itemEntities.add(this);
		angle = i * Math.PI * 2 / te.stacksToSpawn.size();
		setDeltaMovement(0D, 0D, 0D);
		xCoord = te.getBlockPos().getX();
		yCoord = te.getBlockPos().getY();
		zCoord = te.getBlockPos().getZ();
		entityData.set(X_COORD, xCoord);
		entityData.set(Y_COORD, yCoord);
		entityData.set(Z_COORD, zCoord);
		entityData.set(ANGLE, (float)angle);
		spawner = te;
	}
	
		public EntityTeamItem(EntityType<? extends net.minecraft.world.entity.item.ItemEntity> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityTeamItem(Level world)
	{
		this(ModEntities.TEAMS_ITEM, world);
		this.world = level();
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(X_COORD, 0);
		builder.define(Y_COORD, 0);
		builder.define(Z_COORD, 0);
		builder.define(ANGLE, 0F);
	}
	
	@Override
	public void tick()
	{
		++age;
		xo = getX();
		yo = getY();
		zo = getZ();
		yRotO = getYRot();
		if(world.isClientSide())
		{
			xCoord = entityData.get(X_COORD);
			yCoord = entityData.get(Y_COORD);
			zCoord = entityData.get(Z_COORD);
			angle = entityData.get(ANGLE);
			angle += 0.05D;
			entityData.set(ANGLE, (float)angle);
			setPos(xCoord + 0.5F + Math.cos(angle) * 0.3F, yCoord + 0.5F, zCoord + 0.5F + Math.sin(angle) * 0.3F);
		}
		
		//Temporary fire glitch fix
		if(world.isClientSide())
			extinguishFire();
	}
	
	@Override
	public void playerTouch(Player player)
	{
		if(!world.isClientSide())
		{
			int spawnerTeamID = spawner.getTeamID();
			Team spawnerTeam = TeamsManager.getInstance().getTeam(spawnerTeamID);
			Team playerTeam = PlayerHandler.getPlayerData(player).team;
			if(spawnerTeam != null)
			{
				if(playerTeam != spawnerTeam)
					return;
			}
			
			//Getter of ItemEntity
			int var2 = getItem().getCount();
			
			if(var2 <= 0 || player.getInventory().add(getItem()))
			{
				playSound(SoundEvents.ITEM_PICKUP, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				player.take(this, var2);
				
				//Getter of ItemEntity
				if(getItem().getCount() <= 0)
				{
					spawner.itemEntities.remove(this);
					discard();
				}
			}
		}
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
		discard();
	}
}
