package com.flansmod.common.paintjob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.flansmod.common.ModBlockEntities;
import com.flansmod.common.util.ItemStackUtil;

public class TileEntityPaintjobTable extends BlockEntity implements Container
{
	// Stack 0 is InfoType being painted. Stack 1 is paint cans
	private ItemStack inventoryStacks[] = new ItemStack[]{ItemStack.EMPTY.copy(), ItemStack.EMPTY.copy()};
	//private CustomPaintjob inProgressPaintjob;
	
	public TileEntityPaintjobTable(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.PAINTJOB_TABLE, pos, state);
	}
	
	@Override
	public int getContainerSize()
	{
		return 2;
	}
	
	@Override
	public ItemStack getItem(int index)
	{
		return inventoryStacks[index];
	}
	
	@Override
	public ItemStack removeItem(int index, int count)
	{
		ItemStack stack = inventoryStacks[index];
		if(stack == null || stack.isEmpty())
			return ItemStack.EMPTY.copy();
		if(count >= stack.getCount())
		{
			inventoryStacks[index] = ItemStack.EMPTY.copy();
			return stack;
		}
		ItemStack split = stack.split(count);
		setChanged();
		return split;
	}
	
	@Override
	public ItemStack removeItemNoUpdate(int index)
	{
		ItemStack stack = inventoryStacks[index];
		inventoryStacks[index] = ItemStack.EMPTY.copy();
		return stack;
	}
	
	@Override
	public void setItem(int index, ItemStack stack)
	{
		inventoryStacks[index] = stack;
		setChanged();
	}
	
	@Override
	public int getMaxStackSize()
	{
		return 64;
	}
	
	@Override
	public boolean canPlaceItem(int index, ItemStack stack)
	{
		return true;
	}
	
	@Override
	public void setChanged()
	{
		super.setChanged();
	}
	
	@Override
	public boolean stillValid(Player player)
	{
		return true;
	}
	
	@Override
	public boolean isEmpty()
	{
		return (inventoryStacks[0] == null || inventoryStacks[0].isEmpty()) && (inventoryStacks[1] == null || inventoryStacks[1].isEmpty());
	}
	
	@Override
	public void clearContent()
	{
		for(int i = 0; i < getContainerSize(); i++)
		{
			inventoryStacks[i] = ItemStack.EMPTY.copy();
		}
	}
	
	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output)
	{
		super.saveAdditional(output);
		CompoundTag nbt = new CompoundTag();
		for(int i = 0; i < inventoryStacks.length; i++)
		{
			ItemStackUtil.writeItemStack(nbt, "stack_" + i, inventoryStacks[i]);
		}
		output.store("FlanData", CompoundTag.CODEC, nbt);
	}
	
	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input)
	{
		super.loadAdditional(input);
		CompoundTag nbt = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		for(int i = 0; i < inventoryStacks.length; i++)
		{
			inventoryStacks[i] = ItemStackUtil.readItemStack(nbt, "stack_" + i);
		}
	}
	
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket()
	{
		return null;
	}
	
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag nbt = new CompoundTag();
		for(int i = 0; i < inventoryStacks.length; i++)
		{
			ItemStackUtil.writeItemStack(nbt, "stack_" + i, inventoryStacks[i]);
		}
		return nbt;
	}
	
	public ItemStack getPaintableStack()
	{
		return inventoryStacks[0];
	}
	
	public void setPaintableStack(ItemStack stack)
	{
		inventoryStacks[0] = stack;
	}
	
	public ItemStack getPaintCans()
	{
		return inventoryStacks[1];
	}
	
}
