package com.flansmod.common.teams;

import com.flansmod.common.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import com.flansmod.common.FlansMod;
import com.flansmod.common.teams.ArmourBoxType.ArmourBoxEntry;

public class BlockArmourBox extends Block
{
	public ArmourBoxType type;
	
	public BlockArmourBox(ArmourBoxType t)
	{
		this(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2F, 4F).pushReaction(PushReaction.BLOCK), t);
	}

	public BlockArmourBox(BlockBehaviour.Properties properties, ArmourBoxType t)
	{
		super(properties);
		type = t;
		
		type.block = this;
		type.item = com.flansmod.common.ModItems.blockItem(this);
		ModItems.registerTypeItem(type.item, type);
	}
	
	public Block setTranslationKey(String key)
	{
		return this;
	}
	
	public void buyArmour(String shortName, int piece, Inventory inventory)
	{
		if(FlansMod.isClient())
		{
			FlansMod.proxy.buyArmour(shortName, piece, type);
		}
		ArmourBoxEntry entryPicked = null;
		for(ArmourBoxEntry page : type.pages)
		{
			if(page.shortName.equals(shortName))
				entryPicked = page;
		}
		
		ItemStack resultStack = new ItemStack(entryPicked.armours[piece].item);
		
		//Check the player has the required items
		for(ItemStack check : entryPicked.requiredStacks[piece])
		{
			int numMatchingStuff = 0;
			for(int j = 0; j < inventory.getContainerSize(); j++)
			{
				ItemStack stack = inventory.getItem(j);
				if(stack != null && !stack.isEmpty() && stack.getItem() == check.getItem() && stack.getDamageValue() == check.getDamageValue())
					numMatchingStuff += stack.getCount();
			}
			if(numMatchingStuff < check.getCount())
				return;
		}
		//Take the required items
		for(ItemStack remove : entryPicked.requiredStacks[piece])
		{
			int amountLeft = remove.getCount();
			for(int j = 0; j < inventory.getContainerSize(); j++)
			{
				ItemStack stack = inventory.getItem(j);
				if(amountLeft > 0 && stack != null && !stack.isEmpty() && stack.getItem() == remove.getItem() && stack.getDamageValue() == remove.getDamageValue())
					amountLeft -= inventory.removeItem(j, amountLeft).getCount();
			}
		}
		if(!inventory.add(resultStack))
			inventory.player.drop(resultStack, false);
	}
	
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if(player.isCrouching())
			return InteractionResult.PASS;
		if(world.isClientSide())
			FlansMod.proxy.openArmourBox(player.getInventory(), type);
		return InteractionResult.SUCCESS;
	}
}
