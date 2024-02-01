package net.simforge.refdata.airports.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FSEconomyAirports {

    private final Map<String, Airport> data = new HashMap<>();

    private FSEconomyAirports() {

    }

    private static FSEconomyAirports instance;

    public synchronized static FSEconomyAirports get() {
        if (instance != null) {
            return instance;
        }

        final InputStream in = FSEconomyAirports.class.getResourceAsStream("/fseconomy-icaodata.csv");
        final String content;
        try {
            content = IOHelper.readInputStream(in);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read airport data", e);
        }
        final Csv csv = Csv.fromContent(content);

        final FSEconomyAirports _instance = new FSEconomyAirports();
        for (int row = 0; row < csv.rowCount(); row++) {
            final String icao = csv.value(row, "icao");
            final String name = csv.value(row, "name");
            final String city = csv.value(row, "city");
            final String country = csv.value(row, "country");
            _instance.data.put(icao, new Airport(icao, name, city, country));
        }

        instance = _instance;

        return instance;
    }

    public Optional<Airport> findByIcao(final String icao) {
        return Optional.ofNullable(data.get(icao));
    }

    @Getter
    @RequiredArgsConstructor
    public static class Airport {
        private final String icao;
        private final String name;
        private final String city;
        private final String country;
    }
}
