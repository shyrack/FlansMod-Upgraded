package com.flansmod.common.enchantments;

import java.util.ArrayList;
import java.util.List;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.FireableGun;

import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class EnchantmentModule 
{
	public static Holder<Enchantment> STEADY_ENCHANT,
								NIMBLE_ENCHANT,
								LUMBERJACK_ENCHANT,
								DUELIST_ENCHANT,
								SHARPSHOOTER_ENCHANT,
								JUGGERNAUT_ENCHANT;
	
	public EnchantmentModule()
	{
		
	}
	
	public void PreInit()
	{
		
	}
	
	public void Init()
	{
		DynamicRegistrySetupCallback.EVENT.register(view ->
		{
			view.getOptional(Registries.ENCHANTMENT).ifPresent(registry ->
			{
				STEADY_ENCHANT = register(registry, "enchantment_steady", EnchantmentSteady.build(id("enchantment_steady"), offHandItems()));
				NIMBLE_ENCHANT = register(registry, "enchantment_nimble", EnchantmentNimble.build(id("enchantment_nimble"), gloveItems()));
				LUMBERJACK_ENCHANT = register(registry, "enchantment_lumberjack", EnchantmentLumberjack.build(id("enchantment_lumberjack"), offHandItems()));
				DUELIST_ENCHANT = register(registry, "enchantment_duelist", EnchantmentDuelist.build(id("enchantment_duelist"), offHandItems()));
				SHARPSHOOTER_ENCHANT = register(registry, "enchantment_sharpshooter", EnchantmentSharpshooter.build(id("enchantment_sharpshooter"), offHandItems()));
				JUGGERNAUT_ENCHANT = register(registry, "enchantment_juggernaut", EnchantmentJuggernaut.build(id("enchantment_juggernaut"), armourItems()));
			});
		});
	}
	
	private static Identifier id(String name)
	{
		return Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, name);
	}
	
	private static Holder<Enchantment> register(Registry<Enchantment> registry, String name, Enchantment enchantment)
	{
		ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id(name));
		return Registry.registerForHolder(registry, key, enchantment);
	}
	
	private static HolderSet<Item> offHandItems()
	{
		List<Item> items = new ArrayList<>();
		items.add(Items.SHIELD);
		for(GloveType glove : GloveType.gloves)
		{
			if(glove.item != null)
				items.add(glove.item);
		}
		return HolderSet.direct(BuiltInRegistries.ITEM::wrapAsHolder, items);
	}
	
	private static HolderSet<Item> gloveItems()
	{
		List<Item> items = new ArrayList<>();
		for(GloveType glove : GloveType.gloves)
		{
			if(glove.item != null)
				items.add(glove.item);
		}
		if(items.isEmpty())
			items.add(Items.SHIELD);
		return HolderSet.direct(BuiltInRegistries.ITEM::wrapAsHolder, items);
	}
	
	private static HolderSet<Item> armourItems()
	{
		return HolderSet.direct(BuiltInRegistries.ITEM::wrapAsHolder,
				Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
	}
	
	public void PostInit()
	{
		
	}
	
	public static void ModifyGun(FireableGun fireableGun, LivingEntity entity, ItemStack otherHand) 
	{
		if(!FlansMod.enchantmentModuleEnabled)
			return;
		
		int steadyLevel = EnchantmentHelper.getItemEnchantmentLevel(STEADY_ENCHANT, otherHand);
		// Cut 25% of spread for each level of Steady on the glove (multiplicative)
		for(int i = 0; i < steadyLevel; i++)
			fireableGun.MultiplySpread(0.75f);

		
		int sharpshooterLevel = EnchantmentHelper.getItemEnchantmentLevel(SHARPSHOOTER_ENCHANT, otherHand);
		// Add 10% damage for each level of Sharpshooter on the glove (multiplicative)
		for(int i = 0; i < sharpshooterLevel; i++)
			fireableGun.MultiplyDamage(1.10f);		
		
		if(steadyLevel > 0 || sharpshooterLevel > 0)
			otherHand.hurtAndBreak(1, entity, EquipmentSlot.OFFHAND);
	}

	public static float ModifyReloadTime(float reloadTime, LivingEntity entity, ItemStack otherHand) 
	{
		if(!FlansMod.enchantmentModuleEnabled)
			return reloadTime;
		
		int nimbleLevel = EnchantmentHelper.getItemEnchantmentLevel(NIMBLE_ENCHANT, otherHand);
		// Cut 15% of reload time for each level of Nimble on the glove (multiplicative)
		for(int i = 0; i < nimbleLevel; i++)
			reloadTime *= 0.85f;
		if(nimbleLevel > 0)
			otherHand.hurtAndBreak(1, entity, EquipmentSlot.OFFHAND);
		
		return reloadTime;
	}

	/**
	 * Applies the melee and armour enchantment effects to incoming damage.
	 * Kept as a static helper: modern Minecraft has no Forge-style damage event,
	 * so this must be invoked from wherever the damage is applied.
	 */
	public static void modifyDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, org.apache.commons.lang3.mutable.MutableFloat amount)
	{
		if(!FlansMod.enchantmentModuleEnabled)
			return;
		
		Entity trueSource = source.getDirectEntity();
		if(trueSource != null && trueSource instanceof LivingEntity)
		{
			LivingEntity attacker = (LivingEntity)trueSource;
			ItemStack weaponStack = attacker.getMainHandItem();
			ItemStack offHandStack = attacker.getOffhandItem();
			
			// Apply lumberjack offhand effect
			if(weaponStack.getItem() instanceof AxeItem)
			{
				int lumberjackLevel = EnchantmentHelper.getItemEnchantmentLevel(LUMBERJACK_ENCHANT, offHandStack);
				// Add 10% damage for each level of Lumberjack on the glove (multiplicative)
				for(int i = 0; i < lumberjackLevel; i++)
					amount.setValue(amount.floatValue() * 1.10f);
				
				if(lumberjackLevel > 0)
					offHandStack.hurtAndBreak(1, attacker, EquipmentSlot.OFFHAND);
			}
			// Apply duelist offhand effect
			if(weaponStack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.SWORDS))
			{
				int duelistLevel = EnchantmentHelper.getItemEnchantmentLevel(DUELIST_ENCHANT, offHandStack);
				// Add 10% damage for each level of Duelist on the glove (multiplicative)
				for(int i = 0; i < duelistLevel; i++)
					amount.setValue(amount.floatValue() * 1.10f);
				
				if(duelistLevel > 0)
					offHandStack.hurtAndBreak(1, attacker, EquipmentSlot.OFFHAND);
			}
			
			// Then apply juggernaut effects
			int juggernautLevel = 0;
			for(EquipmentSlot slot : ARMOUR_SLOTS)
			{
				juggernautLevel += EnchantmentHelper.getItemEnchantmentLevel(JUGGERNAUT_ENCHANT, entity.getItemBySlot(slot));
			}
			
			if(juggernautLevel > 0)
			{
				final float minPercent = 0.25f; // With all 4 armour pieces enchanted, we drop to 25% max damage per hit
				final float exponent = (float)Math.log(minPercent) / 4f; // 4 because that's the theoretical max level of the enchant
				
				float maxDamageAsPercentOfHP = (float)Math.exp(exponent * juggernautLevel);
				
				float maxHP = entity.getMaxHealth() + entity.getArmorValue();
				
				if(amount.floatValue() > maxHP * maxDamageAsPercentOfHP)
				{
					float absorbedDmg = amount.floatValue() - maxHP * maxDamageAsPercentOfHP;
					// Don't want to just annihalate the armour in edge cases. That would be :(
					if(absorbedDmg > 256.0f)
						absorbedDmg = 256.0f;
					for(EquipmentSlot slot : ARMOUR_SLOTS)
					{
						ItemStack armour = entity.getItemBySlot(slot);
						if(EnchantmentHelper.getItemEnchantmentLevel(JUGGERNAUT_ENCHANT, armour) > 0)
						{
							armour.hurtAndBreak(Mth.floor(absorbedDmg), entity, slot);
						}
					}
					
					
					FlansMod.log.info("Juggernaut applied to incoming damage of " + amount.floatValue() + " over the threshold of " + (maxHP * maxDamageAsPercentOfHP));
					amount.setValue(maxHP * maxDamageAsPercentOfHP);
				}
			}
		}
	}
	
	private static final EquipmentSlot[] ARMOUR_SLOTS = new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};
}
