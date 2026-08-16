package com.flansmod.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentPackFixtureTest {

    private static final Path TEST_PACK = Path.of("src/test/resources/test_pack");

    @Test
    void testPackDirectoryExists() {
        assertTrue(Files.exists(TEST_PACK), "test_pack directory must exist");
        assertTrue(Files.isDirectory(TEST_PACK), "test_pack must be a directory");
    }

    @Test
    void gunsFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("guns/AK47.txt");
        assertTrue(Files.exists(f), "guns/AK47.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "guns/AK47.txt must not be empty");
    }

    @Test
    void gunsFixtureHasKeyValueFormat() throws Exception {
        List<String> lines = Files.readAllLines(TEST_PACK.resolve("guns/AK47.txt"));
        boolean hasNameOrShortName = lines.stream().anyMatch(
                l -> l.toLowerCase().startsWith("name ") || l.toLowerCase().startsWith("shortname "));
        assertTrue(hasNameOrShortName, "Gun fixture must contain Name or ShortName key");
    }

    @Test
    void bulletsFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("bullets/AK47Ammo.txt");
        assertTrue(Files.exists(f), "bullets/AK47Ammo.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "bullets/AK47Ammo.txt must not be empty");
    }

    @Test
    void bulletsFixtureHasKeyValueFormat() throws Exception {
        List<String> lines = Files.readAllLines(TEST_PACK.resolve("bullets/AK47Ammo.txt"));
        boolean hasKey = lines.stream().anyMatch(l -> l.contains(" ") && !l.trim().startsWith("//"));
        assertTrue(hasKey, "Bullet fixture must contain key=value pairs");
    }

    @Test
    void vehiclesFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("vehicles/Abrams.txt");
        assertTrue(Files.exists(f), "vehicles/Abrams.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "vehicles/Abrams.txt must not be empty");
    }

    @Test
    void planesFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("planes/A10.txt");
        assertTrue(Files.exists(f), "planes/A10.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "planes/A10.txt must not be empty");
    }

    @Test
    void teamsFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("teams/Criminals.txt");
        assertTrue(Files.exists(f), "teams/Criminals.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "teams/Criminals.txt must not be empty");
    }

    @Test
    void classesFixtureExists() throws Exception {
        Path f = TEST_PACK.resolve("classes/CriminalAssault.txt");
        assertTrue(Files.exists(f), "classes/CriminalAssault.txt fixture must exist");
        List<String> lines = Files.readAllLines(f);
        assertFalse(lines.isEmpty(), "classes/CriminalAssault.txt must not be empty");
    }

    @Test
    void allFixtureDirectoriesExist() {
        String[] dirs = {"guns", "bullets", "vehicles", "planes", "teams", "classes"};
        for (String dir : dirs) {
            Path d = TEST_PACK.resolve(dir);
            assertTrue(Files.exists(d) && Files.isDirectory(d),
                    "Fixture directory must exist: " + dir);
            try (var stream = Files.list(d)) {
                assertTrue(stream.findAny().isPresent(),
                        "Fixture directory must contain at least one file: " + dir);
            } catch (IOException e) {
                fail("Failed to list directory " + dir + ": " + e.getMessage());
            }
        }
    }
}
