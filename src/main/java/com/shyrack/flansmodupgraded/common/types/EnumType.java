package com.shyrack.flansmodupgraded.common.types;

import com.shyrack.flansmodupgraded.common.ItemHolderType;
import com.shyrack.flansmodupgraded.common.TileEntityItemHolder;
import com.shyrack.flansmodupgraded.common.driveables.*;
import com.shyrack.flansmodupgraded.common.driveables.mechas.*;
import com.shyrack.flansmodupgraded.common.enchantments.GloveType;
import com.shyrack.flansmodupgraded.common.enchantments.ItemGlove;
import com.shyrack.flansmodupgraded.common.guns.*;
import com.shyrack.flansmodupgraded.common.guns.boxes.BlockGunBox;
import com.shyrack.flansmodupgraded.common.guns.boxes.GunBoxType;
import com.shyrack.flansmodupgraded.common.parts.ItemPart;
import com.shyrack.flansmodupgraded.common.parts.PartType;
import com.shyrack.flansmodupgraded.common.teams.*;
import com.shyrack.flansmodupgraded.common.tools.ItemTool;
import com.shyrack.flansmodupgraded.common.tools.ToolType;

public enum EnumType {

    part("parts"), bullet("bullets"), attachment("attachments"), grenade("grenades"), gun("guns"),
    aa("aaguns"), vehicle("vehicles"), plane("planes"), mechaItem("mechaItems"), mecha("mechas"),
    tool("tools"), armour("armorFiles"), armourBox("armorBoxes"), box("boxes"), playerClass("classes"),
    team("teams"), itemHolder("itemHolders"), rewardBox("rewardBoxes"), loadout("loadouts"),
    glove("gloves");

    public String folderName;

    EnumType(String s) {
        folderName = s;
    }

    public static EnumType get(String s) {
        for (EnumType e : values()) {
            if (e.folderName.equals(s))
                return e;
        }
        return null;
    }

    public Class<? extends InfoType> getTypeClass() {
        switch (this) {
            case bullet:
                return BulletType.class;
            case aa:
                return AAGunType.class;
            case vehicle:
                return VehicleType.class;
            case plane:
                return PlaneType.class;
            case mechaItem:
                return MechaItemType.class;
            case mecha:
                return MechaType.class;
            case attachment:
                return AttachmentType.class;
            case gun:
                return GunType.class;
            case grenade:
                return GrenadeType.class;
            case tool:
                return ToolType.class;
            case armour:
                return ArmourType.class;
            case armourBox:
                return ArmourBoxType.class;
            case playerClass:
                return PlayerClass.class;
            case team:
                return Team.class;
            case box:
                return GunBoxType.class;
            case part:
                return PartType.class;
            case itemHolder:
                return ItemHolderType.class;
            case rewardBox:
                return RewardBox.class;
            case loadout:
                return LoadoutPool.class;
            case glove:
                return GloveType.class;
            default:
                return InfoType.class;
        }
    }

    public static EnumType getFromObject(Object o) {
        if (o instanceof MechaType || o instanceof ItemMecha || o instanceof EntityMecha) return mecha;
        if (o instanceof PlaneType || o instanceof ItemPlane || o instanceof EntityPlane) return plane;
        if (o instanceof VehicleType || o instanceof ItemVehicle || o instanceof EntityVehicle) return vehicle;
        if (o instanceof BulletType || o instanceof ItemBullet || o instanceof EntityBullet) return bullet;
        if (o instanceof AAGunType || o instanceof ItemAAGun || o instanceof EntityAAGun) return aa;
        if (o instanceof MechaItemType || o instanceof ItemMechaAddon) return mechaItem;
        if (o instanceof AttachmentType || o instanceof ItemAttachment) return attachment;
        if (o instanceof GunType || o instanceof ItemGun || o instanceof EntityMG) return gun;
        if (o instanceof GrenadeType || o instanceof ItemGrenade || o instanceof EntityGrenade) return grenade;
        if (o instanceof ToolType || o instanceof ItemTool) return tool;
        if (o instanceof ArmourType || o instanceof ItemTeamArmour) return armour;
        if (o instanceof ArmourBoxType || o instanceof BlockArmourBox) return armourBox;
        if (o instanceof PlayerClass) return playerClass;
        if (o instanceof Team) return team;
        if (o instanceof GunBoxType || o instanceof BlockGunBox) return box;
        if (o instanceof PartType || o instanceof ItemPart) return part;
        if (o instanceof ItemHolderType || o instanceof TileEntityItemHolder) return itemHolder;
        if (o instanceof RewardBox) return rewardBox;
        if (o instanceof LoadoutPool) return loadout;
        if (o instanceof GloveType || o instanceof ItemGlove) return glove;
        return null;
    }

}
