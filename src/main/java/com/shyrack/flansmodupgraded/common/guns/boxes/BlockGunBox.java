package com.shyrack.flansmodupgraded.common.guns.boxes;

import java.util.ArrayList;

import com.shyrack.flansmodupgraded.common.FlansMod;
import com.shyrack.flansmodupgraded.common.guns.GunType;
import com.shyrack.flansmodupgraded.common.guns.boxes.GunBoxType.GunBoxEntry;
import com.shyrack.flansmodupgraded.common.types.InfoType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class BlockGunBox extends Block {

    public GunBoxType type;

    public BlockGunBox(GunBoxType t) {
        super(Properties.of(Material.WOOD).strength(2F).explosionResistance(4F));
        type = t;
        setRegistryName(type.shortName);
        setTranslationKey(type.shortName);
        setCreativeTab(FlansMod.tabFlanGuns);
        type.block = this;
    }

    public void buyGun(InfoType gun, Inventory inventory, GunBoxType type) {
        //FlansMod.proxy.buyGun(type, gun);
        GunBoxEntry entry = type.canCraft(gun);
        if (entry != null) {
            boolean canBuy = true;
            for (ItemStack check : entry.requiredParts) {
                int numMatchingStuff = 0;
                for (int j = 0; j < inventory.getSizeInventory(); j++) {
                    ItemStack stack = inventory.getStackInSlot(j);
                    if (stack != null && !stack.isEmpty() && stack.getItem() == check.getItem() && stack.getItemDamage() == check.getItemDamage()) {
                        numMatchingStuff += stack.getCount();
                    }
                }
                if (numMatchingStuff < check.getCount()) {
                    canBuy = false;
                }
            }
            if (canBuy) {
                for (ItemStack remove : entry.requiredParts) {
                    int amountLeft = remove.getCount();
                    for (int j = 0; j < inventory.getSizeInventory(); j++) {
                        ItemStack stack = inventory.getStackInSlot(j);
                        if (amountLeft > 0 && stack != null && !stack.isEmpty() && stack.getItem() == remove.getItem() && stack.getItemDamage() == remove.getItemDamage()) {
                            amountLeft -= inventory.decrStackSize(j, amountLeft).getCount();
                        }
                    }
                }
                ItemStack gunStack = new ItemStack(entry.type.item);
                if (entry.type instanceof GunType) {
                    GunType gunType = (GunType) entry.type;
                    NBTTagCompound tags = new NBTTagCompound();
                    tags.setString("Paint", gunType.defaultPaintjob.iconName);
                    //Add ammo tags
                    NBTTagList ammoTagsList = new NBTTagList();
                    for (int j = 0; j < gunType.numAmmoItemsInGun; j++) {
                        ammoTagsList.appendTag(new NBTTagCompound());
                    }
                    tags.setTag("ammo", ammoTagsList);

                    gunStack.setTagCompound(tags);
                }
                if (!inventory.addItemStackToInventory(gunStack)) {
                    // Drop gun on floor
                    inventory.player.dropItem(gunStack, false);
                }
            } else {
                // Cant buy
                // TODO : Add flashing red squares around the items you lack
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float par7, float par8, float par9) {
        if (player.isSneaking())
            return false;
        if (!world.isRemote)
            player.openGui(FlansMod.INSTANCE, 5, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<>();
        ret.add(new ItemStack(this, 1, 0));
        return ret;
    }
}
