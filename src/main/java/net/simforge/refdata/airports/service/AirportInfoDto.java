package net.simforge.refdata.airports.service;

import lombok.Data;
import net.simforge.commons.misc.Geo;

@Data
public class AirportInfoDto {
    private String icao;
    private String name;
    private String city;
    private String country;
    private Geo.Coords coords;
    private float defaultBoundaryRadius;
}
