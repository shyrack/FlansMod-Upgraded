package com.flansmod.common.teams;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerHandler;

public class EntityFlag extends Entity implements ITeamObject
{
	protected Level world;
	private static final EntityDataAccessor<Byte> TEAMID = SynchedEntityData.defineId(EntityFlag.class, EntityDataSerializers.BYTE);
	
	public int baseID;
	public EntityFlagpole base;
	public boolean isHome = true;
	public int timeUntilReturn;
	
		public EntityFlag(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityFlag(Level world)
	{
		this(ModEntities.FLAG, world);
		this.world = level();

	}
	
	public EntityFlag(Level world, EntityFlagpole pole)
	{
		this(world);
		setPos(pole.getX(), pole.getY() + 2F, pole.getZ());
		setBase(pole);
	}
	
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount)
	{
		return false;
	}
	
	@Override
	public boolean isPickable()
	{
		return true;
	}
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(TEAMID, (byte)0);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		//If the base is null, maybe because the flag loaded before the base, check again to see if it exists.
		//Do not do this client side
		if(base == null && !world.isClientSide())
		{
			setBase(TeamsManager.getInstance().getBase(baseID));
		}
		if(getVehicle() != null && getVehicle().isRemoved())
		{
			if(getVehicle() instanceof ServerPlayer)
			{
				ServerPlayer player = ((ServerPlayer)getVehicle());
				Team team = PlayerHandler.getPlayerData(player.getName().getString()).team;
				TeamsManager.getInstance();
				TeamsManager.messageAll("\u00a7f" + player.getName().getString() + " dropped the \u00a7" + team.textColour + team.name + "\u00a7f flag");
			}
			else if(getVehicle() instanceof EntityFlagpole)
			{
				discard();
			}
			stopRiding();
		}
		if(getRemovalReason() != null || world.getEntity(getId()) == null)
		{
			if(world instanceof ServerLevel)
				((ServerLevel)world).addFreshEntity(this);
		}
		
		if(timeUntilReturn > 0)
		{
			if(getVehicle() instanceof ServerPlayer || isHome)
				timeUntilReturn = 0;
			else
			{
				timeUntilReturn--;
				if(timeUntilReturn == 0)
				{
					reset();
					Team flagTeam = TeamsManager.getInstance().getTeam(getBase().getOwnerID());
					TeamsManager.messageAll("\u00a7fThe \u00a7" + flagTeam.textColour + flagTeam.name + "\u00a7f flag returned itself");
				}
			}
		}
		
		//Temporary fire glitch fix
		if(world.isClientSide())
			extinguishFire();
	}
	
	@Override
	public void stopRiding()
	{
		super.stopRiding();
		if(TeamsManager.getInstance().currentRound != null && TeamsManager.getInstance().currentRound.gametype instanceof GametypeCTF)
		{
			timeUntilReturn = ((GametypeCTF)TeamsManager.getInstance().currentRound.gametype).flagReturnTime * 20;
		}
		else timeUntilReturn = 600; //30 seconds
	}
	
	public void reset()
	{
		if(base == null)
		{
			if(getVehicle() instanceof EntityFlagpole)
				base = (EntityFlagpole)getVehicle();
		}
		stopRiding();
		if(base != null)
			setPos(base.getX(), base.getY() + 2F, base.getZ());
		//startRiding(base);
		isHome = true;
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
		//baseID = input.getIntOr("Base", 0);
		//setBase(TeamsManager.getInstance().getBase(baseID));
	}
	
	@Override
	protected void addAdditionalSaveData(ValueOutput output)
	{
		//output.putInt("Base", base == null ? -1 : base.getBaseID());
	}
	
	@Override
	public ITeamBase getBase()
	{
		return base;
	}
	
	@Override
	public void onBaseSet(int newTeamID)
	{
		entityData.set(TEAMID, (byte)newTeamID);
		if(base != null)
			setPos(base.getX(), base.getY() + 2F, base.getZ());
		//startRiding(base);
	}
	
	@Override
	public void onBaseCapture(int newTeamID)
	{
		onBaseSet(newTeamID);
	}
	
	@Override
	public void setBase(ITeamBase b)
	{
		base = (EntityFlagpole)b;
		if(base != null)
		{
			base.addObject(this);
			onBaseSet(base.getOwnerID());
		}
	}
	
	@Override
	public void destroy()
	{
		discard();
	}
	
	@Override
	public double getPosX()
	{
		return getX();
	}
	
	@Override
	public double getPosY()
	{
		return getY();
	}
	
	@Override
	public double getPosZ()
	{
		return getZ();
	}
	
	public int getTeamID()
	{
		return entityData.get(TEAMID);
	}
	
	@Override
	public boolean isSpawnPoint()
	{
		return false;
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 vec)
	{
		if(player instanceof ServerPlayer)
			TeamsManager.getInstance().playerClickedEntity((ServerPlayer)player, this);
		return InteractionResult.PASS;
	}
	
	public ItemStack getPickedResult(HitResult target)
	{
		return new ItemStack(FlansMod.flag);
	}
	
	@Override
	public boolean forceChunkLoading()
	{
		return false;
	}
}
