package com.flansmod.common.tools;

import com.flansmod.common.ModItems;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;

import com.flansmod.client.debug.EntityDebugVector;
import com.flansmod.common.FlansMod;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.network.PacketFlak;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.vector.Vector3f;

public class ItemTool extends Item implements IFlanItem
{
	public ToolType type;
	
	public ItemTool(Item.Properties properties)
	{
		super(properties.stacksTo(1));
	}
	
	public ItemTool(ToolType t)
	{
		this(new Item.Properties().stacksTo(1).durability(t.toolLife).setId(ModItems.itemKey(t)));
		type = t;
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
	}
	
	@Override
	public InteractionResult use(Level world, Player entityplayer, InteractionHand hand)
	{
		ItemStack itemstack = entityplayer.getItemInHand(hand);
		if(type.foodness > 0)
			return InteractionResult.PASS;
		
		else if(type.parachute)
		{
			//Create a parachute, spawn it and put the player in it
			if(!world.isClientSide())
			{
				EntityParachute parachute = new EntityParachute(world, type, entityplayer);
				world.addFreshEntity(parachute);
				entityplayer.startRiding(parachute);
			}
			
			//If not in creative and the tool should decay, damage it
			if(!entityplayer.getAbilities().instabuild && type.toolLife > 0)
				itemstack.setDamageValue(itemstack.getDamageValue() + 1);
			//If the tool is damagable and is destroyed upon being used up, then destroy it
			if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getDamageValue() == itemstack.getMaxDamage())
				itemstack.setCount(itemstack.getCount() - 1);
			//Our work here is done. Let's be off
			return InteractionResult.SUCCESS;
		}
		else if(type.remote)
		{
			PlayerData data = PlayerHandler.getPlayerData(entityplayer);
			//If we have some remote explosives out there
			if(data != null && data.remoteExplosives.size() > 0)
			{
				//Detonate it
				data.remoteExplosives.get(0).detonate();
				//Remove it from the list to detonate
				if(data.remoteExplosives.get(0).detonated)
					data.remoteExplosives.remove(0);
				
				//If not in creative and the tool should decay, damage it
				if(!entityplayer.getAbilities().instabuild && type.toolLife > 0)
					itemstack.setDamageValue(itemstack.getDamageValue() + 1);
				//If the tool is damagable and is destroyed upon being used up, then destroy it
				if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getDamageValue() == itemstack.getMaxDamage())
					itemstack.setCount(itemstack.getCount() - 1);
				//Our work here is done. Let's be off
				return InteractionResult.SUCCESS;
			}
		}
		else
		{
			
			//Raytracing
			float cosYaw = Mth.cos(-entityplayer.getYRot() * 0.01745329F);
			float sinYaw = Mth.sin(-entityplayer.getYRot() * 0.01745329F);
			float cosPitch = -Mth.cos(entityplayer.getXRot() * 0.01745329F);
			float sinPitch = Mth.sin(entityplayer.getXRot() * 0.01745329F);
			double length = 5D;
			Vec3 posVec = new Vec3(entityplayer.getX(), entityplayer.getY() + entityplayer.getEyeHeight(), entityplayer.getZ());
			Vec3 lookVec = posVec.add(sinYaw * cosPitch * length, sinPitch * length, cosYaw * cosPitch * length);
			
			if(world.isClientSide() && FlansMod.DEBUG)
			{
				world.addFreshEntity(new EntityDebugVector(world, new Vector3f(posVec), new Vector3f(posVec.subtract(lookVec)), 100));
			}
			
			if(type.healDriveables)
			{
				//Iterate over all EntityDriveables
				for(Entity obj : world.getEntities((Entity)null, new AABB(posVec, lookVec).inflate(2D), e -> true))
				{
					if(obj instanceof EntityDriveable)
					{
						EntityDriveable driveable = (EntityDriveable)obj;
						//Raytrace
						DriveablePart part = driveable.raytraceParts(new Vector3f(posVec), Vector3f.sub(new Vector3f(lookVec), new Vector3f(posVec), null));
						//If we hit something that is healable
						if(part != null && part.maxHealth > 0)
						{
							//If its broken and the tool is inifinite or has durability left
							if(part.health < part.maxHealth && (type.toolLife == 0 || itemstack.getDamageValue() < itemstack.getMaxDamage()))
							{
								//Heal it
								part.health += type.healAmount;
								//If it is over full health, cap it
								if(part.health > part.maxHealth)
									part.health = part.maxHealth;
								//If not in creative and the tool should decay, damage it
								if(!entityplayer.getAbilities().instabuild && type.toolLife > 0)
									itemstack.setDamageValue(itemstack.getDamageValue() + 1);
								//If the tool is damagable and is destroyed upon being used up, then destroy it
								if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getDamageValue() == itemstack.getMaxDamage())
									itemstack.setCount(itemstack.getCount() - 1);
								//Our work here is done. Let's be off
								return InteractionResult.SUCCESS;
							}
						}
					}
				}
			}
			
			if(!world.isClientSide() && type.healPlayers)
			{
				//By default, heal the player
				LivingEntity hitLiving = entityplayer;
				
				//Iterate over entities within range of the ray
				List<Entity> list = world.getEntities((Entity)null, new AABB(
						Math.min(posVec.x, lookVec.x), Math.min(posVec.y, lookVec.y), Math.min(posVec.z, lookVec.z),
						Math.max(posVec.x, lookVec.x), Math.max(posVec.y, lookVec.y), Math.max(posVec.z, lookVec.z)), e -> e instanceof LivingEntity);
				for(Entity aList : list)
				{
					if(!(aList instanceof LivingEntity))
						continue;
					LivingEntity checkEntity = (LivingEntity)aList;
					//Don't check the player using it
					if(checkEntity == entityplayer)
						continue;
					//Do a more accurate ray trace on this entity
					//If it hit, heal it
					if(checkEntity.getBoundingBox().clip(posVec, lookVec).isPresent())
						hitLiving = checkEntity;
				}
				//Now heal whatever it was we just decided to heal
				if(hitLiving != null)
				{
					//If its finished, don't use it
					if(itemstack.getDamageValue() >= itemstack.getMaxDamage() && type.toolLife > 0)
						return InteractionResult.FAIL;
					
					hitLiving.heal(type.healAmount);
					FlansMod.getPacketHandler().sendToAllAround(new PacketFlak(hitLiving.getX(), hitLiving.getY(), hitLiving.getZ(), 5, "heart"), hitLiving.getX(), hitLiving.getY(), hitLiving.getZ(), 50F);
					
					//If not in creative and the tool should decay, damage it
					if(!entityplayer.getAbilities().instabuild && type.toolLife > 0)
						itemstack.setDamageValue(itemstack.getDamageValue() + 1);
					//If the tool is damagable and is destroyed upon being used up, then destroy it
					if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getDamageValue() >= itemstack.getMaxDamage())
						itemstack.setCount(itemstack.getCount() - 1);
					
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.FAIL;
	}
	
	@Override
	public String toString()
	{
		return type == null ? getDescriptionId() : type.name;
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}
}
