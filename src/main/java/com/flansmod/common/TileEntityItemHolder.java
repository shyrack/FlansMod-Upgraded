package com.flansmod.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.flansmod.common.util.ItemStackUtil;

public class TileEntityItemHolder extends BlockEntity implements Container
{
	private ItemStack stack = ItemStack.EMPTY.copy();
	public ItemHolderType type;
	
	public TileEntityItemHolder(BlockPos pos, BlockState state)
	{
		super(ModBlockEntities.ITEM_HOLDER, pos, state);
	}
	
	public TileEntityItemHolder(ItemHolderType type)
	{
		this(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		this.type = type;
	}
	
	@Override
	public int getContainerSize()
	{
		return 1;
	}
	
	@Override
	public ItemStack getItem(int index)
	{
		return getStack();
	}
	
	@Override
	public ItemStack removeItem(int index, int count)
	{
		if(getStack() != null && !getStack().isEmpty())
		{
			getStack().setCount(getStack().getCount() - count);
			if(getStack().getCount() <= 0)
				setStack(ItemStack.EMPTY.copy());
		}
		updateToClients();
		return getStack();
	}
	
	@Override
	public ItemStack removeItemNoUpdate(int index)
	{
		ItemStack temp = stack;
		stack = ItemStack.EMPTY.copy();
		return temp;
	}
	
	@Override
	public void setItem(int index, ItemStack stack)
	{
		this.setStack(stack);
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
		return stack == null || stack.isEmpty();
	}
	
	@Override
	public void clearContent()
	{
		stack = ItemStack.EMPTY.copy();
		updateToClients();
	}
	
	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input)
	{
		super.loadAdditional(input);
		
		CompoundTag nbt = input.read("FlanData", CompoundTag.CODEC).orElse(new CompoundTag());
		stack = ItemStackUtil.readItemStack(nbt, "stack");
		if(type == null)
			type = ItemHolderType.getItemHolder(nbt.getStringOr("type", ""));
	}
	
	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output)
	{
		super.saveAdditional(output);
		
		CompoundTag nbt = new CompoundTag();
		ItemStackUtil.writeItemStack(nbt, "stack", stack);
		if(type != null)
			nbt.putString("type", type.shortName);
		output.store("FlanData", CompoundTag.CODEC, nbt);
	}
	
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider)
	{
		CompoundTag nbt = new CompoundTag();
		ItemStackUtil.writeItemStack(nbt, "stack", stack);
		if(type != null)
			nbt.putString("type", type.shortName);
		return nbt;
	}
	
	public ItemStack getStack()
	{
		return stack;
	}
	
	public void setStack(ItemStack stack)
	{
		this.stack = stack;
		updateToClients();
	}
	
	private void updateToClients()
	{
		if(getLevel() == null)
			return;
		BlockState state = getLevel().getBlockState(worldPosition);
		getLevel().sendBlockUpdated(worldPosition, state, state, 3);
		setChanged();
	}
}
