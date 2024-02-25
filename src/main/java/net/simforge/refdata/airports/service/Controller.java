package net.simforge.refdata.airports.service;

import net.simforge.commons.misc.Geo;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import net.simforge.refdata.airports.fse.FSEAirport;
import net.simforge.refdata.airports.fse.FSEAirports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@CrossOrigin
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    @RequestMapping("/hello-world")
    public String getHelloWorld() {
        return "Hello, World!";
    }

    @GetMapping("/v1/distance")
    @ResponseBody
    public ResponseEntity<Object> getDistance(@RequestParam final String from,
                                              @RequestParam final String to) {
        final String msg = "Distance from " + from + " to " + to + ": ";

        final Airports airports = Airports.get();
        final Airport airportFrom = airports.getByIcao(from);
        final Airport airportTo = airports.getByIcao(to);

        if (airportFrom == null) {
            logger.warn(msg + "could not find 'from' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + from + "' not found");
        }
        if (airportTo == null) {
            logger.warn(msg + "could not find 'to' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + to + "' not found");
        }
        final double distance = Geo.distance(airportFrom.getCoords(), airportTo.getCoords());
        final String response = String.valueOf((int) distance);
        logger.info(msg + response + " nm");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/airport/info")
    @ResponseBody
    public ResponseEntity<AirportInfoDto> getAirportInfo(@RequestParam final String icao) {
        final Airport airport = Airports.get().getByIcao(icao);
        if (airport == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(buildAirportInfoDto(airport));
    }

    @GetMapping("/v1/airport/vicinity")
    @ResponseBody
    public ResponseEntity<List<AirportInfoDto>> getAirportsInVicinity(@RequestParam final double lat,
                                                                      @RequestParam final double lon,
                                                                      @RequestParam final double radius) {
        return ResponseEntity.ok(Airports.get()
                .findAllWithinRadius(Geo.coords(lat, lon), radius).stream()
                .map(Controller::buildAirportInfoDto)
                .collect(Collectors.toList()));
    }

    private static final FSEAirport NOT_FOUND = FSEAirport.builder()
            .withIcao("ZZZZ")
            .withName("Unknown")
            .withCity("Unknown")
            .withCountry("Unknown")
            .build();

    private static AirportInfoDto buildAirportInfoDto(Airport airport) {
        final FSEAirport fseAirport = FSEAirports.get().findByIcao(airport.getIcao()).orElse(NOT_FOUND);

        final AirportInfoDto dto = new AirportInfoDto();
        dto.setIcao(airport.getIcao());
        dto.setName(fseAirport.getName());
        dto.setCity(fseAirport.getCity());
        dto.setCountry(fseAirport.getCountry());
        dto.setCoords(airport.getCoords());
        dto.setDefaultBoundaryRadius(airport.getDefaultBoundaryRadius());
        dto.setBoundaryType(airport.getBoundaryType().name());
        dto.setBoundaryData(airport.getBoundaryData());
        return dto;
    }
}
