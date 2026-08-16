package com.flansmod.common.driveables.mechas;

import com.flansmod.common.ModItems;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;

import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.EnumDriveablePart;
import com.flansmod.common.paintjob.IPaintableItem;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;

public class ItemMecha extends Item implements IPaintableItem
{
	public MechaType type;

	public ItemMecha(Item.Properties properties)
	{
		super(properties.stacksTo(1));
	}

	public ItemMecha(MechaType type1)
	{
		this(new Item.Properties().stacksTo(1).setId(ModItems.itemKey(type1)));
		type = type1;
		type.item = this;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type != null && type.description != null)
		{
			for(String line : type.description.split("_"))
			{
				tooltip.accept(Component.literal(line));
			}
		}
		CompoundTag tags = getTagCompound(stack, null);
		String engineName = tags.getStringOr("Engine", "");
		PartType part = PartType.getPart(engineName);
		if(part != null)
			tooltip.accept(Component.literal(part.name));
	}
	
	private CompoundTag getTagCompound(ItemStack stack, Level world)
	{
		CompoundTag tags = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if(tags.isEmpty())
		{
			tags.putString("Type", type.shortName);
			if(PartType.defaultEngines.containsKey(EnumType.mecha))
				tags.putString("Engine", PartType.defaultEngines.get(EnumType.mecha).shortName);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tags));
		}
		return tags;
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
		Vec3 posVec = new Vec3(entityplayer.getX(), entityplayer.getY() + entityplayer.getEyeHeight(), entityplayer.getZ());
		Vec3 lookVec = posVec.add(sinYaw * cosPitch * length, sinPitch * length, cosYaw * cosPitch * length);
		HitResult hit = world.clip(new ClipContext(posVec, lookVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entityplayer));

		//Result check
		if(hit == null)
		{
			return InteractionResult.PASS;
		}
		if(hit.getType() == HitResult.Type.BLOCK)
		{
			BlockPos pos = ((net.minecraft.world.phys.BlockHitResult)hit).getBlockPos();
			if(!world.isClientSide())
			{
				world.addFreshEntity(new EntityMecha(world, (double)pos.getX() + 0.5F, (double)pos.getY() + 1.5F + type.yOffset, (double)pos.getZ() + 0.5F, entityplayer, type, getData(itemstack, world), getTagCompound(itemstack, world)));
			}
			if(!entityplayer.getAbilities().instabuild)
			{
				itemstack.setCount(itemstack.getCount() - 1);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	
	public DriveableData getData(ItemStack itemstack, Level world)
	{
		return new DriveableData(getTagCompound(itemstack, world), itemstack.getDamageValue());
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}

	@Override
	public InfoType getInfoType()
	{
		return type;
	}

	@Override
	public PaintableType GetPaintableType()
	{
		return type;
	}
}
