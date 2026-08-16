package com.flansmod.common.guns;

import com.flansmod.common.FlansMod;
import com.flansmod.common.guns.GunUtil;
import com.flansmod.common.util.FlansModUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.Level;

public class ContainerGunModTable extends AbstractContainerMenu
{
	private InventoryGunModTable inventory;
	public Inventory playerInv;
	public Level world;
	
	public ContainerGunModTable(Inventory i, Level w)
	{
		super(null, 0);
		playerInv = i;
		inventory = new InventoryGunModTable();
		world = w;
		
		//Gun slot
		SlotGun gunSlot = new SlotGun(inventory, 0, 80, 110, null);
		addSlot(gunSlot);
		
		//Attachment Slots
		addSlot(new SlotGun(inventory, 1, 54, 110, gunSlot));
		addSlot(new SlotGun(inventory, 2, 80, 84, gunSlot));
		addSlot(new SlotGun(inventory, 3, 106, 110, gunSlot));
		addSlot(new SlotGun(inventory, 4, 80, 136, gunSlot));
		
		for(int row = 0; row < 4; row++)
		{
			for(int col = 0; col < 2; col++)
			{
				addSlot(new SlotGun(inventory, 5 + row * 2 + col, 10 + col * 18, 83 + row * 18, gunSlot));
			}
		}
		
		//Main inventory slots
		for(int row = 0; row < 3; row++)
		{
			for(int col = 0; col < 9; col++)
			{
				addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 176 + row * 18));
			}
			
		}
		//Quickbar slots
		for(int col = 0; col < 9; col++)
		{
			addSlot(new Slot(playerInv, col, 8 + col * 18, 234));
		}
	}
	
	@Override
	public void removed(Player player)
	{
		if(!inventory.getItem(0).isEmpty())
			player.drop(inventory.getItem(0), false);
	}
	
	@Override
	public boolean stillValid(Player entityplayer)
	{
		return true;
	}
	
	@Override
	public ItemStack quickMoveStack(Player player, int slotID)
	{
		ItemStack stack = ItemStack.EMPTY.copy();
		Slot currentSlot = slots.get(slotID);
		
		Slot gunSlot = slots.get(0);
		
		if(currentSlot != null && currentSlot.hasItem())
		{
			ItemStack slotStack = currentSlot.getItem();
			stack = slotStack.copy();
			
			// gun slot, 4 attach slots and 8 generics
			if(slotID >= 13)
			{
				if(slotStack.getItem() instanceof ItemGun && !gunSlot.hasItem())
				{
					gunSlot.set(slotStack);
					currentSlot.set(ItemStack.EMPTY.copy());
				}
				if(slotStack.getItem() instanceof ItemAttachment)
				{
					for(int i = 1; i < 12; i++)
					{
						Slot attachmentSlot = slots.get(i);
						if(!attachmentSlot.hasItem() && attachmentSlot.mayPlace(slotStack))
						{
							attachmentSlot.set(slotStack);
							currentSlot.set(ItemStack.EMPTY.copy());
							break;
						}
					}
				}
				return ItemStack.EMPTY.copy();
			}
			else
			{
				if(!moveItemStackTo(slotStack, 13, slots.size(), true))
				{
					return ItemStack.EMPTY.copy();
				}
			}
			
			if(slotStack.getCount() == 0)
			{
				currentSlot.set(ItemStack.EMPTY.copy());
			}
			else
			{
				currentSlot.setChanged();
			}
			
			if(slotStack.getCount() == stack.getCount())
			{
				return ItemStack.EMPTY.copy();
			}
			
			currentSlot.onTake(player, slotStack);
		}
		
		return stack;
	}
	
	public void pressButton(boolean paint, boolean left)
	{
		//Nope.
	}
	
	public void clickPaintjob(int i)
	{
		ItemStack gunStack = inventory.getItem(0);
		if(gunStack != null && gunStack.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)gunStack.getItem()).GetType();
			clickPaintjob(gunType.getPaintjob(i));
		}
	}
	
	public void clickPaintjob(Paintjob paintjob)
	{		
		ItemStack gunStack = inventory.getItem(0);
		if(gunStack != null && gunStack.getItem() instanceof ItemGun)
		{
			GunType gunType = ((ItemGun)gunStack.getItem()).GetType();
			int numDyes = paintjob.dyesNeeded.length;
			
			boolean legendary = false;
			for(int n = 0; n < numDyes; n++)
			{
				if(paintjob.dyesNeeded[n].getItem() == FlansMod.rainbowPaintcan)
				{
					legendary = true;
				}
			}
			
			if(!playerInv.player.getAbilities().instabuild)
			{
				//Calculate which dyes we have in our inventory
				for(int n = 0; n < numDyes; n++)
				{
					int amountNeeded = paintjob.dyesNeeded[n].getCount();
					boolean lookingForRainbow = paintjob.dyesNeeded[n].getItem() == FlansMod.rainbowPaintcan;
					for(int s = 0; s < playerInv.getContainerSize(); s++)
					{
						ItemStack stack = playerInv.getItem(s);
						if(lookingForRainbow)
						{
							if(stack.getItem() == FlansMod.rainbowPaintcan)
								amountNeeded -= stack.getCount();
						}
						else
						{
							if(stack != null && stack.getItem() instanceof DyeItem && stack.getItem() == paintjob.dyesNeeded[n].getItem())
								amountNeeded -= stack.getCount();
						}
					}
					//We don't have enough of this dye
					if(amountNeeded > 0)
						return;
				}
				
				for(int n = 0; n < numDyes; n++)
				{
					int amountNeeded = paintjob.dyesNeeded[n].getCount();
					for(int s = 0; s < playerInv.getContainerSize(); s++)
					{
						if(amountNeeded <= 0)
							continue;
						ItemStack stack = playerInv.getItem(s);
						boolean lookingForRainbow = paintjob.dyesNeeded[n].getItem() == FlansMod.rainbowPaintcan;
						if(lookingForRainbow)
						{
							if(stack.getItem() == FlansMod.rainbowPaintcan)
							{
								ItemStack consumed = playerInv.removeItem(s, amountNeeded);
								amountNeeded -= stack.getCount();
							}
						}
						else
						{
							if(stack != null && stack.getItem() instanceof DyeItem && stack.getItem() == paintjob.dyesNeeded[n].getItem())
							{
								ItemStack consumed = playerInv.removeItem(s, amountNeeded);
								amountNeeded -= consumed.getCount();
							}
						}
					}
				}
			}
			
			//Paint the gun. This line is only reached if the player is in creative or they have had their dyes taken already
			//FlansModUtil.getItemTag(gunStack).putString("Paint", paintjob.iconName);
			gunStack.setDamageValue(paintjob.ID);
			if(legendary)
			{
				if(!GunUtil.hasTag(gunStack))
				{
					GunUtil.setTag(gunStack, new CompoundTag());
				}
				if(!GunUtil.getTag(gunStack).contains("display"))
				{
					GunUtil.getTag(gunStack).put("display", new CompoundTag());
				}
				if(!GunUtil.getTag(gunStack).getCompoundOrEmpty("display").contains("Name"))
				{
					GunUtil.getTag(gunStack).getCompoundOrEmpty("display").putString("Name", "\u00a7e" + playerInv.player.getName().getString() + "'s " + gunStack.getHoverName().getString());
				}
				GunUtil.getTag(gunStack).putString("LegendaryCrafter", playerInv.player.getName().getString());
			}
		}
	}
}
