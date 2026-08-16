package com.flansmod.apocalypse.common.entity;

import java.util.Optional;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.common.FlansMod;
import com.flansmod.common.ModEntities;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.EntityGrenade;
import com.flansmod.common.guns.EnumFireMode;
import com.flansmod.common.guns.EnumSpreadPattern;
import com.flansmod.common.guns.FireableGun;
import com.flansmod.common.guns.FiredShot;
import com.flansmod.common.guns.GrenadeType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGrenade;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ItemShootable;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.ShotHandler;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.vector.Vector3f;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class EntitySkullDrone extends Mob implements Container
{
	protected Level world;
	protected static final EntityDataAccessor<Integer> LOOKING_AT_ENTITY = SynchedEntityData.defineId(EntitySkullDrone.class, EntityDataSerializers.INT);
	   
	private float shootDelay = 0;
	private int soundDelay = 0;

	private Vector3f offsetFromTarget = new Vector3f();
	
	public GunAnimations animations;
	
	public EntitySkullDrone(EntityType<? extends Mob> type, Level world)
	{
		super(type, world);
		this.world = level();
	}

	public EntitySkullDrone(Level worldIn)
	{
		this(ModEntities.SKULL_DRONE, worldIn);
		this.world = level();

		setNoGravity(true);
		setPersistenceRequired();
		setNoAi(true);	
		if(worldIn.isClientSide())
			initAnimations();
	}
	
	public static AttributeSupplier.Builder createAttributes()
	{
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0D);
	}
	
	private void initAnimations()
	{
		animations = new GunAnimations();
	}
	
	private void updateClient()
	{
		animations.update();
	}
	
	@Override 
	public void tick()
	{
		super.tick();
		
		if(world.isClientSide())
			updateClient();
		this.fallDistance = 0f;
		
		int entityID = entityData.get(LOOKING_AT_ENTITY);
		Entity target = world.getEntity(entityID);
		
		if(target != null)
		{
			if(target.isRemoved())
			{
				SetTarget(null);
			}
			else if(random.nextInt(1200) == 0)
			{
				SetTarget(null);
			}
			else
			{
				double dX = target.getX() - getX();
				double dY = target.getY() - getY();
				double dZ = target.getZ() - getZ();
				
				if(!world.isClientSide())
				{
					// Position code
					if(random.nextInt(60) == 0)
					{
						offsetFromTarget.scale(0.5f);
						offsetFromTarget.x += random.nextGaussian() * 10f;
						offsetFromTarget.z += random.nextGaussian() * 10f;
						offsetFromTarget.y = random.nextFloat() * 5.0f + 5.0f;		
					}
					
					this.setDeltaMovement(((target.getX() + offsetFromTarget.x) - getX()) * 0.06f, this.getDeltaMovement().y, this.getDeltaMovement().z);
					this.setDeltaMovement(this.getDeltaMovement().x, ((target.getY() + offsetFromTarget.y) - getY()) * 0.06f, this.getDeltaMovement().z);
					this.setDeltaMovement(this.getDeltaMovement().x, this.getDeltaMovement().y, ((target.getZ() + offsetFromTarget.z) - getZ()) * 0.06f);
					
					this.move(MoverType.SELF, this.getDeltaMovement());
				}
				
				// Look at code
				float targetYaw = (float)(Math.atan2(dZ, dX) * 180d / Math.PI);
				float targetPitch = (float)(Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ)) * 180d / Math.PI);
				
				float deltaYaw = targetYaw - getYRot();
				float deltaPitch = targetPitch - getXRot();
				
				while(deltaYaw > 180f)
					deltaYaw -= 360f;
				while(deltaYaw < -180f)
					deltaYaw += 360f;
				
				setYRot(getYRot() + deltaYaw / 2f);
				setXRot(getXRot() + deltaPitch / 2f);
				
				if(!world.isClientSide())// && deltaYaw < 1f && deltaPitch < 1f)
				{
					shootDelay--;
					UseGun();
				}
			}
		}
		else if(random.nextInt(1200) == 0)
		{
			Entity closestPlayer = world.getNearestPlayer(getX(), getY(), getZ(), 100.0d, false);
			if(closestPlayer != null)
			{
				SetTarget(closestPlayer);
			}
		}
	}
	
	private void UseGun()
	{
		ItemStack gunStack = getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		if(!gunStack.isEmpty() && gunStack.getItem() instanceof ItemGun)
		{
			ItemGun gunItem = (ItemGun)gunStack.getItem();
			GunType gunType = gunItem.GetType();
			
			//If we can shoot
			if(shootDelay <= 0)
			{
				//Go through the bullet stacks in the gun and see if any of them are not null
				int bulletID = 0;
				ItemStack ammoStackInGun = null;
				for(; bulletID < gunType.numAmmoItemsInGun; bulletID++)
				{
					ItemStack checkingStack = gunItem.getBulletItemStack(gunStack, bulletID);
					if(checkingStack != null && !checkingStack.isEmpty() && checkingStack.getDamageValue() < checkingStack.getMaxDamage())
					{
						ammoStackInGun = checkingStack;
						break;
					}
				}
				
				//If no bullet stack was found, reload
				if(ammoStackInGun == null || ammoStackInGun.isEmpty())
				{
					gunItem.Reload(gunStack, world, this, this, InteractionHand.MAIN_HAND, true, true, true);
					
					if(gunType.reloadSound != null)
						PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), gunType.reloadSound, gunType.distortSound);
					
					shootDelay = gunType.reloadTime * 3f;
				}
				//A bullet stack was found, so try shooting with it
				else if(ammoStackInGun.getItem() instanceof ItemBullet || ammoStackInGun.getItem() instanceof ItemGrenade)
				{
					//Shoot
					DoShoot(gunStack, gunType, ammoStackInGun, true, true);
					
					//Apply animations to 3D modelled guns
					//TODO this doesn't work
					if(world.isClientSide())
						animations.doShoot(gunType.getPumpDelay(), gunType.getPumpTime());
					
					//Damage the bullet item
					ammoStackInGun.setDamageValue(ammoStackInGun.getDamageValue() + 1);
					
					//Update the stack in the gun
					gunItem.setBulletItemStack(gunStack, ammoStackInGun, bulletID);
				}
			}
		}
	}
	
	private void DoShoot(ItemStack stack, GunType gunType, ItemStack bulletStack, boolean creative, boolean left)
	{
		int entityID = entityData.get(LOOKING_AT_ENTITY);
		Entity target = world.getEntity(entityID);
		if(target == null)
			return;
		ShootableType bulletType = ((ItemShootable)bulletStack.getItem()).type;		
		Vector3f bulletOrigin = new Vector3f(getX(), getY() - 1f, getZ());
		Vector3f aimVector = new Vector3f(target.getX() - getX(), target.getY() - (getY() - 1f), target.getZ() - getZ());
		
		if(!world.isClientSide())
		{
			ShootableType shootableType = ((ItemShootable)bulletStack.getItem()).type;
			if (shootableType instanceof BulletType)
			{
				FireableGun fireableGun = new FireableGun(gunType, gunType.getDamage(stack), gunType.getSpread(stack) * 5f + 10f, gunType.getBulletSpeed(stack), EnumSpreadPattern.circle);
				FiredShot shot = new FiredShot(fireableGun, (BulletType)shootableType, this);
				ShotHandler.fireGun(world, shot, gunType.numBullets*bulletType.numBullets, bulletOrigin, aimVector);
			}
			else if (shootableType instanceof GrenadeType)
			{
				double yaw = Math.atan2(aimVector.z, aimVector.x);
				double pitch = Math.atan2(Math.sqrt(aimVector.z * aimVector.z + aimVector.x * aimVector.x), aimVector.y) - Math.PI/2;
				Optional<Entity> ent = Optional.of(this);
				Optional<Player> player = Optional.ofNullable(null);
				
				EntityGrenade grenade = new EntityGrenade(world, bulletOrigin, (GrenadeType) shootableType, (float)Math.toDegrees(pitch), (float)Math.toDegrees(yaw + Math.PI*1.5), player, ent);
				((ServerLevel)world).addFreshEntity(grenade);
			}
		}
		
		shootDelay = gunType.mode == EnumFireMode.SEMIAUTO ? Math.max(gunType.GetShootDelay(stack), 5) : gunType.GetShootDelay(stack);
				
		// Play a sound if the previous sound has finished
		if(soundDelay <= 0 && gunType.shootSound != null)
		{
			PacketPlaySound.sendSoundPacket(getX(), getY(), getZ(), FlansMod.soundRange, com.flansmod.common.guns.GunUtil.getDimensionId(world), gunType.shootSound, gunType.distortSound);
			soundDelay = gunType.shootSoundLength;
		}
	}
	
	public void SetTarget(Entity entity)
	{
		entityData.set(LOOKING_AT_ENTITY, entity == null ? 0 : entity.getId());
		
		offsetFromTarget.x += random.nextGaussian() * 5f;
		offsetFromTarget.z += random.nextGaussian() * 5f;
		offsetFromTarget.y = 10.0f;		
	}
	
	@Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount)
    {
		if(source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
			return false;
		if(source.getEntity() instanceof EntitySkullBoss 
		|| source.getEntity() instanceof EntitySkullDrone)
			return false;
		
		super.hurtServer(level, source, amount);
		if(!world.isClientSide())
		{
			Entity sourceEntity = source.getEntity();
			if(sourceEntity != null)
				entityData.set(LOOKING_AT_ENTITY, sourceEntity.getId());
		}
		return true;
    }
	
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) 
	{
		super.defineSynchedData(builder);
		builder.define(LOOKING_AT_ENTITY, 0);
	}
	
	@Override
	public void readAdditionalSaveData(ValueInput input) 
	{
		super.readAdditionalSaveData(input);
		entityData.set(LOOKING_AT_ENTITY, input.getIntOr("LookingAt", 0));
		String itemName = input.getStringOr("BulletStackItem", "");
		if(!itemName.isEmpty())
		{
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
			bulletStack = new ItemStack(item, input.getIntOr("BulletStackCount", 1));
		}
		// TODO APOCALYPSE: item NBT components are not saved
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) 
	{
		super.addAdditionalSaveData(output);
		output.putInt("LookingAt", entityData.get(LOOKING_AT_ENTITY));
		if(!bulletStack.isEmpty())
		{
			output.putString("BulletStackItem", BuiltInRegistries.ITEM.getKey(bulletStack.getItem()).toString());
			output.putInt("BulletStackCount", bulletStack.getCount());
		}
	}
	
	// Container
	private ItemStack bulletStack = ItemStack.EMPTY;
	
	@Override
	public int getContainerSize()  { return 1; }
	@Override
	public boolean isEmpty() { return bulletStack.isEmpty(); }
	@Override
	public ItemStack getItem(int index) { return bulletStack; }
	@Override
	public ItemStack removeItem(int index, int count) { bulletStack.setCount(bulletStack.getCount() - count); return bulletStack; }
	@Override
	public ItemStack removeItemNoUpdate(int index) { ItemStack temp = bulletStack; bulletStack = ItemStack.EMPTY; return temp; }
	@Override
	public void setItem(int index, ItemStack stack) { bulletStack = stack;	}
	@Override
	public void setChanged() {}
	@Override
	public boolean stillValid(Player player) { return false; }
	@Override
	public void clearContent() { bulletStack = ItemStack.EMPTY; }
}
