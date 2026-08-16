package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;

public class EntityFlagpole extends Entity implements ITeamBase
{
	protected Level world;
	protected static final AABB POLE_AABB = new AABB(-0.2D, 0.0D, -0.2D, 0.4D, 2.0D, 0.4D);
	
	//Set this when an op sets the base and return to it when the gametype restarts
	public int defaultTeamID;
	//This is the team that currently holds this base, reset it to default team at the end of each round
	public int currentTeamID;
	//The map this base is a part of
	public TeamsMap map;
	//List of all TeamObjects associated with this base
	public List<ITeamObject> objects = new ArrayList<>();
	// List of spawn points. 
	public List<BlockPos> spawnPoints = new ArrayList<>();
	//The name of this base, changeable by the baseList and baseRename commands
	public String name = "Default Name";
	//This base's ID
	//Do not sync IDs. Not necessary. Only sync team of objects. Much easier than syncing base to object links.
	private int ID;
	
	private EntityFlag flag;
	
	public static TeamsManager teamsManager = TeamsManager.getInstance();
	
		public EntityFlagpole(EntityType<?> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityFlagpole(Level world)
	{
		this(ModEntities.FLAGPOLE, world);
		this.world = level();

	}
	
	public EntityFlagpole(Level world, double x, double y, double z)
	{
		this(world);
		setPos(x, y, z);
		flag = new EntityFlag(world, this);
		objects.add(flag);
		if(world instanceof ServerLevel)
			((ServerLevel)world).addFreshEntity(flag);
		//flag.startRiding(this);
		if(teamsManager.maps.size() > 0)
			map = teamsManager.maps.values().iterator().next();
	}
	
	public EntityFlagpole(Level world, int x, int y, int z)
	{
		this(world, x + 0.5D, y, z + 0.5D);
	}
	
	public EntityFlagpole(Level world, BlockPos pos)
	{
		this(world, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D);
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
	}
	
	@Override
	protected void readAdditionalSaveData(ValueInput input)
	{
		setBaseID(input.getIntOr("ID", 0));
		currentTeamID = defaultTeamID = input.getIntOr("TeamID", 0);
		map = teamsManager.maps.get(input.getStringOr("Map", ""));
		name = input.getStringOr("Name", "");
		setMap(map);
	}
	
	@Override
	protected void addAdditionalSaveData(ValueOutput output)
	{
		output.putInt("TeamID", defaultTeamID);
		output.putString("Map", map == null ? "" : map.shortName);
		output.putInt("ID", getBaseID());
		output.putString("Name", name);
	}
	
	@Override
	public TeamsMap getMap()
	{
		return map;
	}
	
	@Override
	public void setMap(TeamsMap newMap)
	{
		if(newMap == null)
		{
			FlansMod.log.warn("Flagpole given invalid map");
			return;
		}
		if(map != null && map != newMap)
			map.removeBase(this);
		map = newMap;
		newMap.addBase(this);
	}
	
	public void setMapFirstTime(TeamsMap newMap)
	{
		if(newMap == null)
		{
			FlansMod.log.warn("Flagpole given invalid map");
			return;
		}
		if(map != null && map != newMap)
			map.removeBase(this);
		map = newMap;
		newMap.addBaseFirstTime(this);
	}
	
	@Override
	public List<ITeamObject> getObjects()
	{
		return objects;
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if(!world.isClientSide())
		{
			if(flag == null && getFirstPassenger() instanceof EntityFlag)
			{
				flag = (EntityFlag)getFirstPassenger();
			}
			if(flag == null)
			{
				flag = new EntityFlag(world, this);
				objects.add(flag);
			}
			if(!flag.isAlive() || world.getEntity(flag.getId()) == null)
			{
				if(world instanceof ServerLevel)
					((ServerLevel)world).addFreshEntity(flag);
			}
			if(flag.isHome)
			{
				flag.setPos(getX(), getY() + 2F, getZ());
				if(!flag.isPassenger() && tickCount > 2) // Heckin' race conditions. You'd think MC would sort queue bad passenger packets until later...
					flag.startRiding(this);
			}
		}
		
		//Temporary fire glitch fix
		if(world.isClientSide())
			extinguishFire();
	}
	
	@Override
	public void startRound()
	{
		currentTeamID = defaultTeamID;
	}
	
	@Override
	public void addObject(ITeamObject object)
	{
		objects.add(object);
		if(map != null)
		{
			map.addObject(this, object);
		}
	}
	
	@Override
	public String getBaseName()
	{
		return name;
	}
	
	@Override
	public void setBaseName(String newName)
	{
		name = newName;
	}
	
	@Override
	public void destroy()
	{
		if(map != null)
		{
			map.removeBase(this);
		}
		discard();
	}
	
	@Override
	public Entity getEntity()
	{
		return this;
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
	
	@Override
	public Level getWorld()
	{
		return world;
	}
	
	@Override
	public void roundCleanup()
	{
		if(flag != null)
			flag.reset();
	}
	
	@Override
	public ITeamObject getFlag()
	{
		return flag;
	}
	
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 vec)
	{
		if(!world.isClientSide() && player instanceof ServerPlayer)
		{
			PlayerData data = PlayerHandler.getPlayerData(player);
			if(data != null && data.team == null && TeamsManager.getInstance().playerIsOp(player) &&
				(player.getMainHandItem().isEmpty() || !(player.getMainHandItem().getItem() instanceof ItemOpStick)))
				ItemOpStick.openBaseEditGUI(this, (ServerPlayer)player);
			
			TeamsManager.getInstance().playerClickedEntity((ServerPlayer)player, this);
		}
		return InteractionResult.PASS;
	}
	
	public ItemStack getPickedResult(HitResult target)
	{
		return new ItemStack(FlansMod.flag);
	}
	
	@Override
	public void setBaseID(int i)
	{
		ID = i;
	}
	
	@Override
	public int getBaseID()
	{
		return ID;
	}
	
	@Override
	public int getDefaultOwnerID()
	{
		return defaultTeamID;
	}
	
	@Override
	public void setDefaultOwnerID(int id)
	{
		currentTeamID = defaultTeamID = id;
		for(ITeamObject object : objects)
			object.onBaseSet(id);
	}
	
	@Override
	public int getOwnerID()
	{
		return currentTeamID;
	}
	
	@Override
	public void setOwnerID(int id)
	{
		currentTeamID = id;
	}
}
