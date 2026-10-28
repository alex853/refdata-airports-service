package net.simforge.refdata.airports.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransitionStorageTests {
    @TempDir
    File tempDir;

    @Test
    void unknownAirportHasNoValues() {
        final TransitionDto dto = new TransitionStorage(new File(tempDir, "transitions.json")).get("EGLL");

        assertNull(dto.getTa());
        assertNull(dto.getTl());
    }

    @Test
    void mostFrequentValueWins() {
        final TransitionStorage storage = new TransitionStorage(new File(tempDir, "transitions.json"));

        storage.register("EGLL", 5000, null);
        storage.register("EGLL", 6000, null);
        final TransitionDto dto = storage.register("EGLL", 6000, 7000);

        assertEquals(6000, dto.getTa());
        assertEquals(7000, dto.getTl());
    }

    @Test
    void mostRecentValueWinsOnTie() throws InterruptedException {
        final TransitionStorage storage = new TransitionStorage(new File(tempDir, "transitions.json"));

        storage.register("EGLL", 5000, null);
        Thread.sleep(5);
        final TransitionDto dto = storage.register("EGLL", 6000, null);

        assertEquals(6000, dto.getTa());
        assertNull(dto.getTl());
    }

    @Test
    void dataSurvivesFlushAndReload() throws IOException {
        final File file = new File(tempDir, "sub/transitions.json");
        final TransitionStorage storage = new TransitionStorage(file);
        storage.register("EGLL", 5000, 6000);
        storage.register("EGLL", 5000, null);
        storage.register("EGLL", 6000, null);
        storage.flush();

        final TransitionStorage reloaded = new TransitionStorage(file);
        reloaded.load();

        final TransitionDto dto = reloaded.get("EGLL");
        assertEquals(5000, dto.getTa());
        assertEquals(6000, dto.getTl());
    }
}
