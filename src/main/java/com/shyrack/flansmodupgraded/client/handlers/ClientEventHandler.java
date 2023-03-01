package com.shyrack.flansmodupgraded.client.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.shyrack.flansmodupgraded.client.ClientRenderHooks;
import com.shyrack.flansmodupgraded.client.FlansModClient;
import com.shyrack.flansmodupgraded.client.model.InstantBulletRenderer;
import com.shyrack.flansmodupgraded.client.model.RenderFlag;
import com.shyrack.flansmodupgraded.client.model.RenderGun;
import com.shyrack.flansmodupgraded.common.FlansMod;
import com.shyrack.flansmodupgraded.common.guns.ItemGun;

/**
 * All handled events for the client should go through here and be passed on, this makes it easier to see which events
 * are being handled by the mod
 */
public class ClientEventHandler {

    private KeyInputHandler keyInputHandler;
    private MouseInputHandler mouseInputHandler;
    private ClientRenderHooks renderHooks;

    public ClientEventHandler() {
        this.keyInputHandler = new KeyInputHandler();
        this.mouseInputHandler = new MouseInputHandler();
        this.renderHooks = new ClientRenderHooks();
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        switch (event.phase) {
            case START: {
                RenderGun.smoothing = event.renderTickTime;
                FlansModClient.updateCameraZoom(event.renderTickTime);
                renderHooks.setPartialTick(event.renderTickTime);
                renderHooks.updatePlayerView();
                break;
            }
            case END: {
                break;
            }
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        switch (event.phase) {
            case START: {
                //Handle all packets received since last tick
                FlansMod.getPacketHandler().handleClientPackets();
                FlansModClient.updateFlashlights(Minecraft.getInstance());
                break;
            }
            case END: {
                InstantBulletRenderer.UpdateAllTrails();
                renderHooks.update();
                RenderFlag.angle += 2F;
                FlansModClient.tick();
                keyInputHandler.checkTickKeys();
                break;
            }
        }
    }

    @SubscribeEvent
    public void chatMessage(ClientChatReceivedEvent event) {
        if (event.getMessage().getUnformattedText().equals("#flansmod")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void checkMouseInput(InputEvent.MouseButton event) {
        this.mouseInputHandler.checkMouseInput(event);

        Player player = Minecraft.getInstance().player;
        if (player.getHeldItemMainhand().getItem() instanceof ItemGun) {
            if (((ItemGun) player.getHeldItemMainhand().getItem()).GetType().oneHanded &&
                    Minecraft.getInstance().gameSettings.keyBindSneak.isKeyDown() &&
                    Math.abs(event.getDwheel()) > 0)
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        keyInputHandler.checkEventKeys();
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        InstantBulletRenderer.RenderAllTrails(event.getPartialTicks());
    }

    // ----------------------------------------
    // Lots of events for the ClientRenderHooks
    // ----------------------------------------
    @SubscribeEvent
    public void renderItemFrame(RenderItemInFrameEvent event) {
        renderHooks.renderItemFrame(event);
    }

    @SubscribeEvent
    public void renderHeldItem(RenderSpecificHandEvent event) {
        renderHooks.renderHeldItem(event);
    }

    @SubscribeEvent
    public void renderThirdPersonWeapons(RenderLivingEvent.Pre event) {
        renderHooks.renderThirdPersonWeapons(event);
    }

    @SubscribeEvent
    public void renderPlayer(RenderPlayerEvent.Pre event) {
        renderHooks.renderPlayer(event);
    }

    @SubscribeEvent
    public void cameraSetup(CameraSetup event) {
        renderHooks.cameraSetup(event);
    }

    @SubscribeEvent
    public void modifyHUD(RenderGuiOverlayEvent event) {
        renderHooks.modifyHUD(event);
    }

}
