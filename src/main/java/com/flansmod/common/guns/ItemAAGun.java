package com.flansmod.common.guns;

import com.flansmod.common.ModItems;
import java.util.ArrayList;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class ItemAAGun extends Item implements IFlanItem
{
	public static final ArrayList<String> names = new ArrayList<>();
	public AAGunType type;
	
	public ItemAAGun(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemAAGun(AAGunType type1)
	{
		super(new Item.Properties().stacksTo(1).setId(ModItems.itemKey(type1)));
		type = type1;
		type.item = this;
	}
	
	@Override
	public InteractionResult use(Level world, Player entityplayer, InteractionHand hand)
	{
		ItemStack itemstack = entityplayer.getItemInHand(hand);
		//Raytracing
		float cosYaw = Mth.cos(-entityplayer.getYRot() * 0.01745329F - 3.141593F);
		float sinYaw = Mth.sin(-entityplayer.getYRot() * 0.01745329F - 3.141593F);
		float cosPitch = -Mth.cos(-entityplayer.getXRot() * 0.01745329F);
		float sinPitch = Mth.sin(-entityplayer.getXRot() * 0.01745329F);
		double length = 5D;
		Vec3 posVec = new Vec3(entityplayer.getX(), entityplayer.getEyeY(), entityplayer.getZ());
		Vec3 lookVec = posVec.add(sinYaw * cosPitch * length, sinPitch * length, cosYaw * cosPitch * length);
		HitResult hit = world.clip(new ClipContext(posVec, lookVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entityplayer));
		
		//Result check
		if(hit == null)
		{
			return InteractionResult.PASS;
		}
		if(hit.getType() == HitResult.Type.BLOCK)
		{
			int i = ((BlockHitResult)hit).getBlockPos().getX();
			int j = ((BlockHitResult)hit).getBlockPos().getY();
			int k = ((BlockHitResult)hit).getBlockPos().getZ();
			if(!world.isClientSide() && world.getBlockState(((BlockHitResult)hit).getBlockPos()).isSolidRender() && ((BlockHitResult)hit).getDirection() == Direction.UP)
			{
				((ServerLevel)world).addFreshEntity(new EntityAAGun(world, type, (double)i + 0.5F, (double)j + 1F, (double)k + 0.5F, entityplayer));
			}
			if(!entityplayer.getAbilities().instabuild)
			{
				itemstack.setCount(itemstack.getCount() - 1);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	
	public Entity spawnAAGun(Level world, double x, double y, double z, ItemStack stack)
	{
		Entity entity = new EntityAAGun(world, type, x, y, z, null);
		if(!world.isClientSide())
		{
			((ServerLevel)world).addFreshEntity(entity);
		}
		return entity;
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
}
