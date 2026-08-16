package com.flansmod.common.driveables.mechas;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.flansmod.common.guns.ItemGun;

public class SlotMecha extends Slot
{
	private EnumMechaSlotType slotType;
	
	public SlotMecha(Container inv, EnumMechaSlotType e, int x, int y)
	{
		super(inv, e.ordinal(), x, y);
		slotType = e;
	}
	
	@Override
	public boolean mayPlace(ItemStack stack)
	{
		if(stack == null || stack.isEmpty())
			return true;

		EnumMechaItemType itemType = null;
		Item item = stack.getItem();
		if(item instanceof ItemGun && ((ItemGun)item).GetType().usableByMechas)
			itemType = EnumMechaItemType.tool;
		else if(item instanceof ItemMechaAddon)
			itemType = ((ItemMechaAddon)item).type.type;
		else return false;

		return slotType.accepts(itemType);
	}
	
	@Override
	public void set(ItemStack stack)
	{
		if(!mayPlace(stack))
			return;
		container.setItem(slotType.ordinal(), stack);
		setChanged();
	}

}
