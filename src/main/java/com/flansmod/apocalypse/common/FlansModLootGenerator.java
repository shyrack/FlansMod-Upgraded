package com.flansmod.apocalypse.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import com.flansmod.common.TileEntityItemHolder;
import com.flansmod.common.driveables.DriveableType;
import com.flansmod.common.driveables.EnumPlaneMode;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.ShootableType;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.parts.EnumPartCategory;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.teams.PlayerClass;
import com.flansmod.common.teams.Team;
import com.flansmod.common.tools.ToolType;
import com.flansmod.common.types.EnumType;

public class FlansModLootGenerator
{
	private static ArrayList<VehicleType> tanks, cars;
	private static ArrayList<PlaneType> planes, helicopters;
	private static ArrayList<MechaType> mechas, dungeonMechas;
	private static ArrayList<PartType> vehicleEngines, planeEngines, mechaEngines;
	private static ArrayList<GunType> validGuns;
	
	private static final Item[] dyeItems = new Item[]{
			Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
			Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE,
			Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
			Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE};
	
	private static int[] potions = new int[]{8193, 8194, 8195, 8197, 8198, 8201, 8203, 8205, 8206};
	
	public FlansModLootGenerator()
	{
		tanks = new ArrayList<>();
		cars = new ArrayList<>();
		planes = new ArrayList<>();
		helicopters = new ArrayList<>();
		mechas = new ArrayList<>();
		dungeonMechas = new ArrayList<>();
		
		
		for(DriveableType type : DriveableType.types)
		{
			if(type instanceof VehicleType)
			{
				if(((VehicleType)type).tank)
					tanks.add((VehicleType)type);
				else if(!type.floatOnWater)
					cars.add((VehicleType)type);
			}
			else if(type instanceof PlaneType)
			{
				if(((PlaneType)type).mode == EnumPlaneMode.PLANE)
					planes.add((PlaneType)type);
				else helicopters.add((PlaneType)type);
			}
			else if(type instanceof MechaType)
			{
				mechas.add((MechaType)type);
				if(((MechaType)type).height <= 3F)
					dungeonMechas.add((MechaType)type);
			}
		}
		
		vehicleEngines = new ArrayList<>();
		mechaEngines = new ArrayList<>();
		planeEngines = new ArrayList<>();
		
		for(PartType type : PartType.partsByCategory.get(EnumPartCategory.ENGINE))
		{
			if(type.isAIChip)
				continue;
			if(type.worksWith.contains(EnumType.plane))
				planeEngines.add(type);
			if(type.worksWith.contains(EnumType.vehicle))
				vehicleEngines.add(type);
			if(type.worksWith.contains(EnumType.mecha))
				mechaEngines.add(type);
		}
		
		validGuns = new ArrayList<>();
		for(GunType type : GunType.gunList)
			if(type.dungeonChance != 0)
				validGuns.add(type);
	}
	
	public ItemStack getRandomLoadedGun(Random rand, boolean explosivesAllowed)
	{
		ItemStack stack = getRandomUnloadedGun(rand);
		
		GunType gunType = ((ItemGun)stack.getItem()).GetType();
		List<ShootableType> ammoList = explosivesAllowed ? gunType.ammo : gunType.nonExplosiveAmmo;
		if(ammoList.size() > 0)
		{
			ListTag ammoTagsList = new ListTag();
			for(int i = 0; i < gunType.numAmmoItemsInGun; i++)
			{
				CompoundTag ammoTag = new CompoundTag();
				ShootableType ammoType = ammoList.get(rand.nextInt(ammoList.size()));
				ItemStack ammoStack = new ItemStack(ammoType.item);
				ammoStack.setDamageValue(rand.nextInt(ammoType.roundsPerItem));
				GunUtil.stackToTag(ammoTag, ammoStack);
				ammoTagsList.add(ammoTag);
			}
			GunUtil.getOrCreateTag(stack).put("ammo", ammoTagsList);
		}
		if(gunType.paintjobs.size() > 1)
			stack.setDamageValue(rand.nextInt(gunType.nonlegendarypaintjobs.size()));
		return stack;
	}
	
