package net.simforge.refdata.airports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import net.simforge.commons.legacy.misc.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Accumulates transition altitude (TA) and transition level (TL) statistics per airport.
 * Each observed value has a counter, the most frequent value wins (the most recent one on a tie).
 * <p>
 * Data is kept in memory and flushed to {airports.storage.root}/transitions.json periodically and on shutdown.
 */
@Component
public class TransitionStorage {
    private static final Logger logger = LoggerFactory.getLogger(TransitionStorage.class);

    private static final String DEFAULT_STORAGE_ROOT = "~/simforge/airports-storage";
    private static final String FILENAME = "transitions.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File file;
    private final Map<String, AirportTransitions> airports = new HashMap<>();
    private boolean dirty;

    @Autowired
    public TransitionStorage() {
        this(getDefaultFile());
    }

    TransitionStorage(final File file) {
        this.file = file;
    }

    @PostConstruct
    synchronized void load() throws IOException {
        if (!file.exists()) {
            logger.info("Transitions file {} does not exist, starting empty", file);
            return;
        }

        final Map<String, AirportTransitions> loaded = objectMapper.readValue(file, new TypeReference<Map<String, AirportTransitions>>() {
        });
        airports.putAll(loaded);
        logger.info("Transitions loaded from {}, {} airports", file, airports.size());
    }

    public synchronized TransitionDto register(final String icao, final Integer ta, final Integer tl) {
        final AirportTransitions transitions = airports.computeIfAbsent(icao, key -> new AirportTransitions());
        final long now = System.currentTimeMillis();

        if (ta != null) {
            transitions.getTa().computeIfAbsent(ta, key -> new Counter()).hit(now);
            dirty = true;
        }
        if (tl != null) {
            transitions.getTl().computeIfAbsent(tl, key -> new Counter()).hit(now);
            dirty = true;
        }

        return toDto(transitions);
    }

    public synchronized TransitionDto get(final String icao) {
        final AirportTransitions transitions = airports.get(icao);
        return transitions != null ? toDto(transitions) : new TransitionDto();
    }

    @Scheduled(fixedDelay = 30000)
    @PreDestroy
    synchronized void flush() throws IOException {
        if (!dirty) {
            return;
        }

        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        final File tmpFile = new File(file.getParentFile(), FILENAME + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpFile, airports);
        Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        dirty = false;
        logger.info("Transitions flushed to {}", file);
    }

    private static TransitionDto toDto(final AirportTransitions transitions) {
        return new TransitionDto(pickBest(transitions.getTa()), pickBest(transitions.getTl()));
    }

    private static Integer pickBest(final Map<Integer, Counter> counters) {
        Integer best = null;
        Counter bestCounter = null;
        for (Map.Entry<Integer, Counter> entry : counters.entrySet()) {
            final Counter counter = entry.getValue();
            if (bestCounter == null
                    || counter.getCount() > bestCounter.getCount()
                    || (counter.getCount() == bestCounter.getCount() && counter.getLast() > bestCounter.getLast())) {
                best = entry.getKey();
                bestCounter = counter;
            }
        }
        return best;
    }

    private static File getDefaultFile() {
        final String storageRootStr = Settings.get("airports.storage.root");
        String root = storageRootStr != null ? storageRootStr : DEFAULT_STORAGE_ROOT;
        if (root.startsWith("~")) {
            root = System.getProperty("user.home") + root.substring(1);
        }
        return new File(root, FILENAME);
    }

    @Data
    public static class AirportTransitions {
        private Map<Integer, Counter> ta = new HashMap<>();
        private Map<Integer, Counter> tl = new HashMap<>();
    }

    @Data
    public static class Counter {
        private int count;
        private long last; // epoch millis

        void hit(final long now) {
            count++;
            last = now;
        }
    }
}
