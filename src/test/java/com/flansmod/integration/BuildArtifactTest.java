package com.flansmod.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class BuildArtifactTest {

    private static Path jarPath;

    @BeforeAll
    static void findJar() {
        Path libsDir = Path.of("build/libs");
        assertTrue(Files.exists(libsDir), "build/libs directory must exist (run ./gradlew build first)");
        try (var stream = Files.list(libsDir)) {
            jarPath = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("-sources"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            fail("Failed to list build/libs: " + e.getMessage());
        }
        assertNotNull(jarPath, "No JAR found in build/libs");
    }

    @Test
    void jarExists() {
        assertTrue(Files.exists(jarPath), "JAR file must exist");
        assertTrue(Files.isRegularFile(jarPath), "JAR must be a regular file");
    }

    @Test
    void jarHasCorrectBaseName() {
        String name = jarPath.getFileName().toString();
        assertTrue(name.startsWith("flansmod-"), "JAR name must start with flansmod-");
        assertTrue(name.endsWith(".jar"), "JAR name must end with .jar");
    }

    @Test
    void jarContainsFabricModJson() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("fabric.mod.json");
            assertNotNull(entry, "JAR must contain fabric.mod.json");
        }
    }

    @Test
    void jarContainsApocalypseModJson() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("flansmodapocalypse.mod.json");
            assertNotNull(entry, "JAR must contain flansmodapocalypse.mod.json");
        }
    }

    @Test
    void jarContainsFlansModClass() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("com/flansmod/common/FlansMod.class");
            assertNotNull(entry, "JAR must contain FlansMod.class");
        }
    }

    @Test
    void jarContainsFlansModClientClass() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("com/flansmod/client/FlansModClient.class");
            assertNotNull(entry, "JAR must contain FlansModClient.class");
        }
    }

    @Test
    void jarContainsFlansModApocalypseClass() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("com/flansmod/apocalypse/common/FlansModApocalypse.class");
            assertNotNull(entry, "JAR must contain FlansModApocalypse.class");
        }
    }

    @Test
    void jarContainsFlansModApocalypseClientClass() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("com/flansmod/apocalypse/client/FlansModApocalypseClient.class");
            assertNotNull(entry, "JAR must contain FlansModApocalypseClient.class");
        }
    }

    @Test
    void jarContainsMainModIcon() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("assets/flansmod/icon.png");
            assertNotNull(entry, "JAR must contain assets/flansmod/icon.png");
        }
    }

    @Test
    void jarContainsApocalypseIcon() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("assets/flansmodapocalypse/icon.png");
            assertNotNull(entry, "JAR must contain assets/flansmodapocalypse/icon.png");
        }
    }

    @Test
    void jarContainsPackMcmeta() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("pack.mcmeta");
            assertNotNull(entry, "JAR must contain pack.mcmeta");
        }
    }

    @Test
    void jarDoesNotContainMcmodInfo() throws Exception {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry("mcmod.info");
            assertNull(entry, "JAR must NOT contain old mcmod.info");
        }
    }

    @Test
    void jarContainsItemAssets() throws Exception {
        String[] items = {"gun", "bullet", "grenade", "attachment", "aa_gun",
                "plane", "vehicle", "mecha", "mecha_addon", "part", "tool",
                "team_armour", "flagpole", "op_stick", "reward_box", "shootable", "paintcan"};
        try (var jar = new JarFile(jarPath.toFile())) {
            for (String id : items) {
                assertNotNull(jar.getEntry("assets/flansmod/items/" + id + ".json"),
                        "Missing items/" + id + ".json");
                assertNotNull(jar.getEntry("assets/flansmod/models/item/" + id + ".json"),
                        "Missing models/item/" + id + ".json");
                assertNotNull(jar.getEntry("assets/flansmod/textures/item/" + id + ".png"),
                        "Missing textures/item/" + id + ".png");
            }
        }
    }
}
