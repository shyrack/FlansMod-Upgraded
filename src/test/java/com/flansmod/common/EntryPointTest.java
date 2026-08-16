package com.flansmod.common;

import com.flansmod.apocalypse.common.FlansModApocalypse;
import com.flansmod.client.FlansModClient;
import com.flansmod.apocalypse.client.FlansModApocalypseClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Entry point tests. These must never trigger registry bootstrap, so they only
 * assert structure (mod ids, logger names, interface implementation) without
 * instantiating the entry points.
 */
class EntryPointTest {

    @Test
    void flansModHasCorrectModId() {
        assertEquals("flansmod", FlansMod.MOD_ID);
    }

    @Test
    void flansModLoggerIsNotNull() {
        assertNotNull(FlansMod.LOGGER);
    }

    @Test
    void flansModLoggerHasCorrectName() {
        assertEquals("flansmod", FlansMod.LOGGER.getName());
    }

    @Test
    void flansModImplementsModInitializer() {
        assertTrue(net.fabricmc.api.ModInitializer.class.isAssignableFrom(FlansMod.class));
    }

    @Test
    void flansModHasOnInitializeMethod() throws Exception {
        Method m = FlansMod.class.getMethod("onInitialize");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void flansModApocalypseHasCorrectModId() {
        assertEquals("flansmodapocalypse", FlansModApocalypse.MOD_ID);
    }

    @Test
    void flansModApocalypseImplementsModInitializer() {
        assertTrue(net.fabricmc.api.ModInitializer.class.isAssignableFrom(FlansModApocalypse.class));
    }

    @Test
    void flansModApocalypseHasOnInitializeMethod() throws Exception {
        Method m = FlansModApocalypse.class.getMethod("onInitialize");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void flansModClientImplementsClientModInitializer() {
        assertTrue(net.fabricmc.api.ClientModInitializer.class.isAssignableFrom(FlansModClient.class));
    }

    @Test
    void flansModClientHasOnInitializeClientMethod() throws Exception {
        Method m = FlansModClient.class.getMethod("onInitializeClient");
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void flansModApocalypseClientImplementsClientModInitializer() {
        assertTrue(net.fabricmc.api.ClientModInitializer.class.isAssignableFrom(FlansModApocalypseClient.class));
    }

    @Test
    void flansModApocalypseClientHasOnInitializeClientMethod() throws Exception {
        Method m = FlansModApocalypseClient.class.getMethod("onInitializeClient");
        assertEquals(void.class, m.getReturnType());
    }
}
