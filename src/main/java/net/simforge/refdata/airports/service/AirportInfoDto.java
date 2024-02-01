package net.simforge.refdata.airports.service;

import lombok.Data;

@Data
public class AirportInfoDto {
    private String icao;
    private String name;
    private String city;
    private String country;
}
