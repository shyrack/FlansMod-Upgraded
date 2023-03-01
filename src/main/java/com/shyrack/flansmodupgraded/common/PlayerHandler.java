package com.shyrack.flansmodupgraded.common;

import com.shyrack.flansmodupgraded.common.driveables.EntityDriveable;
import com.shyrack.flansmodupgraded.common.driveables.EntitySeat;
import com.shyrack.flansmodupgraded.common.teams.TeamsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.*;

public class PlayerHandler {

    public static Map<UUID, PlayerData> serverSideData = new HashMap<>();
    public static Map<UUID, PlayerData> clientSideData = new HashMap<>();
    public static ArrayList<UUID> clientsToRemoveAfterThisRound = new ArrayList<>();
    public static Field floatingTickCount = null;

    public PlayerHandler() {
        MinecraftForge.EVENT_BUS.register(this);

        try {
            floatingTickCount = ObfuscationReflectionHelper.findField(NetHandlerPlayServer.class, "floatingTickCount", "field_147365_f");
        } catch (Exception e) {
            FlansMod.log.error("Couldn't find floatingTickCount field.", e);
        }
    }

    @SubscribeEvent
    public void onEntityHurt(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getVehicle() instanceof EntityDriveable || entity.getVehicle() instanceof EntitySeat) {
            //TODO Set Drivable damage
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityKilled(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            getPlayerData((Player) entity).playerKilled();
        }
    }

    public void serverTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.getServer() == null) {
            FlansMod.log.warn("Receiving server ticks when server is null");
        } else {
            for (ServerLevel serverLevel : Objects.requireNonNull(Minecraft.getInstance().level.getServer()).getAllLevels()) {
                for (ServerPlayer player : serverLevel.players()) {
                    getPlayerData(player).tick(player);
                }
            }
        }
    }

    public void clientTick() {
        if (Minecraft.getInstance().level != null) {
            for (Player player : Minecraft.getInstance().level.players()) {
                getPlayerData(player).tick(player);
            }
        }
    }

    public static PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUUID();
        Map<UUID, PlayerData> playerDataMap = FMLEnvironment.dist != Dist.CLIENT ? PlayerHandler.clientSideData : PlayerHandler.serverSideData;
        return PlayerHandler.getAndSetEntryFromPlayerData(playerDataMap, uuid);
    }

    public static PlayerData getAndSetEntryFromPlayerData(Map<UUID, PlayerData> map, UUID uuid) {
        if (!map.containsKey(uuid)) {
            map.put(uuid, new PlayerData(uuid));
        }
        return map.get(uuid);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        PlayerData data = new PlayerData(uuid);
        data.ReadFromFile();

        getAndSetEntryFromPlayerData(serverSideData, uuid);
        clientsToRemoveAfterThisRound.remove(uuid);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        clientsToRemoveAfterThisRound.add(uuid);

        if (TeamsManager.getInstance().currentRound == null) {
            roundEnded();
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        getAndSetEntryFromPlayerData(serverSideData, uuid);
    }

    /**
     * Called by teams manager to remove lingering player data
     */
    public static void roundEnded() {
        for (UUID uuid : clientsToRemoveAfterThisRound) {
            PlayerData data = serverSideData.get(uuid);
            if (data != null) {
                data.WriteToFile();
            }
            serverSideData.remove(uuid);
        }
    }

}
