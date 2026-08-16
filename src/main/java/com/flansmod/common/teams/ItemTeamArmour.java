package com.flansmod.common.teams;

import com.flansmod.common.ModItems;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

import com.flansmod.common.FlansMod;
import com.flansmod.common.types.IFlanItem;
import com.flansmod.common.types.InfoType;

public class ItemTeamArmour extends Item implements IFlanItem
{
	public ArmourType type;
	
	protected static final Identifier KNOCKBACK_RESIST_MODIFIER = Identifier.fromNamespaceAndPath("flansmod", "armour_knockback_resist");
	protected static final Identifier MOVEMENT_SPEED_MODIFIER = Identifier.fromNamespaceAndPath("flansmod", "armour_movement_speed");
	
	public ItemTeamArmour(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemTeamArmour(ArmourType t)
	{
		super(buildProperties(t).setId(ModItems.itemKey(t)));
		type = t;
		type.item = this;
	}
	
	public Item setTranslationKey(String key)
	{
		return this;
	}
	
	private static Item.Properties buildProperties(ArmourType t)
	{
		Item.Properties properties = new Item.Properties().stacksTo(1)
			.equippable(getEquipmentSlotForType(t.type));
		if(t.Enchantability > 0)
			properties = properties.enchantable(t.Enchantability);
		if(t.Durability > 0)
			properties = properties.durability(t.Durability);
		ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
			.withModifierAdded(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KNOCKBACK_RESIST_MODIFIER, t.knockbackModifier, AttributeModifier.Operation.ADD_VALUE), getSlotGroupForType(t.type))
			.withModifierAdded(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_SPEED_MODIFIER, t.moveSpeedModifier - 1.0f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), getSlotGroupForType(t.type));
		return properties.component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
	}
	
	public static EquipmentSlot getEquipmentSlotForType(int type)
	{
		switch(type)
		{
			case 0: return EquipmentSlot.HEAD;
			case 1: return EquipmentSlot.CHEST;
			case 2: return EquipmentSlot.LEGS;
			default: return EquipmentSlot.FEET;
		}
	}
	
	public static EquipmentSlotGroup getSlotGroupForType(int type)
	{
		switch(type)
		{
			case 0: return EquipmentSlotGroup.HEAD;
			case 1: return EquipmentSlotGroup.CHEST;
			case 2: return EquipmentSlotGroup.LEGS;
			default: return EquipmentSlotGroup.FEET;
		}
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(type.description != null)
		{
			for(String line : type.description.split("_"))
				tooltip.accept(Component.literal(line));
		}
		if(Math.abs(type.jumpModifier - 1F) > 0.01F)
			tooltip.accept(Component.literal("\u00a73+" + (int)((type.jumpModifier - 1F) * 100F) + "% Jump Height"));
		if(type.smokeProtection)
			tooltip.accept(Component.literal("\u00a72+Smoke Protection"));
		if(type.nightVision)
			tooltip.accept(Component.literal("\u00a72+Night Vision"));
		if(type.negateFallDamage)
			tooltip.accept(Component.literal("\u00a72+Negates Fall Damage"));
	}
	
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot)
	{
		if(entity instanceof Player player && slot == getEquipmentSlotForType(type.type))
		{
			if(type.nightVision && FlansMod.ticker % 25 == 0)
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 250)); // 16 = night vision
			if(type.jumpModifier > 1.01F && FlansMod.ticker % 25 == 0)
				player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 250, (int)((type.jumpModifier - 1F) * 2F), true, false)); // 8 = jump boost
			if(type.negateFallDamage)
				player.fallDistance = 0F;
		}
	}
	
	@Override
	public InfoType getInfoType()
	{
		return type;
	}
}
