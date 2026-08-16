package com.flansmod.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.enchantments.EnchantmentModule;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.GunUtil;

/**
 * This packet is send by the client to request a reload. The server checks if the player can reload and in this case actually reloads and sends a GunAnimationPacket as response.
 * The GunAnimationPacket plays the reload animation and sets the pumpDelay & pumpTime times to prevent the client from shooting while reloading
 */
public class PacketReload extends PacketBase
{
	public boolean isOffHand;
	public boolean isForced;
	
	public PacketReload()
	{
	}
	
	public PacketReload(InteractionHand hand, boolean isForced)
	{
		this.isOffHand = hand == InteractionHand.OFF_HAND;
		this.isForced = isForced;
	}
	
	@Override
	public void encodeInto(ByteBuf data)
	{
		data.writeBoolean(isOffHand);
		data.writeBoolean(isForced);
	}
	
	@Override
	public void decodeInto(ByteBuf data)
	{
		isOffHand = data.readBoolean();
		isForced = data.readBoolean();
	}
	
	@Override
	public void handleServerSide(ServerPlayer playerEntity)
	{
		InteractionHand hand = isOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		PlayerData data = PlayerHandler.getPlayerData(playerEntity);
		ItemStack main = playerEntity.getMainHandItem();
		ItemStack off = playerEntity.getOffhandItem();
		ItemStack stack = isOffHand ? off : main;
		boolean hasOffHand = main != null && !main.isEmpty() && off != null && !off.isEmpty();
		ItemStack otherHand = isOffHand ? main : off;
		if(data != null && stack != null && stack.getItem() instanceof ItemGun)
		{
			GunType type = ((ItemGun)stack.getItem()).GetType();
			
			if(((ItemGun)stack.getItem()).Reload(stack, playerEntity.level(), playerEntity, playerEntity.getInventory(), hand, hasOffHand, isForced, playerEntity.getAbilities().instabuild))
			{
				float reloadDelay = EnchantmentModule.ModifyReloadTime(type.reloadTime, playerEntity, otherHand);
				
				//Set the reload delay
				data.shootTimeRight = data.shootTimeLeft = reloadDelay;
				if(isOffHand)
					data.reloadingLeft = true;
				else data.reloadingRight = true;
				//Play reload sound
				if(type.reloadSound != null)
					PacketPlaySound.sendSoundPacket(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(playerEntity.level()), type.reloadSound, false);
			
				FlansMod.getPacketHandler().sendTo(new PacketGunAnimation(hand, (int)reloadDelay, type.getPumpDelayAfterReload(), type.getPumpTime()), playerEntity);
			}
		}
	}
	
	@Override
	public void handleClientSide(Player clientPlayer)
	{
		FlansMod.log.warn("Recieved reload packet on client!");
	}
}
