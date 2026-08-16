package com.flansmod.common.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestContentPackTest {

    @Test
    void getTestPackPathReturnsExistingDirectory() {
        Path path = TestContentPack.getTestPackPath();
        assertNotNull(path);
        assertTrue(Files.exists(path), "test_pack path must exist");
        assertTrue(Files.isDirectory(path), "test_pack must be a directory");
    }

    @Test
    void getFixtureReturnsCorrectPath() {
        Path fixture = TestContentPack.getFixture("guns", "AK47.txt");
        assertTrue(fixture.endsWith(Path.of("test_pack/guns/AK47.txt")),
                "Fixture path must end with correct relative path");
    }

    @Test
    void fixtureExistsReturnsTrueForExistingFixture() {
        assertTrue(TestContentPack.fixtureExists("guns", "AK47.txt"),
                "Must report true for existing fixture");
    }

    @Test
    void fixtureExistsReturnsFalseForMissingFixture() {
        assertFalse(TestContentPack.fixtureExists("nonexistent", "missing.txt"),
                "Must report false for missing fixture");
    }

    @Test
    void readFixtureReturnsNonEmptyList() {
        List<String> lines = TestContentPack.readFixture("guns", "AK47.txt");
        assertNotNull(lines, "Read fixture must not return null");
        assertFalse(lines.isEmpty(), "Read fixture must return non-empty lines");
    }

    @Test
    void readFixtureReturnsEmptyListForMissingFile() {
        List<String> lines = TestContentPack.readFixture("nope", "nope.txt");
        assertNotNull(lines, "Read missing fixture must not return null");
        assertTrue(lines.isEmpty(), "Read missing fixture must return empty list");
    }
}
