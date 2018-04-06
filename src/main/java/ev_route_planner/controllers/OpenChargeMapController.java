package ev_route_planner.controllers;

import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ev_route_planner.services.OpenChargeMapService;

import java.io.IOException;

@RestController
@RequestMapping("/openchargemap")
public class OpenChargeMapController {

    @Autowired
    OpenChargeMapService openChargeMapService;

    // Searches by country
    @RequestMapping("/bycountry")
    public ChargingSite[] searchByCountry(@RequestParam(value = "q") String countryCode,
                                          @RequestParam(value = "maxresults", defaultValue = "50") int maxResults) {
        ChargingSite[] chargingSites = openChargeMapService.searchByCountry(countryCode, maxResults);
        return chargingSites;
    }

    // Searches by latitude & longitude & distance in miles or km from it
    @RequestMapping("/bylatlong")
    public ChargingSite[] searchByLatLong(@RequestParam(value = "latitude") double latitude,
                                          @RequestParam(value = "longitude") double longitude,
                                          @RequestParam(value = "distance") double distance,
                                          @RequestParam(value = "distanceunit") int distanceUnit,
                                          @RequestParam(value = "levelid") int levelID,
                                          @RequestParam(value = "maxresults") int maxResults) {
        ChargingSite[] chargingSites = openChargeMapService.searchByLatLong(latitude, longitude, distance, distanceUnit,
                levelID, maxResults);
        return chargingSites;
    }

    // Gets user's approximate latitude and longitude and shows charging stations within a predefined radius
    // Although the request in the service class is using the postForObject() method, this is a GET request apparently.
    @RequestMapping(method = RequestMethod.GET, value = "/nearme")
    public ChargingSite[] searchNearMe() throws IOException {
        ChargingSite[] sitesNearMe = openChargeMapService.searchNearMe();
        return sitesNearMe;
    }
}