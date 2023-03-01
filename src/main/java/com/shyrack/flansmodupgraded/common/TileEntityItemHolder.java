package com.shyrack.flansmodupgraded.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityItemHolder<T extends BlockEntity> extends BlockEntity implements Inventory {

    private ItemStack stack;
    public ItemHolderType type;

    public TileEntityItemHolder(BlockEntityType<T> blockEntityType, BlockPos blockPos, BlockState blockState, ItemHolderType itemHolderType) {
        super(blockEntityType, blockPos, blockState);
        this.stack = ItemStack.EMPTY.copy();
        this.type = itemHolderType;
    }

    @Override
    public String getName() {
        return "ItemHolder";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return null;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return getStack();
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (getStack() != null && !getStack().isEmpty()) {
            getStack().setCount(getStack().getCount() - count);
            if (getStack().getCount() <= 0)
                setStack(ItemStack.EMPTY.copy());
        }
        updateToClients();
        return getStack();
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.setStack(stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(Player player) {
        return true;
    }

    @Override
    public void openInventory(Player player) {
    }

    @Override
    public void closeInventory(Player player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        NBTTagCompound stackNBT = new NBTTagCompound();
        getStack().writeToNBT(stackNBT);
        nbt.setTag("stack", stackNBT);
        nbt.setString("type", type.shortName);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        stack = new ItemStack(nbt.getCompoundTag("stack"));
        if (type == null)
            type = ItemHolderType.getItemHolder(nbt.getString("type"));
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), nbt);
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
        updateToClients();
    }

    @Override
    public boolean isEmpty() {
        return stack == null || stack.isEmpty();
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack temp = stack;
        stack = ItemStack.EMPTY.copy();
        updateToClients();
        return temp;
    }

    private void updateToClients() {
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
        markDirty();
    }

}
