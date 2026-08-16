package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.ItemGun;

public class PacketGunFire extends PacketBase
{
	private InteractionHand hand;
	
	public PacketGunFire() {
		
	}
	
	public PacketGunFire(InteractionHand hand)
	{
		this.hand = hand;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		//TODO Proper packet enum encoding
		data.writeInt(InteractionHand.MAIN_HAND.equals(hand)?0:1);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		//TODO Proper packet enum encoding
		hand = data.readInt()==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		ItemStack itemstack = playerEntity.getItemInHand(hand);
		//TODO can itemstack be null?
		Item item = itemstack.getItem();
		if (item instanceof ItemGun) {
			ItemGun gun = (ItemGun) item;
			gun.shootServer(hand, playerEntity, itemstack);
			
		} else {
			FlansMod.log.warn("Received invalid PacketGunFire. Item in hand is not an instance of ItemGun");
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Received gun button packet on client. Skipping.");
	}
}
