package com.flansmod.apocalypse.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.FireableGun;
import com.flansmod.common.guns.FiredShot;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.ShotHandler;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.vector.Vector3f;

// TODO APOCALYPSE: 1.12.2 extended AbstractSkeleton; 26.1.2 has a package-private abstract
// getStepSound() there that cannot be implemented outside its package, so Monster is used instead
public class EntityFlansModShooter extends Monster implements RangedAttackMob
{
	protected Level world;
	
	public class EntityAIAttackRangedGun extends RangedBowAttackGoal<EntityFlansModShooter>
	{
		private EntityFlansModShooter entity;
		
		public EntityAIAttackRangedGun(EntityFlansModShooter mob, double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
			super(mob, moveSpeedAmpIn, attackCooldownIn, maxAttackDistanceIn);
			entity = mob;
		}
		
		@Override
	    protected boolean isHoldingBow()
	    {
	        return !entity.getMainHandItem().isEmpty() 
	        	&& entity.getMainHandItem().getItem() instanceof ItemGun;
	    }

	}
	
	private EntityAIAttackRangedGun shooterShoot;
    private MeleeAttackGoal shooterMelee;
	public ItemStack[] ammoStacks;
	public float shootDelay = 0;
	public float minigunSpeed = 0.0F;
	public int loopedSoundDelay = 0;
	public boolean reloading = false;
	public boolean shouldPlayWarmupSound = true;
	private int soundDelay = 0;
	
	public EntityFlansModShooter(EntityType<? extends Monster> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

public EntityFlansModShooter(Level world)
	{
		this(ModEntities.FLANSMOD_SHOOTER, world);
		ammoStacks = new ItemStack[0];

		this.setPersistenceRequired();
	}
	
	public static AttributeSupplier.Builder createAttributes()
	{
		return Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 80D)
				.add(Attributes.MOVEMENT_SPEED, 0.25D);
	}
	
