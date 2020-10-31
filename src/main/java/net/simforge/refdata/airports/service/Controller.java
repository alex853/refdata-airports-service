package net.simforge.refdata.airports.service;

import net.simforge.commons.misc.Geo;
import net.simforge.refdata.airports.Airport;
import net.simforge.refdata.airports.Airports;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/")
@CrossOrigin
public class Controller {
    @RequestMapping("/hello-world")
    public String getHelloWorld() {
        return "Hello, World!";
    }

    @GetMapping("/v1/distance")
    @ResponseBody
    public ResponseEntity<Object> getDistance(@RequestParam(value = "from", required = true) String from, @RequestParam(value = "to", required = true) String to) {
        Airports airports = Airports.get();
        Airport airportFrom = airports.getByIcao(from);
        Airport airportTo = airports.getByIcao(to);

        if (airportFrom == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + from + "' not found");
        }
        if (airportTo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Airport '" + to + "' not found");
        }
        double distance = Geo.distance(airportFrom.getCoords(), airportTo.getCoords());
        return ResponseEntity.ok(String.valueOf((int) distance));
    }
}
