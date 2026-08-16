package com.flansmod.common.network;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.mechas.EntityMecha;
import com.flansmod.common.driveables.mechas.EnumMechaSlotType;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.util.ItemStackUtil;

public class PacketMechaControl extends PacketDriveableControl
{
	public float legYaw, legSwing;
	public ItemStack leftStack, rightStack;
	
	public PacketMechaControl()
	{
	}
	
	public PacketMechaControl(EntityDriveable driveable)
	{
		super(driveable);
		EntityMecha mecha = (EntityMecha)driveable;
		legYaw = mecha.legAxes.getYaw();
		legSwing = mecha.legSwing;
		leftStack = mecha.inventory.getItem(EnumMechaSlotType.leftTool);
		rightStack = mecha.inventory.getItem(EnumMechaSlotType.rightTool);
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		super.encodeInto(data);
		data.writeFloat(legYaw);
		data.writeFloat(legSwing);
		writeStack(data, leftStack);
		writeStack(data, rightStack);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		super.decodeInto(data);
		legYaw = data.readFloat();
		legSwing = data.readFloat();
		leftStack = readStack(data);
		rightStack = readStack(data);
		
	}
	
	private static void writeStack(ByteBuf data, ItemStack stack)
	{
		CompoundTag tags = new CompoundTag();
		ItemStackUtil.writeItemStack(tags, "stack", stack);
		try
		{
			NbtIo.write(tags, new ByteBufOutputStream(data));
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to write mecha stack to buffer.", e);
		}
	}
	
	private static ItemStack readStack(ByteBuf data)
	{
		try
		{
			CompoundTag tags = NbtIo.read(new ByteBufInputStream(data));
			return ItemStackUtil.readItemStack(tags, "stack");
		}
		catch(IOException e)
		{
			FlansMod.log.error("Failed to read mecha stack from buffer.", e);
			return ItemStack.EMPTY.copy();
		}
	}
	
	@Override
	protected void updateDriveable(EntityDriveable driveable, boolean clientSide)
	{
		super.updateDriveable(driveable, clientSide);
		EntityMecha mecha = (EntityMecha)driveable;
		mecha.legAxes.setAngles(legYaw, 0F, 0F);
		mecha.legSwing = legSwing / 2F;
		if(clientSide)
		{
			mecha.inventory.setItem(EnumMechaSlotType.leftTool, leftStack);
			mecha.inventory.setItem(EnumMechaSlotType.rightTool, rightStack);
		}
		else
		{
			FlansMod.getPacketHandler().sendToAllAround(new PacketMechaControl(mecha),
					posX,
					posY,
					posZ,
					FlansMod.driveableUpdateRange,
					GunUtil.getDimensionId(mecha.level()));
		}
	}
}
