package com.flansmod.common.guns;

import com.flansmod.common.ModItems;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.flansmod.client.FlansModClient;
import com.flansmod.client.debug.EntityDebugDot;
import com.flansmod.client.model.GunAnimations;
import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.util.FlansModUtil;
import com.flansmod.common.PlayerData;
import com.flansmod.common.PlayerHandler;
import com.flansmod.common.enchantments.EnchantmentModule;
import com.flansmod.common.enchantments.ItemGlove;
import com.flansmod.common.guns.raytracing.FlansModRaytracer;
import com.flansmod.common.network.PacketGunFire;
import com.flansmod.common.network.PacketPlaySound;
import com.flansmod.common.network.PacketReload;
import com.flansmod.common.paintjob.IPaintableItem;
import com.flansmod.common.paintjob.PaintableType;
import com.flansmod.common.teams.EntityFlag;
import com.flansmod.common.teams.EntityFlagpole;
import com.flansmod.common.teams.EntityGunItem;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.vector.Vector3f;

public class ItemGun extends Item implements IPaintableItem
{
	
	private GunType type;
	
	public GunType GetType()
	{
		return type;
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
	
	private int soundDelay = 0;
	
	private static boolean rightMouseHeld;
	private static boolean lastRightMouseHeld;
	private static boolean leftMouseHeld;
	private static boolean lastLeftMouseHeld;
	
	private static boolean GetMouseHeld(InteractionHand hand)
	{
		if(FlansMod.shootOnRightClick)
			return hand == InteractionHand.MAIN_HAND ? rightMouseHeld : leftMouseHeld;
		else
			return hand == InteractionHand.MAIN_HAND ? leftMouseHeld : rightMouseHeld;
	}
	
	private static boolean GetLastMouseHeld(InteractionHand hand)
	{
		if(FlansMod.shootOnRightClick)
			return hand == InteractionHand.MAIN_HAND ? lastRightMouseHeld : lastLeftMouseHeld;
		else
			return hand == InteractionHand.MAIN_HAND ? lastLeftMouseHeld : lastRightMouseHeld;
	}
	
	public ItemGun(Item.Properties properties)
	{
		super(properties);
	}
	
	public ItemGun(GunType type)
	{
		super(buildProperties(type));
		this.type = type;
		type.item = this;
	}

	private static Item.Properties buildProperties(GunType type)
	{
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, KNOCKBACK_RESIST_MODIFIER.toString()), type.knockbackModifier, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
		builder.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, MOVEMENT_SPEED_MODIFIER.toString()), type.moveSpeedModifier - 1.0f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
		builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, ATTACK_DAMAGE_MODIFIER.toString()), type.meleeDamage, AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
		return new Item.Properties().stacksTo(1).setId(ModItems.itemKey(type)).attributes(builder.build());
	}
	
	/**
	 * Get the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets)
	 */
	public ItemStack getBulletItemStack(ItemStack gun, int id)
	{
		//If the gun has no tags, give it some
		if(!GunUtil.hasTag(gun))
		{
			GunUtil.setTag(gun, new CompoundTag());
			return ItemStack.EMPTY.copy();
		}
		//If the gun has no ammo tags, give it some
		if(!GunUtil.getTag(gun).contains("ammo"))
		{
			ListTag ammoTagsList = new ListTag();
			for(int i = 0; i < type.numAmmoItemsInGun; i++)
			{
				ammoTagsList.add(new CompoundTag());
			}
			GunUtil.getTag(gun).put("ammo", ammoTagsList);
			return ItemStack.EMPTY.copy();
		}
		//Take the list of ammo tags
		ListTag ammoTagsList = GunUtil.getTag(gun).getListOrEmpty("ammo");
		//Get the specific ammo tags required
		CompoundTag ammoTags = ammoTagsList.getCompoundOrEmpty(id);
		return GunUtil.stackFromTag(ammoTags);
	}
	
	/**
	 * Set the bullet item stack stored in the gun's NBT data (the loaded magazine / bullets)
	 */
	public void setBulletItemStack(ItemStack gun, ItemStack bullet, int id)
	{
		//If the gun has no tags, give it some
		if(!GunUtil.hasTag(gun))
		{
			GunUtil.setTag(gun, new CompoundTag());
		}
		//If the gun has no ammo tags, give it some
		if(!GunUtil.getTag(gun).contains("ammo"))
		{
			ListTag ammoTagsList = new ListTag();
			for(int i = 0; i < type.numAmmoItemsInGun; i++)
			{
				ammoTagsList.add(new CompoundTag());
			}
			GunUtil.getTag(gun).put("ammo", ammoTagsList);
		}
		//Take the list of ammo tags
		ListTag ammoTagsList = GunUtil.getTag(gun).getListOrEmpty("ammo");
		//Get the specific ammo tags required
		CompoundTag ammoTags = ammoTagsList.getCompoundOrEmpty(id);
		//Represent empty slots by nulltypes
		if(bullet == null)
		{
			ammoTags = new CompoundTag();
		}
		//Set the tags to match the bullet stack
		GunUtil.stackToTag(ammoTags, bullet);
	}
	
	/**
	 * Method for dropping items on reload and on shoot
	 */
	public static void dropItem(Level world, Entity entity, String itemName)
	{
		if(itemName != null && !world.isClientSide())
		{
			ItemStack dropStack = InfoType.getRecipeElement(itemName);
			entity.spawnAtLocation((ServerLevel)world, dropStack, 0.5F);
		}
	}
	
	/**
	 * Deployable guns only
	 */
	@Override
	public InteractionResult use(Level world, Player entityplayer, InteractionHand hand)
	{
		ItemStack itemstack = entityplayer.getItemInHand(hand);
		
		if(type.deployable)
		{
			//Raytracing
			float cosYaw = Mth.cos(-entityplayer.getYRot() * 0.01745329F - 3.141593F);
			float sinYaw = Mth.sin(-entityplayer.getYRot() * 0.01745329F - 3.141593F);
			float cosPitch = -Mth.cos(-entityplayer.getXRot() * 0.01745329F);
			float sinPitch = Mth.sin(-entityplayer.getXRot() * 0.01745329F);
			double length = 5D;
			Vec3 posVec = new Vec3(entityplayer.getX(), entityplayer.getEyeY(), entityplayer.getZ());
			Vec3 lookVec = posVec.add(sinYaw * cosPitch * length, sinPitch * length, cosYaw * cosPitch * length);
			HitResult look = world.clip(new ClipContext(posVec, lookVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entityplayer));
			
			//Result check
			if(look != null && look.getType() == HitResult.Type.BLOCK)
			{
				BlockHitResult blockLook = (BlockHitResult)look;
				if(blockLook.getDirection() == Direction.UP)
				{
					int playerDir = Mth.floor(((entityplayer.getYRot() * 4F) / 360F) + 0.5D) & 3;
					int i = blockLook.getBlockPos().getX();
					int j = blockLook.getBlockPos().getY();
					int k = blockLook.getBlockPos().getZ();
					if(!world.isClientSide())
					{
						if(world.getBlockState(new BlockPos(i, j, k)).getBlock() == Blocks.SNOW)
						{
							j--;
						}
						if(isSolid(world, i, j, k) &&
								(world.getBlockState(new BlockPos(i, j + 1, k)).getBlock() == Blocks.AIR || world.getBlockState(new BlockPos(i, j + 1, k)).getBlock() == Blocks.SNOW)
								&&
								(world.getBlockState(new BlockPos(i + (playerDir == 1 ? 1 : 0) - (playerDir == 3 ? 1 : 0), j + 1, k - (playerDir == 0 ? 1 : 0) + (playerDir == 2 ? 1 : 0))).getBlock() == Blocks.AIR)
								&&
								(world.getBlockState(new BlockPos(i + (playerDir == 1 ? 1 : 0) - (playerDir == 3 ? 1 : 0), j, k - (playerDir == 0 ? 1 : 0) + (playerDir == 2 ? 1 : 0))).getBlock() == Blocks.AIR
										|| world.getBlockState(new BlockPos(i + (playerDir == 1 ? 1 : 0) - (playerDir == 3 ? 1 : 0), j, k - (playerDir == 0 ? 1 : 0) + (playerDir == 2 ? 1 : 0))).getBlock() == Blocks.SNOW))
						{
							for(EntityMG mg : EntityMG.mgs)
							{
								if(mg.blockX == i && mg.blockY == j + 1 && mg.blockZ == k && !mg.isRemoved())
									return InteractionResult.SUCCESS;
							}
							EntityMG mg = new EntityMG(world, i, j + 1, k, playerDir, type);
							
							if(getBulletItemStack(itemstack, 0) != null && !getBulletItemStack(itemstack, 0).isEmpty())
							{
								mg.ammo = getBulletItemStack(itemstack, 0);
							}
							((ServerLevel)world).addFreshEntity(mg);
							
							if(!entityplayer.getAbilities().instabuild)
								itemstack.setCount(0);
						}
					}
				}
			}
		}
		return InteractionResult.PASS;
	}
	
	// _____________________________________________________________________________
	//
	// Shooting code
	// _____________________________________________________________________________
	
	public void onUpdateClient(ItemStack gunstack, int gunSlot, Level world, Entity entity, InteractionHand hand, boolean hasOffHand)
	{
		if(!(entity instanceof Player))
		{
			//This code is for players only
			return;
		}
		
		// This code is not for deployables
		if(type.deployable)
			return;
		
		//Scope Handling
		IScope currentScope = type.getCurrentScope(gunstack);
		if(!hasOffHand)
		{
			switch(hand)
			{
				case MAIN_HAND:
				{
					if(GetMouseHeld(InteractionHand.OFF_HAND) && !GetLastMouseHeld(InteractionHand.OFF_HAND)
							&& (type.secondaryFunction == EnumSecondaryFunction.ADS_ZOOM || type.secondaryFunction == EnumSecondaryFunction.ZOOM))
					{
						FlansModClient.setScope(currentScope);
					}
					break;
				}
				case OFF_HAND:
				{
					if(GetMouseHeld(InteractionHand.MAIN_HAND) && !GetLastMouseHeld(InteractionHand.MAIN_HAND)
							&& (type.secondaryFunction == EnumSecondaryFunction.ADS_ZOOM || type.secondaryFunction == EnumSecondaryFunction.ZOOM))
					{
						FlansModClient.setScope(currentScope);
					}
					break;
				}
			}
		}
		
		// Get useful objects
		Minecraft mc = Minecraft.getInstance();
		Player player = (Player) entity;
		PlayerData data = PlayerHandler.getPlayerData(player);
		//Slow down minigun
		data.minigunSpeed *= 0.9f;
		Boolean hold = GetMouseHeld(hand);
		Boolean held = GetLastMouseHeld(hand);
		
		// Do not shoot ammo bags, flags or dropped gun items
		if(mc.hitResult != null && mc.hitResult instanceof EntityHitResult entityHitResult && (entityHitResult.getEntity() instanceof EntityFlagpole || entityHitResult.getEntity() instanceof EntityFlag || entityHitResult.getEntity() instanceof EntityGunItem || (entityHitResult.getEntity() instanceof EntityGrenade && ((EntityGrenade)entityHitResult.getEntity()).type.isDeployableBag)))
			hold = false;
		
		//TODO idle sound should be done on the server side
		// Play idle sounds
		if(soundDelay <= 0 && type.idleSound != null)
		{
			PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.idleSound, false);
			soundDelay = type.idleSoundLength;
		}
		
		if (!gunCanBeHandled(type, player))
			return;
		
		if(type.usableByPlayers)
		{
			GunAnimations animations = FlansModClient.getGunAnimations(player, hand);
			
			boolean needsToReload = needsToReload(gunstack);
			boolean shouldShootThisTick = false;
			switch(type.getFireMode(gunstack))
			{
				case BURST:
				{
					//PlayerData burst rounds handled on client
					if(data.GetBurstRoundsRemaining(hand) > 0)
					{
						shouldShootThisTick = true;
					}
					// Fallthrough to semi auto
				}
				case SEMIAUTO:
				{
					if(hold && !held)
					{
						shouldShootThisTick = true;
					}
					else needsToReload = false;
					break;
				}
				case MINIGUN:
				{
					if(needsToReload)
					{
						needsToReload = hold;
						break;
					}
					if(hold)
					{
						//PlayerData minigunspeed used on client side
						if (data.minigunSpeed < type.minigunMaxSpeed)
						{
							data.minigunSpeed += 2.0f;
							animations.addMinigunBarrelRotationSpeed(2.0f);
						}
						// TODO : Re-add looping sounds
						if(data.minigunSpeed < type.minigunStartSpeed)
						{							
							if(type.useLoopingSounds && data.loopedSoundDelay <= 0 && data.minigunSpeed > 0.1F && !data.reloadingRight && !data.isSpinning)
							{
								data.loopedSoundDelay = type.warmupSoundLength;
								PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.warmupSound, false);
								data.isSpinning = true;
							}
							break;
						}
					}
					
					//else fallthrough to full auto
				}
				case FULLAUTO:
				{
					shouldShootThisTick = hold;
					if(!shouldShootThisTick)
					{
						needsToReload = false;
					}
					
					//Play looping sounds for minigun
					if(type.useLoopingSounds && data.loopedSoundDelay <= 0 && data.minigunSpeed > type.minigunStartSpeed)
					{
						data.loopedSoundDelay = type.loopedSoundLength;
						PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.loopedSound, false);
						data.isSpinning = true; // isSpinning = true
					}
					if(type.useLoopingSounds && data.isSpinning && data.minigunSpeed < type.minigunStartSpeed)
					{
						PacketPlaySound.sendSoundPacket(player.getX(), player.getY(), player.getZ(), FlansMod.soundRange, GunUtil.getDimensionId(world), type.cooldownSound, false);
						data.isSpinning = false;
					}
					
					break;
				}
				default:
					needsToReload = false;
					break;
			}
			
			// Do reload if we pressed fire.
			if(needsToReload)
			{
				FlansMod.getPacketHandler().sendToServer(new PacketReload(hand, false));
			}
			// Fire!
			else if(shouldShootThisTick)
			{
				shoot(hand, player, gunstack, data, world, animations);
			}
		}
	}
	
	/**
	 * Used to determine if for example an player is holding a two handed gun but the other hand (the one without a gun) is holding something else
	 * For example a player is holding two miniguns, a gun requiring both hands, so this method returns true
	 * 
	 * @param type   The GunType of the gun
	 * @param player The player who is handling the gun
	 * @return if the player can handle the gun based on the contents of the main and off hand and the GunType
	 */
	public boolean gunCanBeHandled(GunType type, Player player)
	{
		// We can always use a 1H gun
		if(type.oneHanded)
			return true;
		
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		Boolean hasItemInBothHands = !main.isEmpty() && !off.isEmpty();
		if(hasItemInBothHands) 
		{
			// Gloves are special enchantable items that can be placed in the offhand while still letting you shoot 2H
			if(off.getItem() instanceof ItemGlove)
				return true;
			else
				return false;
		}
		
		return true;
	}
	
	public void shoot(InteractionHand hand, Player player, ItemStack gunstack, PlayerData data, Level world, GunAnimations animations)
	{
		if(type.usableByPlayers)
		{
			float shootTime = data.GetShootTime(hand);
			
			ItemStack otherHand = null;
			if(hand == InteractionHand.MAIN_HAND)
				otherHand = player.getOffhandItem();
			else otherHand = player.getMainHandItem();
			
			if (!world.isClientSide() && shootTime > 0f)
			{
				//data.addToQueue(hand);
				//Hacky code
				//This essentially skips ticks for a smoother client experience
				if (shootTime < 4)
				{
					while (shootTime > 0)
					{
						shootTime--;
					}
				}
			}
			
			if (world.isClientSide() && shootTime <= 0)
				//Send the server the instruction to shoot
				FlansMod.getPacketHandler().sendToServer(new PacketGunFire(hand));
			
			// For each 
			while(shootTime <= 0.0f)
			{
				
				// Add the delay for this shot and shoot it!
				shootTime += type.GetShootDelay(gunstack);
				
				int bulletID = 0;
				ItemStack bulletStack = ItemStack.EMPTY.copy();
				for(; bulletID < type.numAmmoItemsInGun; bulletID++)
				{
					ItemStack checkingStack = getBulletItemStack(gunstack, bulletID);
					if(checkingStack != null && checkingStack.getDamageValue() < checkingStack.getMaxDamage())
					{
						bulletStack = checkingStack;
						break;
					}
				}
				
				if(bulletStack.isEmpty())
				{
					continue;
				}
				
				final ItemStack bullet = bulletStack;
				final Integer bulletid = bulletID;
				
				ItemShootable shootableItem = (ItemShootable)bulletStack.getItem();
				ShootableType shootableType = shootableItem.type;
				Vector3f rayTraceOrigin = new Vector3f(player.getEyePosition(0.0f));
				
					ShootBulletHandler handler = isExtraBullet ->
					{
						if(!isExtraBullet)
						{
							// Drop item on shooting if bullet requires it
							if(shootableType.dropItemOnShoot != null && !player.getAbilities().instabuild)
								dropItem(world, player, shootableType.dropItemOnShoot);
							// Drop item on shooting if gun requires it
							if(type.dropItemOnShoot != null)// && !entityplayer.getAbilities().instabuild)
								dropItem(world, player, type.dropItemOnShoot);
							
							if(type.knockback > 0)
							{
							//TODO : Apply knockback		
							}
							
							//Damage the bullet item
							bullet.setDamageValue(bullet.getDamageValue() + 1);
							
							//Update the stack in the gun
							setBulletItemStack(gunstack, bullet, bulletid);
							
							if(type.consumeGunUponUse)
							{
								player.setItemInHand(hand, ItemStack.EMPTY.copy());
							}
						}
					};
					
					if (world.isClientSide())
					{	
						
						Integer bulletAmount = type.numBullets*shootableType.numBullets;
						for(int i = 0; i < bulletAmount; i++)
						{
							//Smooth effects, no need to wait for the server response
							handler.shooting(i < bulletAmount - 1);
						}
						
						animations.doShoot(type.getPumpDelay(), type.getPumpTime());
						Float recoil = type.getRecoil(gunstack);
						FlansModClient.playerRecoil += recoil;
						animations.recoil += recoil;
						
					} else
					{
						Vector3f rayTraceDirection = new Vector3f(player.getViewVector(1.0F));
						
						if (shootableType instanceof BulletType)
						{
							//Fire gun
							FireableGun fireableGun = 
									new FireableGun(type,
											type.getDamage(gunstack),
											type.getSpread(gunstack), 
											type.bulletSpeed, 
											type.getSpreadPattern(gunstack));
							
							if(otherHand.getItem() instanceof ShieldItem || otherHand.getItem() instanceof ItemGlove)
							{
								EnchantmentModule.ModifyGun(fireableGun, player, otherHand);
							}
							
							FiredShot shot = new FiredShot(fireableGun, (BulletType)shootableType, (ServerPlayer)player);
							//TODO gunOrigin? & animation origin
							ShotHandler.fireGun(world, shot, type.numBullets*shootableType.numBullets, rayTraceOrigin, rayTraceDirection, handler);
						}
						else if (shootableType instanceof GrenadeType)
						{
							//throw grenade
							ItemGrenade grenade = (ItemGrenade) shootableItem;
							grenade.throwGrenade(world, player);
							handler.shooting(false);
						}
						
						boolean silenced = type.getBarrel(gunstack) != null && type.getBarrel(gunstack).silencer;
						playShotSound(world, rayTraceOrigin, silenced);
					}
				int gunSlot = player.getInventory().getSelectedSlot();
				if(type.consumeGunUponUse)
					player.getInventory().setItem(gunSlot, ItemStack.EMPTY.copy());
			}
			data.SetShootTime(hand, shootTime);
		}
	}
	
	public void shootServer(InteractionHand hand, ServerPlayer player, ItemStack gunstack)
		{

			// Get useful objects
			PlayerData data = PlayerHandler.getPlayerData(player);
			Level world = player.level();
			
			// This code is not for deployables
			if(type.deployable)
				return;
			
			if (!gunCanBeHandled(type, player))
				return;
			
			shoot(hand, player, gunstack, data, world, null);
			
			if(FlansMod.DEBUG)
			{
				Vector3f gunOrigin = FlansModRaytracer.GetPlayerMuzzlePosition(player, hand);
				((ServerLevel)world).addFreshEntity(new EntityDebugDot(world, gunOrigin, 100, 1.0f, 1.0f, 1.0f));
			}
	}
	
	public void playShotSound(Level world, Vector3f position, Boolean silenced) {
		// Play shot sounds
		if(soundDelay <= 0 && type.shootSound != null)
		{
			PacketPlaySound.sendSoundPacket(position.x, position.y, position.z, FlansMod.soundRange, GunUtil.getDimensionId(world), type.shootSound, silenced);
			soundDelay = type.idleSoundLength;
		}
	}

	public void onUpdateServer(ItemStack itemstack, int gunSlot, Level world, Entity entity, InteractionHand hand, boolean hasOffHand)
	{
		if(!(entity instanceof ServerPlayer))
		{
			return;
		}
		ServerPlayer player = (ServerPlayer)entity;
		PlayerData data = PlayerHandler.getPlayerData(player);
		
		if(player.getInventory().getSelectedItem() != itemstack)
		{
			//If the player is no longer holding a gun, emulate a release of the shoot button
			if(player.getInventory().getSelectedItem().isEmpty() || !(player.getInventory().getSelectedItem().getItem() instanceof ItemGun))
			{
				data.isShootingRight = data.isShootingLeft = false;
			}
			return;
		}
		
		// And finally do sounds
		if(soundDelay > 0)
		{
			soundDelay--;
		}
	}
	
	/**
	 * Generic update method. If we have an off hand weapon, it will also make calls for that
	 * Passes on to onUpdateEach
	 */
	@Override
	public void inventoryTick(ItemStack itemstack, ServerLevel world, Entity entity, EquipmentSlot slot)
	{
		if(entity instanceof Player)
		{
			Player player = (Player)entity;
			InteractionHand hand;
			if(itemstack == player.getMainHandItem())
			{
				hand = InteractionHand.MAIN_HAND;
			}
			else if(itemstack == player.getOffhandItem())
			{
				hand = InteractionHand.OFF_HAND;
			}
			else
			{
				return;
			}
			
			ItemStack main = player.getMainHandItem();
			ItemStack off = player.getOffhandItem();
			boolean hasOffHand = !main.isEmpty() && !off.isEmpty();
			
			onUpdateEach(itemstack, 0, world, entity, hand, hasOffHand);
		}
	}
	
	/**
	 * Called once for each weapon we are weilding
	 */
	private void onUpdateEach(ItemStack itemstack, int gunSlot, Level world, Entity entity, InteractionHand hand, boolean hasOffHand)
	{
		if(world.isClientSide())
			onUpdateClient(itemstack, gunSlot, world, entity, hand, hasOffHand);
		else onUpdateServer(itemstack, gunSlot, world, entity, hand, hasOffHand);
	}
	
	public boolean Reload(ItemStack gunstack, Level world, Entity entity, Container inventory, InteractionHand hand, boolean hasOffHand, boolean forceReload, boolean isCreative)
	{
		//Deployable guns cannot be reloaded in the inventory
		
		//TODO investigate if this code can can actually be called by an deployable
		if(type.deployable)
			return false;
		
		//If you cannot reload half way through a clip, reject the player for trying to do so
		if(forceReload && !type.canForceReload)
			return false;
		
		//For playing sounds afterwards
		boolean reloadedSomething = false;
		//Check each ammo slot, one at a time
		for(int i = 0; i < type.numAmmoItemsInGun; i++)
		{
			//Get the stack in the slot
			ItemStack bulletStack = getBulletItemStack(gunstack, i);
			
			//If there is no magazine, if the magazine is empty or if this is a forced reload
			if(bulletStack == null || bulletStack.isEmpty() || bulletStack.getDamageValue() == bulletStack.getMaxDamage() || forceReload)
			{
				//Iterate over all inventory slots and find the magazine / bullet item with the most bullets
				int bestSlot = -1;
				int bulletsInBestSlot = 0;
				for(int j = 0; j < inventory.getContainerSize(); j++)
				{
					ItemStack item = inventory.getItem(j);
					if(item.getItem() instanceof ItemShootable && type.isCorrectAmmo(((ItemShootable)(item.getItem())).type))
					{
						int bulletsInThisSlot = item.getMaxDamage() - item.getDamageValue();
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
					ItemStack newBulletStack = inventory.getItem(bestSlot);
					ShootableType newBulletType = ((ItemShootable)newBulletStack.getItem()).type;
					
					//Unload the old magazine (Drop an item if it is required and the player is not in creative mode)
					if(bulletStack != null && bulletStack.getItem() instanceof ItemShootable && ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload != null && !isCreative && bulletStack.getDamageValue() == bulletStack.getMaxDamage())
					{
						if(!world.isClientSide())
							dropItem(world, entity, ((ItemShootable)bulletStack.getItem()).type.dropItemOnReload);
					}
					
					//The magazine was not finished, pull it out and give it back to the player or, failing that, drop it
					if(bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage())
					{
						if(!InventoryHelper.add(inventory, bulletStack, isCreative))
						{
							if(!world.isClientSide())
								entity.spawnAtLocation((ServerLevel)world, bulletStack, 0.5F);
						}
					}
					
					//Load the new magazine
					ItemStack stackToLoad = newBulletStack.copy();
					stackToLoad.setCount(1);
					setBulletItemStack(gunstack, stackToLoad, i);
					
					//Remove the magazine from the inventory
					if(!isCreative)
						newBulletStack.setCount(newBulletStack.getCount() - 1);
					if(newBulletStack.getCount() <= 0)
						newBulletStack = ItemStack.EMPTY.copy();
					inventory.setItem(bestSlot, newBulletStack);
					
					
					//Tell the sound player that we reloaded something
					reloadedSomething = true;
				}
			}
		}
		return reloadedSomething;
	}
	
	private boolean needsToReload(ItemStack stack)
	{
		for(int i = 0; i < type.numAmmoItemsInGun; i++)
		{
			ItemStack bulletStack = getBulletItemStack(stack, i);
			if(bulletStack != null && !bulletStack.isEmpty() && bulletStack.getDamageValue() < bulletStack.getMaxDamage())
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean CanReload(ItemStack gunstack, Container inventory)
	{
		for(int i = 0; i < inventory.getContainerSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if(type.isCorrectAmmo(stack))
			{
				return true;
			}
		}
		return false;
	}
	
	private ItemStack getBestNonEmptyShootableStack(ItemStack stack)
	{
		for(int i = 0; i < type.numAmmoItemsInGun; i++)
		{
			ItemStack shootableStack = getBulletItemStack(stack, i);
			if(shootableStack != null && !shootableStack.isEmpty() && shootableStack.getDamageValue() < shootableStack.getMaxDamage())
			{
				return shootableStack;
			}
		}
		return null;
	}
	
	
	// _____________________________________________________________________________
	//
	// Minecraft base item overrides
	// _____________________________________________________________________________
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag)
	{
		if(GunUtil.hasTag(stack) && GunUtil.getTag(stack).contains("LegendaryCrafter"))
		{
			String crafter = GunUtil.getTag(stack).getStringOr("LegendaryCrafter", "");
			tooltip.accept(Component.literal("Legendary Skin Crafted by " + crafter));
		}
		
		if(type.description != null)
		{
			for(String line : type.description.split("_"))
				tooltip.accept(Component.literal(line));
		}
		if(type.showDamage)
			tooltip.accept(Component.literal("\u00a79Damage" + "\u00a77: " + type.getDamage(stack)));
		if(type.showRecoil)
			tooltip.accept(Component.literal("\u00a79Recoil" + "\u00a77: " + type.getRecoil(stack)));
		if(type.showSpread)
			tooltip.accept(Component.literal("\u00a79Accuracy" + "\u00a77: " + type.getSpread(stack)));
		if(type.showReloadTime)
			tooltip.accept(Component.literal("\u00a79Reload Time" + "\u00a77: " + type.getReloadTime(stack) / 20 + "s"));
		for(AttachmentType attachment : type.getCurrentAttachments(stack))
		{
			if(type.showAttachments)
			{
				tooltip.accept(Component.literal(attachment.name));
			}
		}
		for(int i = 0; i < type.numAmmoItemsInGun; i++)
		{
			ItemStack bulletStack = getBulletItemStack(stack, i);
			if(bulletStack != null && bulletStack.getItem() instanceof ItemBullet)
			{
				BulletType bulletType = ((ItemBullet)bulletStack.getItem()).type;
				//String line = bulletType.name + (bulletStack.getMaxDamage() == 1 ? "" : " " + (bulletStack.getMaxDamage() - bulletStack.getDamageValue()) + "/" + bulletStack.getMaxDamage());
				String line = bulletType.name + " " + (bulletStack.getMaxDamage() - bulletStack.getDamageValue()) + "/" + bulletStack.getMaxDamage();
				tooltip.accept(Component.literal(line));
			}
		}
	}
	
	public DamageSource getMeleeDamage(Player attacker)
	{
		return new EntityDamageSourceFlan(type.shortName, attacker, attacker, type);
	}
	
	private boolean isSolid(Level world, int i, int j, int k)
	{
		BlockState state = world.getBlockState(new BlockPos(i, j, k));
		return state.isSolid();
	}
	
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)
	{
		return false;
	}
	
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity)
	{
		return 100;
	}
	
	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack)
	{
		return ItemUseAnimation.BOW;
	}
	
	protected static final UUID KNOCKBACK_RESIST_MODIFIER = UUID.fromString("77777777-645C-4F38-A497-9C13A33DB5CF");
	protected static final UUID MOVEMENT_SPEED_MODIFIER = UUID.fromString("99999999-4180-4865-B01B-BCCE9785ACA3");
	protected static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("88888888-4180-4865-B01B-BCCE9785ACA3");
	
	@Override
	public boolean isFoil(ItemStack stack)
	{
		return false;
	}
}
