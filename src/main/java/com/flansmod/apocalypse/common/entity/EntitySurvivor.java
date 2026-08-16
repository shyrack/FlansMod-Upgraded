package com.flansmod.apocalypse.common.entity;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.ModEntities;

public class EntitySurvivor extends EntityFlansModShooter
{
	public static ArrayList<GunType> validGuns;
	
	private final Random utilRandom = new Random();
	
	public EntitySurvivor(EntityType<? extends EntityFlansModShooter> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntitySurvivor(Level world)
	{
		this(ModEntities.SURVIVOR, world);
		this.world = level();
		if(!world.isClientSide())
		{
			if(validGuns == null)
				initGunList();
			
			//Pick a random gun for this survivor
			GunType gun = validGuns.get(world.getRandom().nextInt(validGuns.size()));
			ItemStack gunStack = FlansModApocalypse.getLootGenerator().loadAndPaintGun(gun, utilRandom);
			this.setItemSlot(EquipmentSlot.MAINHAND, gunStack);
			//Add random armour
			FlansModApocalypse.getLootGenerator().dressMeUp(this, utilRandom);
			
			ammoStacks = new ItemStack[5];
			int numAmmo = world.getRandom().nextInt(4) + 2;
			for(int i = 0; i < numAmmo; i++)
			{
				ShootableType type = gun.ammo.get(world.getRandom().nextInt(gun.ammo.size()));
				ammoStacks[i] = new ItemStack(type.item);
			}
		}
		//targetTasks.addTask(4, new NearestAttackableTargetGoal(this, EntityFlansModShooter.class, true));
		//tasks.addTask(5, new EntityAIGoSomewhere(this, 1.0D, world.rand.nextDouble() * 10D, world.rand.nextDouble() * 10D));

		setCombatTask();
		
		// TODO APOCALYPSE: 1.12.2 set inventoryArmorDropChances/inventoryHandsDropChances/experienceValue here; these fields no longer exist
	}
	
	/**
	 * Grab the list of guns valid for survivors from the complete gun list
	 */
	private void initGunList()
	{
		validGuns = new ArrayList<>();
		for(GunType gun : GunType.gunList)
		{
			if(gun.mode == EnumFireMode.SEMIAUTO && !gun.deployable && gun.ammo.size() > 0 && !gun.shield && gun.usableByPlayers && gun.dungeonChance != 0)
				validGuns.add(gun);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean maybeBlock)
	{
		int numFoods = utilRandom.nextInt(5);

		for(int i = 0; i < numFoods; i++)
		{
			switch(utilRandom.nextInt(5))
			{
				case 0: spawnAtLocation(level, Items.COOKED_BEEF);
					break;
				case 1: spawnAtLocation(level, Items.BREAD);
					break;
				case 2: spawnAtLocation(level, Items.MUSHROOM_STEW);
					break;
				case 3: spawnAtLocation(level, Items.COOKED_RABBIT);
					break;
				case 4: spawnAtLocation(level, Items.COOKED_CHICKEN);
					break;
			}
		}

		for(int i = 0; i < 5; i++)
		{
			if(ammoStacks[i] != null)
			{
				level.addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(), ammoStacks[i]));
			}
		}

		if(utilRandom.nextInt(5) == 0)
			spawnAtLocation(level, new ItemStack(Blocks.OAK_LOG.asItem(), utilRandom.nextInt(10) + 5));
		if(utilRandom.nextInt(12) == 0)
			spawnAtLocation(level, Items.BOOK);
		if(utilRandom.nextInt(12) == 0)
			spawnAtLocation(level, Items.FLINT_AND_STEEL);
		if(utilRandom.nextInt(40) == 0)
			spawnAtLocation(level, Items.IRON_AXE);
		if(utilRandom.nextInt(40) == 0)
			spawnAtLocation(level, Items.IRON_PICKAXE);
		if(utilRandom.nextInt(40) == 0)
			spawnAtLocation(level, Items.IRON_SHOVEL);
		if(utilRandom.nextInt(4) == 0)
			spawnAtLocation(level, new ItemStack(Blocks.TORCH.asItem(), utilRandom.nextInt(5) + 1));

		if(utilRandom.nextBoolean())
		{
			level.addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(), FlansModApocalypse.getLootGenerator().getSurvivorJournal(utilRandom)));
		}
	}
}
