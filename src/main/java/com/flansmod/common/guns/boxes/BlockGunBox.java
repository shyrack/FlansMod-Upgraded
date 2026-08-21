package com.flansmod.common.guns.boxes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.util.FlansModUtil;
import com.flansmod.common.guns.boxes.GunBoxType.GunBoxEntry;
import com.flansmod.common.types.InfoType;

public class BlockGunBox extends Block
{
	public GunBoxType type;
	
	public BlockGunBox(GunBoxType t)
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2F, 4F).pushReaction(PushReaction.BLOCK), t);
	}

	public BlockGunBox(BlockBehaviour.Properties properties, GunBoxType t)
	{
		super(properties);
		type = t;
		type.block = this;
	}

	public void buyGun(InfoType gun, Inventory inventory, GunBoxType type)
	{
		//FlansMod.proxy.buyGun(type, gun);
		GunBoxEntry entry = type.canCraft(gun);
		if(entry != null)
		{
			boolean canBuy = true;
			for(ItemStack check : entry.requiredParts)
			{
				int numMatchingStuff = 0;
				for(int j = 0; j < inventory.getContainerSize(); j++)
				{
					ItemStack stack = inventory.getItem(j);
					if(stack != null && !stack.isEmpty() && stack.getItem() == check.getItem() && stack.getDamageValue() == check.getDamageValue())
					{
						numMatchingStuff += stack.getCount();
					}
				}
				if(numMatchingStuff < check.getCount())
				{
					canBuy = false;
				}
			}
			if(canBuy)
			{
				for(ItemStack remove : entry.requiredParts)
				{
					int amountLeft = remove.getCount();
					for(int j = 0; j < inventory.getContainerSize(); j++)
					{
						ItemStack stack = inventory.getItem(j);
						if(amountLeft > 0 && stack != null && !stack.isEmpty() && stack.getItem() == remove.getItem() && stack.getDamageValue() == remove.getDamageValue())
						{
							amountLeft -= inventory.removeItem(j, amountLeft).getCount();
						}
					}
				}
				ItemStack gunStack = new ItemStack(entry.type.item);
				if(entry.type instanceof GunType)
				{
					GunType gunType = (GunType)entry.type;
					CompoundTag tags = new CompoundTag();
					tags.putString("Paint", gunType.defaultPaintjob.iconName);
					//Add ammo tags
					ListTag ammoTagsList = new ListTag();
					for(int j = 0; j < gunType.numAmmoItemsInGun; j++)
					{
						ammoTagsList.add(new CompoundTag());
					}
					tags.put("ammo", ammoTagsList);
					
					GunUtil.setTag(gunStack, tags);
				}
				if(!inventory.add(gunStack))
				{
					// Drop gun on floor
					inventory.player.drop(gunStack, false);
				}
			}
			else
			{
				// Cant buy
				// TODO : Add flashing red squares around the items you lack
			}
		}
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(player.isCrouching())
			return InteractionResult.PASS;
		if(world.isClientSide())
			FlansMod.proxy.openGunBox(player.getInventory(), type);
		return InteractionResult.SUCCESS;
	}
}
