package net.simforge.refdata.airports.service;

import net.simforge.commons.misc.Geo;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import net.simforge.refdata.airports.AirportsStorage;
import net.simforge.refdata.airports.boundary.DefaultBoundary;
import net.simforge.refdata.airports.fse.FSEAirport;
import net.simforge.refdata.airports.fse.FSEAirports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
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
        final Optional<Airport> airportFrom = airports.findByIcao(from);
        final Optional<Airport> airportTo = airports.findByIcao(to);

        if (!airportFrom.isPresent()) {
            logger.warn(msg + "could not find 'from' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + from + "' not found");
        }
        if (!airportTo.isPresent()) {
            logger.warn(msg + "could not find 'to' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + to + "' not found");
        }
        final double distance = Geo.distance(airportFrom.get().getCoords(), airportTo.get().getCoords());
        final String response = String.valueOf((int) distance);
        logger.info(msg + response + " nm");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/airport/info")
    @ResponseBody
    public ResponseEntity<AirportInfoDto> getAirportInfo(@RequestParam final String icao) {
        return Airports.get().findByIcao(icao)
                .map(value -> ResponseEntity.ok(buildAirportInfoDto(value)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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

    @PostMapping("/v1/airport/boundary")
    @ResponseBody
    public ResponseEntity<Object> updateAirportBoundary(@RequestParam final String icao,
                                                        @RequestParam final String type,
                                                        @RequestParam final String data) throws IOException {
        final Optional<Airport> airport = Airports.get().findByIcao(icao);
        if (!airport.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + icao + "' not found");
        }

        final Properties properties = new Properties();
        properties.put("type", type);
        properties.put("data", data);
        AirportsStorage.saveBoundary(icao, properties);

        Airports.reset(); // hard-reset, it can be done faster

        return ResponseEntity.ok().build();
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
        dto.setDefaultBoundaryRadius(DefaultBoundary.calcDefaultBoundaryRadius(airport.getRunwaySize()));
        final Properties boundaryInfo = airport.getBoundary().asProperties();
        dto.setBoundaryType(boundaryInfo.getProperty("type"));
        dto.setBoundaryData(boundaryInfo.getProperty("data"));
        return dto;
    }
}
