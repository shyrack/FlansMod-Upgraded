package com.flansmod.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class ModMetadataTest {

    private static String mainModJson;
    private static String apocalypseModJson;

    @BeforeAll
    static void loadModJsonFiles() throws Exception {
        Path libsDir = Path.of("build/libs");
        Path jarPath;
        try (var stream = Files.list(libsDir)) {
            jarPath = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("-sources"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No JAR found"));
        }
        try (var jar = new JarFile(jarPath.toFile())) {
            var mainEntry = jar.getEntry("fabric.mod.json");
            assertNotNull(mainEntry, "fabric.mod.json not found");
            mainModJson = new String(jar.getInputStream(mainEntry).readAllBytes(), StandardCharsets.UTF_8);

            var apocEntry = jar.getEntry("flansmodapocalypse.mod.json");
            assertNotNull(apocEntry, "flansmodapocalypse.mod.json not found");
            apocalypseModJson = new String(jar.getInputStream(apocEntry).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void mainModHasSchemaVersion() {
        assertTrue(mainModJson.contains("\"schemaVersion\": 1"),
                "Main mod must declare schemaVersion 1");
    }

    @Test
    void mainModHasCorrectId() {
        assertTrue(mainModJson.contains("\"id\": \"flansmod\""),
                "Main mod ID must be flansmod");
    }

    @Test
    void mainModHasVersionExpanded() {
        assertTrue(mainModJson.contains("\"version\": \"5.10.0\""),
                "Main mod version must be expanded to 5.10.0, not ${version}");
        assertFalse(mainModJson.contains("${version}"),
                "Version placeholder must be expanded");
    }

    @Test
    void mainModHasCorrectName() {
        assertTrue(mainModJson.contains("\"name\": \"Flan's Mod\""),
                "Main mod name must be Flan's Mod");
    }

    @Test
    void mainModHasMainEntrypoint() {
        assertTrue(mainModJson.contains("\"main\""),
                "Main mod must have main entrypoint");
        assertTrue(mainModJson.contains("com.flansmod.common.FlansMod"),
                "Main mod main entrypoint must be FlansMod");
    }

    @Test
    void mainModHasClientEntrypoint() {
        assertTrue(mainModJson.contains("\"client\""),
                "Main mod must have client entrypoint");
        assertTrue(mainModJson.contains("com.flansmod.client.FlansModClient"),
                "Main mod client entrypoint must be FlansModClient");
    }

    @Test
    void mainModHasFabricLoaderDependency() {
        assertTrue(mainModJson.contains("\"fabricloader\""),
                "Main mod must depend on fabricloader");
        assertTrue(mainModJson.contains("\">=0.19.3\""),
                "Main mod fabricloader version must be >=0.19.3");
    }

    @Test
    void mainModHasMinecraftDependency() {
        assertTrue(mainModJson.contains("\"minecraft\""),
                "Main mod must depend on minecraft");
        assertTrue(mainModJson.contains("\"~26.1.2\""),
                "Main mod minecraft version must be ~26.1.2");
    }

    @Test
    void mainModHasJavaDependency() {
        assertTrue(mainModJson.contains("\"java\""),
                "Main mod must depend on java");
        assertTrue(mainModJson.contains("\">=25\""),
                "Main mod java version must be >=25");
    }

    @Test
    void mainModHasPinnedFabricApiDependency() {
        assertTrue(mainModJson.contains("\"fabric-api\""),
                "Main mod must depend on fabric-api");
        assertTrue(mainModJson.contains("\">=0.152.1\""),
                "Main mod fabric-api must be pinned to >=0.152.1, not *");
        assertFalse(mainModJson.contains("\"fabric-api\": \"*\""),
                "Main mod fabric-api must not use wildcard");
    }

    @Test
    void mainModHasCorrectLicense() {
        assertTrue(mainModJson.contains("\"license\": \"CC BY-NC-SA 3.0\""),
                "Main mod license must be CC BY-NC-SA 3.0");
    }

    @Test
    void mainModHasAuthors() {
        assertTrue(mainModJson.contains("\"authors\""),
                "Main mod must have authors");
        assertTrue(mainModJson.contains("\"jamioflan\""),
                "Main mod author must include jamioflan");
    }

    @Test
    void apocalypseModHasSchemaVersion() {
        assertTrue(apocalypseModJson.contains("\"schemaVersion\": 1"),
                "Apocalypse mod must declare schemaVersion 1");
    }

    @Test
    void apocalypseModHasCorrectId() {
        assertTrue(apocalypseModJson.contains("\"id\": \"flansmodapocalypse\""),
                "Apocalypse mod ID must be flansmodapocalypse");
    }

    @Test
    void apocalypseModHasVersionExpanded() {
        assertTrue(apocalypseModJson.contains("\"version\": \"1.4.0\""),
                "Apocalypse mod version must be expanded to 1.4.0");
        assertFalse(apocalypseModJson.contains("${version}"),
                "Version placeholder must be expanded");
        assertFalse(apocalypseModJson.contains("${apocalypse_version}"),
                "Apocalypse version placeholder must be expanded");
    }

    @Test
    void apocalypseModHasCorrectName() {
        assertTrue(apocalypseModJson.contains("\"name\": \"Flan's Mod: Apocalypse\""),
                "Apocalypse mod name must be correct");
    }

    @Test
    void apocalypseModHasMainEntrypoint() {
        assertTrue(apocalypseModJson.contains("\"main\""),
                "Apocalypse mod must have main entrypoint");
        assertTrue(apocalypseModJson.contains("com.flansmod.apocalypse.common.FlansModApocalypse"),
                "Apocalypse mod main entrypoint must be FlansModApocalypse");
    }

    @Test
    void apocalypseModHasClientEntrypoint() {
        assertTrue(apocalypseModJson.contains("\"client\""),
                "Apocalypse mod must have client entrypoint");
        assertTrue(apocalypseModJson.contains("com.flansmod.apocalypse.client.FlansModApocalypseClient"),
                "Apocalypse mod client entrypoint must be FlansModApocalypseClient");
    }

    @Test
    void apocalypseModDependsOnFlansmod() {
        assertTrue(apocalypseModJson.contains("\"flansmod\""),
                "Apocalypse mod must depend on flansmod");
        assertTrue(apocalypseModJson.contains("\">=5.10.0\""),
                "Apocalypse mod flansmod dependency must be >=5.10.0");
    }

    @Test
    void apocalypseModHasOwnIcon() {
        assertTrue(apocalypseModJson.contains("\"icon\": \"assets/flansmodapocalypse/icon.png\""),
                "Apocalypse mod must use its own namespace icon");
        assertFalse(apocalypseModJson.contains("\"icon\": \"assets/flansmod/icon.png\""),
                "Apocalypse mod must NOT use main mod's icon");
    }

    @Test
    void apocalypseModHasPinnedFabricApi() {
        assertTrue(apocalypseModJson.contains("\"fabric-api\": \">=0.152.1\""),
                "Apocalypse mod fabric-api must be pinned");
    }

    @Test
    void apocalypseModHasCorrectLicense() {
        assertTrue(apocalypseModJson.contains("\"license\": \"CC BY-NC-SA 3.0\""),
                "Apocalypse mod license must be CC BY-NC-SA 3.0");
    }

    @Test
    void creativeTabTranslationsExist() throws Exception {
        var langPath = java.nio.file.Path.of("src/main/resources/assets/flansmod/lang/en_us.json");
        var langJson = java.nio.file.Files.readString(langPath);
        String[] tabKeys = {"itemGroup.flansmod.guns", "itemGroup.flansmod.driveables",
                "itemGroup.flansmod.parts", "itemGroup.flansmod.teams", "itemGroup.flansmod.mechas"};
        for (String key : tabKeys) {
            assertTrue(langJson.contains("\"" + key + "\""),
                    "lang file must contain creative tab key: " + key);
        }
    }

    @Test
    void itemTranslationsExist() throws Exception {
        var langPath = java.nio.file.Path.of("src/main/resources/assets/flansmod/lang/en_us.json");
        var langJson = java.nio.file.Files.readString(langPath);
        String[] items = {"gun", "bullet", "grenade", "attachment", "aa_gun",
                "plane", "vehicle", "mecha", "mecha_addon", "part", "tool",
                "team_armour", "flagpole", "op_stick", "reward_box", "shootable", "paintcan"};
        for (String item : items) {
            assertTrue(langJson.contains("\"item.flansmod." + item + "\""),
                    "lang file must contain item translation: item.flansmod." + item);
        }
    }
}