    @Override
    protected void registerGoals()
    {
        super.registerGoals();
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, Animal.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, EntitySkullDrone.class, true));
    }
	
	@Override
	public void tick()
	{
		super.tick();
		if(shootDelay > 0)
			shootDelay--;
	}
	
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data)
	{
		data = super.finalizeSpawn(level, difficulty, reason, data);

		this.reassessWeaponGoal();
		this.setCanPickUpLoot(this.random.nextFloat() < 0.55F * difficulty.getSpecialMultiplier());

		return data;
	}

    public void reassessWeaponGoal()
    {
		if(shooterShoot == null || shooterMelee == null)
		{
			shooterShoot = new EntityAIAttackRangedGun(this, 1.0D, 20, 15.0F);
			shooterMelee = new MeleeAttackGoal(this, 1.2D, false);
		}
		
        if (this.world != null && !this.world.isClientSide())
        {
            this.goalSelector.removeGoal(this.shooterMelee);
            this.goalSelector.removeGoal(this.shooterShoot);
            ItemStack itemstack = this.getMainHandItem();

            if (itemstack.getItem() instanceof ItemGun)
            {
                int i = 10;

                if (this.world.getDifficulty() != Difficulty.HARD)
                {
                    i = 20;
                }

                this.shooterShoot.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.shooterShoot);
            }
            else
            {
                this.goalSelector.addGoal(4, this.shooterMelee);
            }
        }
    }
	
	public void setCombatTask()
	{
		reassessWeaponGoal();
	}

	@Override
	public void performRangedAttack(LivingEntity entity, float range)
	{
		ItemStack stack = getMainHandItem();
		if(stack != null && stack.getItem() instanceof ItemGun)
		{
			ItemGun item = (ItemGun)stack.getItem();
			GunType type = item.GetType();
			boolean shouldShoot = false;
			switch(type.mode)
			{
				case MINIGUN:
					shouldShoot = minigunSpeed >= type.minigunStartSpeed && shootDelay <= 0;
					break;
				case BURST:
				case FULLAUTO:
				case SEMIAUTO:
					shouldShoot = shootDelay <= 0;
					break;
			}
			
			if(type.useLoopingSounds && loopedSoundDelay <= 0 && minigunSpeed > 0.1F && !reloading)
			{
				loopedSoundDelay = shouldPlayWarmupSound ? type.warmupSoundLength : type.loopedSoundLength;
				PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), shouldPlayWarmupSound ? type.warmupSound : type.loopedSound, false);
				shouldPlayWarmupSound = false;
			}
			
			if(shouldShoot)
			{
				int damage = 0;
				//Check all gun's slots for a valid bullet to shoot
				int bulletID = 0;
				ItemStack bulletStack = ItemStack.EMPTY.copy();
				for(; bulletID < type.numAmmoItemsInGun; bulletID++)
				{
					ItemStack checkingStack = item.getBulletItemStack(stack, bulletID);
					if(checkingStack != null && !checkingStack.isEmpty() && checkingStack.getDamageValue() < checkingStack.getMaxDamage())
					{
						bulletStack = checkingStack;
						break;
					}
				}
				
				//If no bullet stack was found, reload
				if(bulletStack == null || bulletStack.isEmpty())
				{
					if(reload(stack, type, world, this, false, false))
					{
						//Set player shoot delay to be the reload delay
						//Set both gun delays to avoid reloading two guns at once
						shootDelay = (int)type.getReloadTime(stack);
						
						reloading = true;
						
						//Play reload sound
						if(type.reloadSound != null)
							PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.reloadSound, true);
					}
				}
				//A bullet stack was found, so try shooting with it
				else if(bulletStack.getItem() instanceof ItemShootable)
				{
					//Shoot
					shoot(stack, type, world, bulletStack, this, false, entity);
					//Damage the bullet item
					damage = bulletStack.getDamageValue() + 1;
					bulletStack.setDamageValue(damage);
					
					//Update the stack in the gun
					item.setBulletItemStack(stack, bulletStack, bulletID);
				}
				
				switch(type.mode)
				{
					case FULLAUTO: 
					case MINIGUN:
					{
						shootDelay = type.GetShootDelay(stack);
						break;
					}
					case SEMIAUTO:
					{
						shootDelay = 2 * type.GetShootDelay(stack);
						break;
					}
					case BURST:
					{
						shootDelay = (damage % 3 == 0 ? 3 * type.GetShootDelay(stack) : type.GetShootDelay(stack));
						break;
					}
				}

			}
		}
	}
	
	/**
	 * Reload method. Called automatically when firing with an empty clip
	 */
	public boolean reload(ItemStack gunStack, GunType gunType, Level world, Entity entity, boolean creative, boolean forceReload)
	{
		ItemGun item = ((ItemGun)gunType.item);
		//Deployable guns cannot be reloaded in the inventory
		if(gunType.deployable)
			return false;
		//If you cannot reload half way through a clip, reject the player for trying to do so
		if(forceReload && !gunType.canForceReload)
			return false;
		//For playing sounds afterwards
		boolean reloadedSomething = false;
		//Check each ammo slot, one at a time
		for(int i = 0; i < gunType.numAmmoItemsInGun; i++)
		{
			//Get the stack in the slot
			ItemStack bulletStack = item.getBulletItemStack(gunStack, i);
			
			//If there is no magazine, if the magazine is empty or if this is a forced reload
			if(bulletStack == null || bulletStack.isEmpty() || bulletStack.getDamageValue() == bulletStack.getMaxDamage() || forceReload)
			{
				//Iterate over all inventory slots and find the magazine / bullet item with the most bullets
				int bestSlot = -1;
				int bulletsInBestSlot = 0;
				for(int j = 0; j < ammoStacks.length; j++)
				{
					ItemStack searchingStack = ammoStacks[j];
					if(searchingStack != null && searchingStack.getItem() instanceof ItemShootable && gunType.isCorrectAmmo(((ItemShootable)(searchingStack.getItem())).type))
					{
						int bulletsInThisSlot = searchingStack.getMaxDamage() - searchingStack.getDamageValue();
						if(bulletsInThisSlot > bulletsInBestSlot)
						{
							bestSlot = j;
							bulletsInBestSlot = bulletsInThisSlot;
						}
					}
				}
				//If there was a valid non-empty magazine / bullet item somewhere in the inventory, load it
				if(bestSlot != -1)
				{
					ItemStack newBulletStack = ammoStacks[bestSlot];
					ShootableType newBulletType = ((ItemShootable)newBulletStack.getItem()).type;
					//Unload the old magazine (Drop an item if it is required and the player is not in creative mode)
					if(bulletStack != null && bulletStack.getItem() instanceof ItemShootable && ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload != null && !creative)
						item.dropItem(world, this, ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload);
					
					//Load the new magazine
					ItemStack stackToLoad = newBulletStack.copy();
					stackToLoad.setCount(1);
					item.setBulletItemStack(gunStack, stackToLoad, i);
					
					//Remove the magazine from the inventory
					if(!creative)
						newBulletStack.setCount(newBulletStack.getCount() - 1);
					if(newBulletStack.getCount() <= 0)
						newBulletStack = null;
					ammoStacks[bestSlot] = newBulletStack;

					
					//Tell the sound player that we reloaded something
					reloadedSomething = true;
				}
			}
		}
		return reloadedSomething;
	}
	
	/**
	 * Method for shooting to avoid repeated code
	 */
	private void shoot(ItemStack stack, GunType gunType, Level world, ItemStack bulletStack, Entity entity, boolean left, LivingEntity target)
	{
		ShootableType bullet = ((ItemShootable)bulletStack.getItem()).type;
		// Play a sound if the previous sound has finished
		if(soundDelay <= 0 && gunType.shootSound != null)
		{
			AttachmentType barrel = gunType.getBarrel(stack);
			boolean silenced = barrel != null && barrel.silencer;
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), gunType.shootSound, gunType.distortSound, silenced);
			soundDelay = gunType.shootSoundLength;
		}
		if(!world.isClientSide())
		{
			float inaccuracy = 0.5F;
			
			// Spawn the bullet entities
			Vector3f origin = new Vector3f(getX(), getY() + getEyeHeight(), getZ());
			Vector3f direction = new Vector3f(target.getX() - getX(), (target.getY() + target.getEyeHeight()) - (getY() + getEyeHeight()), target.getZ() - getZ()).normalise(null);
			Vector3f.add(direction, new Vector3f(random.nextFloat() * direction.x * inaccuracy, random.nextFloat() * direction.y * inaccuracy, random.nextFloat() * direction.z * inaccuracy), direction);
			
			FireableGun fireableGun = new FireableGun(gunType, gunType.getDamage(stack), gunType.getSpread(stack), gunType.getBulletSpeed(stack), gunType.getSpreadPattern(stack));
			
			//Grenades are currently disabled for this entity
			if (bullet instanceof BulletType)
			{
				FiredShot shot = new FiredShot(fireableGun, (BulletType)bullet, this);
			
				ShotHandler.fireGun(world, shot, gunType.numBullets*bullet.numBullets, origin, direction);
			}
			// Drop item on shooting if bullet requires it
			if(bullet.dropItemOnShoot != null)
				ItemGun.dropItem(world, this, bullet.dropItemOnShoot);
			// Drop item on shooting if gun requires it
			if(gunType.dropItemOnShoot != null)
				ItemGun.dropItem(world, this, gunType.dropItemOnShoot);
		}
		shootDelay = gunType.GetShootDelay(stack);
	}
	
	@Override
	public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason)
	{
		return level.getDifficulty() != Difficulty.PEACEFUL;
	}

	// TODO APOCALYPSE: 1.12.2 overrode getCreatureAttribute/getBrightness/isValidLightLevel/getStepSound/setSwingingArms;
	// these hooks no longer exist in 26.1.2 and were dropped
}
