package com.flansmod.common.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModBlockEntities;
import com.flansmod.common.util.ItemStackUtil;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.guns.ItemAAGun;

public class TileEntitySpawner extends BlockEntity implements ITeamObject
{
	//Server side
	public int spawnDelay = 1200;
	public List<ItemStack> stacksToSpawn = new ArrayList<>();
	public List<EntityTeamItem> itemEntities = new ArrayList<>();
	public Entity spawnedEntity;
	public ITeamBase base;
	private int baseID = -1;
	private int dimension;
	public int currentDelay;
	public boolean isSpawner = false;
	
	//Client side
	private int teamID;
	public String map;
	
	public TileEntitySpawner(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.SPAWNER, pos, state);
		TeamsManager.getInstance().registerObject(this);
	}
	
	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output)
	{
		super.saveAdditional(output);
		isSpawner = isSpawnPoint();
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("isSpawner", isSpawner);
		nbt.putInt("delay", spawnDelay);
		nbt.putInt("Base", baseID);
		if(getLevel() != null)
			nbt.putInt("dim", getLevel().dimension() == Level.NETHER ? -1 : getLevel().dimension() == Level.END ? 1 : 0);
		nbt.putInt("numStacks", stacksToSpawn.size());
		for(int i = 0; i < stacksToSpawn.size(); i++)
		{
			ItemStackUtil.writeItemStack(nbt, "stack" + i, stacksToSpawn.get(i));
		}
		nbt.putByte("TeamID", base == null ? (byte)0 : (byte)base.getOwnerID());
		nbt.putString("Map", base == null || base.getMap() == null ? "" : base.getMap().shortName);
		output.store("FlanData", CompoundTag.CODEC, nbt);
	}
	
	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input)
	{
		super.loadAdditional(input);
		CompoundTag nbt = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		isSpawner = nbt.getBooleanOr("isSpawner", false);
		currentDelay = spawnDelay = nbt.getIntOr("delay", 0);
		baseID = nbt.getIntOr("Base", 0);
		dimension = nbt.getIntOr("dim", 0);
		teamID = nbt.getByteOr("TeamID", (byte)0);
		map = nbt.getStringOr("Map", "");
		setBase(TeamsManager.getInstance().getBase(baseID));
		if(base != null)
			base.addObject(this);
		for(int i = 0; i < nbt.getIntOr("numStacks", 0); i++)
		{
			stacksToSpawn.add(ItemStackUtil.readItemStack(nbt, "stack" + i));
		}
	}
	
	public static void tick(Level world, BlockPos pos, BlockState state, TileEntitySpawner spawner)
	{
		spawner.tick();
	}
	
	public void tick()
	{
		if(getLevel() == null || getLevel().isClientSide())
			return;
		//If the base was loaded after the spawner, check to see if the base has now been loaded
		if(baseID >= 0 && base == null)
		{
			ITeamBase newBase = TeamsManager.getInstance().getBase(baseID);
			if(newBase != null)
			{
				setBase(newBase);
				newBase.addObject(this);
			}
		}
		if(getLevel().getBlockState(getBlockPos()).getBlock() != FlansMod.spawner)
		{
			destroy();
			return;
		}
		if(getLevel().getBlockState(getBlockPos()).getValue(BlockSpawner.TYPE) == 1)
		{
			isSpawner = true;
			return;
		}
		for(int i = itemEntities.size() - 1; i >= 0; i--)
		{
			if(!itemEntities.get(i).isAlive())
				itemEntities.remove(i);
		}
		if(currentDelay > 0 && itemEntities.isEmpty())
		{
			currentDelay--;
		}
		if(currentDelay == 0)
		{
			currentDelay = spawnDelay;
			for(int i = 0; i < stacksToSpawn.size(); i++)
			{
				if(getLevel().getBlockState(getBlockPos()).getValue(BlockSpawner.TYPE) == 2)
				{
					if(spawnedEntity != null && !spawnedEntity.isRemoved())
					{
						continue;
					}
					ItemStack stack = stacksToSpawn.get(i);
					if(stack != null && stack.getItem() instanceof ItemPlane)
					{
						spawnedEntity = ((ItemPlane)stack.getItem()).spawnPlane(getLevel(), getBlockPos().getX() + 0.5F, getBlockPos().getY() + 0.5F, getBlockPos().getZ() + 0.5F, stack);
					}
					if(stack != null && stack.getItem() instanceof ItemVehicle)
					{
						spawnedEntity = ((ItemVehicle)stack.getItem()).spawnVehicle(getLevel(), getBlockPos().getX() + 0.5F, getBlockPos().getY() + 0.5F, getBlockPos().getZ() + 0.5F, stack);
					}
					if(stack != null && stack.getItem() instanceof ItemAAGun)
					{
						spawnedEntity = ((ItemAAGun)stack.getItem()).spawnAAGun(getLevel(), getBlockPos().getX() + 0.5F, getBlockPos().getY(), getBlockPos().getZ() + 0.5F, stack);
					}
				}
				else
				{
					EntityTeamItem itemEntity = new EntityTeamItem(this, i);
					if(getLevel() instanceof net.minecraft.server.level.ServerLevel)
						((net.minecraft.server.level.ServerLevel)getLevel()).addFreshEntity(itemEntity);
				}
			}
		}
	}
	
	@Override
	public ITeamBase getBase()
	{
		return base;
	}
	
	public int getTeamID()
	{
		if(getLevel() != null && getLevel().isClientSide())
			return teamID;
		else return base == null ? 0 : base.getOwnerID();
	}
	
	@Override
	public void onBaseSet(int newTeamID)
	{
		updateToClients();
	}
	
	@Override
	public void onBaseCapture(int newTeamID)
	{
		onBaseSet(newTeamID);
	}
	
	@Override
	public void setBase(ITeamBase b)
	{
		base = b;
		if(b != null)
			baseID = b.getBaseID();
		if(getLevel() != null && !getLevel().isClientSide())
		{
			updateToClients();
		}
	}
	
	private void updateToClients()
	{
		if(getLevel() != null && !getLevel().isClientSide())
		{
			BlockState state = getLevel().getBlockState(getBlockPos());
			getLevel().sendBlockUpdated(getBlockPos(), state, state, 2);
		}
	}
	
	@Override
	public void destroy()
	{
		if(getLevel() != null)
			getLevel().setBlockAndUpdate(getBlockPos(), Blocks.AIR.defaultBlockState());
	}
	
	@Override
	public double getPosX()
	{
		return getBlockPos().getX() + 0.5F;
	}
	
	@Override
	public double getPosY()
	{
		return getBlockPos().getY() + 0.5F;
	}
	
	@Override
	public double getPosZ()
	{
		return getBlockPos().getZ() + 0.5F;
	}
	
	@Override
	public boolean isSpawnPoint()
	{
		return isSpawner;
	}
	
	@Override
	public boolean forceChunkLoading()
	{
		return false;
	}
	
	public Level getWorld()
	{
		return getLevel();
	}
}
