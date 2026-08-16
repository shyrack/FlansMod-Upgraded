package com.flansmod.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class TestContentPack {

    private static final Path TEST_PACK_PATH = Path.of("src/test/resources/test_pack");

    public static Path getTestPackPath() {
        return TEST_PACK_PATH;
    }

    public static Path getFixture(String category, String filename) {
        return TEST_PACK_PATH.resolve(category).resolve(filename);
    }

    public static List<String> readFixture(String category, String filename) {
        try {
            return Files.readAllLines(getFixture(category, filename));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static boolean fixtureExists(String category, String filename) {
        return Files.exists(getFixture(category, filename));
    }
}
