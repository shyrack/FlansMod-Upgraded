package com.flansmod.common.teams;

import java.io.IOException;
import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import com.flansmod.client.gui.teams.EnumLoadoutSlot;
import com.flansmod.common.FlansMod;
import com.flansmod.common.util.ItemStackUtil;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.Paintjob;
import com.flansmod.common.teams.LoadoutPool.LoadoutEntryInfoType;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class PlayerLoadout
{
	public ItemStack[] slots;
	
	public PlayerLoadout()
	{
		slots = new ItemStack[EnumLoadoutSlot.values().length];
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			slots[i] = ItemStack.EMPTY.copy();
		}
	}
	
	public PlayerLoadout copy()
	{
		PlayerLoadout copy = new PlayerLoadout();
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			copy.slots[i] = slots[i] == null ? null : slots[i].copy();
		}
		return copy;
	}
	
	public void writeToBuf(ByteBuf data)
	{
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			CompoundTag stackTags = new CompoundTag();
			ItemStackUtil.writeItemStack(stackTags, "stack", slots[i]);
			try
			{
				NbtIo.write(stackTags, new ByteBufOutputStream(data));
			}
			catch(IOException e)
			{
				FlansMod.log.error("Failed to write loadout stack to buffer.", e);
			}
		}
	}
	
	public void readFromBuf(ByteBuf data)
	{
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			try
			{
				CompoundTag stackTags = NbtIo.read(new ByteBufInputStream(data));
				slots[i] = ItemStackUtil.readItemStack(stackTags, "stack");
			}
			catch(IOException e)
			{
				FlansMod.log.error("Failed to read loadout stack from buffer.", e);
				slots[i] = ItemStack.EMPTY.copy();
			}
		}
	}
	
	public void readFromNBT(CompoundTag tags)
	{
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			slots[i] = ItemStackUtil.readItemStack(tags, "slot_" + i);
			if(slots[i] == null)
				slots[i] = ItemStack.EMPTY.copy();
		}
	}
	
	public void writeToNBT(CompoundTag tags)
	{
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			if(slots[i] != null)
			{
				ItemStackUtil.writeItemStack(tags, "slot_" + i, slots[i]);
			}
		}
	}
	
	public boolean Verify(int currentLevel, ArrayList<RewardBoxInstance> rewardBoxData)
	{
		LoadoutPool pool = TeamsManagerRanked.GetInstance().currentPool;
		if(pool == null)
			return false;
		
		for(int i = 0; i < EnumLoadoutSlot.values().length; i++)
		{
			ItemStack stack = slots[i];
			if(stack == null || !(stack.getItem() instanceof IFlanItem))
				continue;
			InfoType type = ((IFlanItem)stack.getItem()).getInfoType();
			switch(EnumLoadoutSlot.values()[i])
			{
				case primary:
				case secondary:
				{
					if(!VerifyType(type, pool.unlocks[i], currentLevel))
					{
						return false;
					}
					if(type instanceof GunType)
					{
						GunType gun = (GunType)type;
						if(!VerifyType(gun.getBarrel(stack), pool.unlocks[i], currentLevel)) return false;
						if(!VerifyType(gun.getScope(stack), pool.unlocks[i], currentLevel)) return false;
						if(!VerifyType(gun.getStock(stack), pool.unlocks[i], currentLevel)) return false;
						if(!VerifyType(gun.getGrip(stack), pool.unlocks[i], currentLevel)) return false;
						if(!VerifyType(gun.getGeneric(stack, 0), pool.unlocks[i], currentLevel)) return false;
						
						Paintjob paint = gun.getPaintjob(stack.getDamageValue());
						if(!VerifyPaint(paint, rewardBoxData)) return false;
					}
					break;
				}
				case armour:
				case melee:
				case special:
				{
					if(!VerifyType(type, pool.unlocks[i], currentLevel))
					{
						return false;
					}
					break;
				}
				
				default:
				{
					FlansMod.Assert(false, "Missing case in loadout verification");
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean VerifyType(InfoType target, ArrayList<LoadoutEntryInfoType> list, int currentLevel)
	{
		if(target == null) return true;
		
		for(LoadoutEntryInfoType entry : list)
		{
			if(entry.type == target)
			{
				return entry.unlockLevel <= currentLevel;
			}
		}
		FlansMod.Assert(false, "Player put invalid item in slot " + target.shortName);
		return false;
	}
	
	private boolean VerifyPaint(Paintjob paint, ArrayList<RewardBoxInstance> rewardBoxData)
	{
		if(paint.ID == 0) return true;
		
		for(RewardBoxInstance box : rewardBoxData)
		{
			if(box.unlockHash == paint.hashCode())
			{
				return true;
			}
		}
		return false;
	}
}