	public ItemStack getRandomUnloadedGun(Random rand)
	{
		GunType gun = validGuns.get(rand.nextInt(validGuns.size()));
		ItemStack stack = new ItemStack(gun.item);
		CompoundTag tags = new CompoundTag();
		tags.putString("Paint", gun.nonlegendarypaintjobs.get(rand.nextInt(gun.nonlegendarypaintjobs.size())).iconName);
		GunUtil.setTag(stack, tags);
		return stack;
	}
	
	public void addRandomLoot(TileEntityItemHolder holder, Random rand, boolean gunsOnly)
	{
		//Add a gun, 2/3rds of the time
		if(gunsOnly || rand.nextInt(3) != 0)
			holder.setStack(getRandomLoadedGun(rand, true));
		else if(rand.nextBoolean())
			holder.setStack(getSurvivorJournal(rand));
		else if(rand.nextBoolean())
			holder.setStack(new ItemStack(Items.ROTTEN_FLESH, 1 + rand.nextInt(3)));
	}
	
	public void fillVillageChest(Random rand, Container chest)
	{
		int numParts = rand.nextInt(6) + 1;
		int numAmmo = rand.nextInt(6) + 1;
		int numFuel = rand.nextInt(3);
		int numFood = rand.nextInt(3);
		
		//Add 1~5 random parts
		for(int i = 0; i < numParts; i++)
		{
			PartType part = PartType.parts.get(rand.nextInt(PartType.parts.size()));
			chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(part.item, 1));
		}
		
