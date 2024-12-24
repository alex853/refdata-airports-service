package net.simforge.refdata.airports.service;

import lombok.Data;

@Data
public class AirportBoundaryDto {
    private float defaultRadius;
    private String type;
    private String data;
}
