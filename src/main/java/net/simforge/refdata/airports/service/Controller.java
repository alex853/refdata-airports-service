package net.simforge.refdata.airports.service;

import net.simforge.commons.misc.Geo;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<Object> getDistance(@RequestParam String from,
                                              @RequestParam String to) {
        String msg = "Distance from " + from + " to " + to + ": ";

        Airports airports = Airports.get();
        Airport airportFrom = airports.getByIcao(from);
        Airport airportTo = airports.getByIcao(to);

        if (airportFrom == null) {
            logger.warn(msg + "could not find 'from' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + from + "' not found");
        }
        if (airportTo == null) {
            logger.warn(msg + "could not find 'to' airport");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + to + "' not found");
        }
        double distance = Geo.distance(airportFrom.getCoords(), airportTo.getCoords());
        String response = String.valueOf((int) distance);
        logger.info(msg + response + " nm");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/airport/info")
    @ResponseBody
    public ResponseEntity<AirportInfoDto> getAirportInfo(@RequestParam String icao) {
        Airport airport = Airports.get().getByIcao(icao);
        if (airport == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        FSEconomyAirports.Airport notFound = new FSEconomyAirports.Airport("ZZZZ", "Unknown", "Unknown", "Unknown");

        FSEconomyAirports.Airport fseAirport = FSEconomyAirports.get().findByIcao(icao).orElse(notFound);

        final AirportInfoDto dto = new AirportInfoDto();
        dto.setIcao(icao);
        dto.setName(fseAirport.getName());
        dto.setCity(fseAirport.getCity());
        dto.setCountry(fseAirport.getCountry());
        return ResponseEntity.ok(dto);
    }
}
