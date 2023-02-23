package com.shyrack.flansmodupgraded.common.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;

import java.util.UUID;

public class BlockUtil {

    public static boolean destroyBlock(ServerLevel level, BlockPos pos, Entity entity, boolean dropBlock) {
        Player player = entity instanceof Player ? (Player) entity : new FakePlayer(level, new GameProfile(UUID.randomUUID(), "fakePlayer"));
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(breakEvent);

        if (breakEvent.isCanceled()) {
            return false;
        }

        level.destroyBlock(pos, dropBlock);

        return true;
    }

}