		//Add 1~5 random ammo
		for(int i = 0; i < numAmmo; i++)
		{
			ShootableType type = ShootableType.shootables.get(new ArrayList<>(ShootableType.shootables.keySet()).get(rand.nextInt(ShootableType.shootables.size())));
			if(type != null && type.dungeonChance != 0)
				chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(type.item, 1 + (type.maxStackSize > 1 && rand.nextBoolean() ? 1 : 0)));
		}
		
		//Add 0~2 fuel items
		ArrayList<PartType> fuelItems = PartType.partsByCategory.get(EnumPartCategory.FUEL);
		
		for(int i = 0; i < numFuel; i++)
		{
			PartType fuel = fuelItems.get(rand.nextInt(fuelItems.size()));
			chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(fuel.item, rand.nextInt(Math.min(fuel.stackSize - 1, 2)) + 1));
		}
		
		//Add 0~2 food items
		for(int i = 0; i < numFood; i++)
		{
			switch(rand.nextInt(4))
			{
				case 0: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.CHICKEN, rand.nextInt(2) + 1));
					break;
				case 1: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.PORKCHOP, rand.nextInt(2) + 1));
					break;
				case 2: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.BEEF, rand.nextInt(2) + 1));
					break;
				case 3: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.BAKED_POTATO, rand.nextInt(3) + 1));
					break;
			}
		}
		
		//Add 0~1 mecha parts
		if(rand.nextBoolean() && rand.nextBoolean())
		{
			chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(MechaItemType.types.get(rand.nextInt(MechaItemType.types.size())).item));
		}
		
		//Add 0~1 tools
		if(rand.nextBoolean())
		{
			chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(ToolType.tools.get(new ArrayList<>(ToolType.tools.keySet()).get(rand.nextInt(ToolType.tools.size()))).item));
		}
	}
	
	public PartType getRandomFuel(Random rand)
	{
		ArrayList<PartType> fuelItems = PartType.partsByCategory.get(EnumPartCategory.FUEL);
		return fuelItems.get(rand.nextInt(fuelItems.size()));
	}
	
	public ItemStack loadAndPaintGun(GunType gun, Random rand)
	{
		ItemStack stack = new ItemStack(gun.item);
		CompoundTag tags = new CompoundTag();
		tags.putString("Paint", gun.nonlegendarypaintjobs.get(rand.nextInt(gun.nonlegendarypaintjobs.size())).iconName);
		if(gun.ammo.size() > 0)
		{
			ListTag ammoTagsList = new ListTag();
			for(int i = 0; i < gun.numAmmoItemsInGun; i++)
			{
				CompoundTag ammoTag = new CompoundTag();
				ShootableType ammoType = gun.ammo.get(rand.nextInt(gun.ammo.size()));
				ItemStack ammoStack = new ItemStack(ammoType.item);
				ammoStack.setDamageValue(rand.nextInt(ammoType.roundsPerItem));
				GunUtil.stackToTag(ammoTag, ammoStack);
				ammoTagsList.add(ammoTag);
			}
			tags.put("ammo", ammoTagsList);
		}
		GunUtil.setTag(stack, tags);
		return stack;
	}
	
	public void dressMeUp(LivingEntity entity, Random rand)
	{
		if(rand.nextBoolean() && ArmourType.armours.size() > 0)
		{
			//Give a completely random piece of armour
			ArmourType armour = ArmourType.armours.get(rand.nextInt(ArmourType.armours.size()));
			if(armour != null && armour.type != 2)
				entity.setItemSlot(EquipmentSlot.values()[armour.type + 2], new ItemStack(armour.item));
		}
		else if(Team.teams.size() > 0)
		{
			//Give a random set of armour
			Team team = Team.teams.get(rand.nextInt(Team.teams.size()));
			if(team.hat != null)
				entity.setItemSlot(EquipmentSlot.HEAD, team.hat.copy());
			if(team.chest != null)
				entity.setItemSlot(EquipmentSlot.CHEST, team.chest.copy());
			if(team.legs != null)
				entity.setItemSlot(EquipmentSlot.LEGS, team.legs.copy());
			if(team.shoes != null)
				entity.setItemSlot(EquipmentSlot.FEET, team.shoes.copy());
			
			if(team.classes.size() > 0)
			{
				PlayerClass playerClass = team.classes.get(rand.nextInt(team.classes.size()));
				if(playerClass.hat != null)
					entity.setItemSlot(EquipmentSlot.HEAD, playerClass.hat.copy());
				if(playerClass.chest != null)
					entity.setItemSlot(EquipmentSlot.CHEST, playerClass.chest.copy());
				if(playerClass.legs != null)
					entity.setItemSlot(EquipmentSlot.LEGS, playerClass.legs.copy());
				if(playerClass.shoes != null)
					entity.setItemSlot(EquipmentSlot.FEET, playerClass.shoes.copy());
			}
		}
	}
	
	public Block getRandomWeaponBox(Random rand)
	{
		if(rand.nextInt(4) == 0)
		{
			//Get armour box
			if(ArmourBoxType.boxes.size() > 0)
			{
				return ArmourBoxType.boxes.get(new ArrayList<>(ArmourBoxType.boxes.keySet()).get(rand.nextInt(ArmourBoxType.boxes.size()))).block;
			}
		}
		else
		{
			//Get weapon box
			if(GunBoxType.gunBoxMap.size() > 0)
			{
				return GunBoxType.gunBoxMap.get(new ArrayList<>(GunBoxType.gunBoxMap.keySet()).get(rand.nextInt(GunBoxType.gunBoxMap.size()))).block;
			}
		}
		
		return Blocks.AIR;
	}
	
	public DriveableType getRandomDriveable(Random rand)
	{
		switch(rand.nextInt(5))
		{
			case 0: if(cars.size() > 0) return cars.get(rand.nextInt(cars.size()));
			case 1: if(tanks.size() > 0) return tanks.get(rand.nextInt(tanks.size()));
			case 2: if(planes.size() > 0) return planes.get(rand.nextInt(planes.size()));
			case 3: if(helicopters.size() > 0) return helicopters.get(rand.nextInt(helicopters.size()));
			case 4: if(mechas.size() > 0) return mechas.get(rand.nextInt(mechas.size()));
		}
		return null;
	}
	
	public PartType getRandomEngine(DriveableType type, Random rand)
	{
		switch(EnumType.getFromObject(type))
		{
			case vehicle: return vehicleEngines.size() > 0 ? vehicleEngines.get(rand.nextInt(vehicleEngines.size())) : null;
			case plane: return planeEngines.size() > 0 ? planeEngines.get(rand.nextInt(planeEngines.size())) : null;
			case mecha: return mechaEngines.size() > 0 ? mechaEngines.get(rand.nextInt(mechaEngines.size())) : null;
			default: return null;
		}
	}
	
	public PlaneType getRandomPlane(Random rand)
	{
		if(planes.size() > 0)
			return planes.get(rand.nextInt(planes.size()));
		return null;
	}
	
	public void fillBrewingStand(Random rand, BrewingStandBlockEntity tileentity)
	{
		for(int i = 0; i < 3; i++)
			if(rand.nextBoolean())
			{
				ItemStack stack = new ItemStack(Items.POTION);
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));
				tileentity.setItem(i, stack);
			}
	}
	
	public void fillLiquidLabChest(Random rand, Container chest)
	{
		int numItems = 3 + rand.nextInt(4);
		for(int i = 0; i < numItems; i++)
		{
			switch(rand.nextInt(10))
			{
				case 0: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.BOWL, rand.nextInt(5) + 1));
					break;
				case 1: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.WATER_BUCKET));
					break;
				case 2:
					// TODO APOCALYPSE: 1.12.2 placed random Forge fluid buckets; FluidRegistry no longer exists
					chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.BUCKET));
					break;
				case 3: 
				case 4: 
				case 5: 
				case 6:
					ItemStack stack = new ItemStack(Items.POTION);
					stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));
					chest.setItem(rand.nextInt(chest.getContainerSize()), stack);
					break;
				case 7: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(FlansModApocalypse.sulphur, rand.nextInt(12) + 1));
					break;
				case 8: chest.setItem(rand.nextInt(chest.getContainerSize()), getScientistJournal(rand));
					break;
				case 9: chest.setItem(rand.nextInt(chest.getContainerSize()), getScientistJournal(rand));
					break;
			}
		}
	}
	
	public void fillWeaponChest(Random rand, Container chest)
	{
		for(int i = 0; i < 3 + rand.nextInt(3); i++)
		{
			ItemStack stack = getRandomAmmo(rand);
			if(stack != null)
			{
				chest.setItem(rand.nextInt(chest.getContainerSize()), stack);
			}
		}
		for(int i = 0; i < 1 + rand.nextInt(1); i++)
		{
			ItemStack stack = getRandomAttachment(rand);
			if(stack != null)
			{
				chest.setItem(rand.nextInt(chest.getContainerSize()), stack);
			}
		}
		
	}
	
	private ItemStack getRandomAmmo(Random rand)
	{
		GunType randomGun = validGuns.get(rand.nextInt(validGuns.size()));
		if(randomGun.ammo.size() <= 0)
			return null;
		ShootableType randomBullet = randomGun.ammo.get(rand.nextInt(randomGun.ammo.size()));
		return new ItemStack(randomBullet.item);
	}
	
	private ItemStack getRandomAttachment(Random rand)
	{
		AttachmentType type = AttachmentType.attachments.get(rand.nextInt(AttachmentType.attachments.size()));
		return new ItemStack(type.item);
	}
	
	public MechaType getRandomDungeonMecha(Random rand)
	{
		if(dungeonMechas.size() > 0)
			return dungeonMechas.get(rand.nextInt(dungeonMechas.size()));
		return null;
	}
	
	public void fillDyeFactoryChest(Container chest, Random rand)
	{
		int numDyes = rand.nextInt(4);
		int numMisc = rand.nextInt(2);
		
		for(int i = 0; i < numDyes; i++)
		{
			chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(dyeItems[rand.nextInt(16)], rand.nextInt(8) + 1));
		}
		
		for(int i = 0; i < numMisc; i++)
		{
			switch(rand.nextInt(4))
			{
				case 0: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.STRING, rand.nextInt(5) + 1));
					break;
				case 1: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.FEATHER, rand.nextInt(5) + 1));
					break;
				case 2: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.LEATHER, rand.nextInt(8) + 1));
					break;
				case 3: chest.setItem(rand.nextInt(chest.getContainerSize()), new ItemStack(Items.CLAY_BALL, rand.nextInt(32) + 1));
					break;
			}
		}
	}
	
	public ItemStack getScientistJournal(Random rand)
	{
		// TODO APOCALYPSE: 1.12.2 wrote journal text via NBT; written book content is
		// component based in 26.1.2 and not reproduced here
		return new ItemStack(Items.WRITTEN_BOOK);
	}
	
	public ItemStack getSurvivorJournal(Random rand)
	{
		// TODO APOCALYPSE: 1.12.2 wrote journal text via NBT; written book content is
		// component based in 26.1.2 and not reproduced here
		return new ItemStack(Items.WRITTEN_BOOK);
	}
}
