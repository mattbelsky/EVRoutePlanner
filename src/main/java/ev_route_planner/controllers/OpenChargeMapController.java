package ev_route_planner.controllers;

import ev_route_planner.exceptions.ControllerAdviceClass;
import ev_route_planner.model.GeneralResponse;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ev_route_planner.services.OpenChargeMapService;

import java.io.IOException;

/**
 * This controller allows the user to get a list of EV charging sites from one specified location.
 * It currently allows querying by country, by latitude and longitude, as
 * well as by current location which the application will detect.
 */
@RestController
@RequestMapping("/openchargemap")
public class OpenChargeMapController {

    @Autowired
    OpenChargeMapService openChargeMapService;

    @Autowired
    ControllerAdviceClass controllerAdviceClass;

    /**
     * Queries by country
     * @param countryCode ISO country code (i.e. US, CA, ID, UK)
     * @param maxResults the maximum number of results to return
     * @return an array objects containing details about each charging site
     */
    @RequestMapping("/bycountry")
    public GeneralResponse searchByCountry(@RequestParam(value = "q") String countryCode,
                                          @RequestParam(value = "maxresults", defaultValue = "50") int maxResults) {
        ChargingSite[] chargingSites = openChargeMapService.searchByCountry(countryCode, maxResults);
        GeneralResponse response = new GeneralResponse(chargingSites);
        return response;
    }

    /**
     * Searches by latitude & longitude & distance in miles or km from it.
     * @param latitude
     * @param longitude
     * @param distance radius to search
     * @param distanceUnit 1 = km, 2 = mi
     * @param levelID charging levels (1 = home outlet, 2 & 3 = faster charging)
     * @param maxResults the maximum number of results to return
     * @return an array objects containing details about each charging site
     */
    @RequestMapping("/bylatlong")
    public GeneralResponse searchByLatLong(@RequestParam(value = "latitude") double latitude,
                                          @RequestParam(value = "longitude") double longitude,
                                          @RequestParam(value = "distance") double distance,
                                          @RequestParam(value = "distanceunit") int distanceUnit,
                                          @RequestParam(value = "levelid") int levelID,
                                          @RequestParam(value = "maxresults") int maxResults) {
        ChargingSite[] chargingSites = openChargeMapService.searchByLatLong(latitude, longitude, distance, distanceUnit,
                levelID, maxResults);
        GeneralResponse response = new GeneralResponse(chargingSites);
        return response;
    }

    /**
     * Gets user's approximate latitude and longitude and shows charging stations within a predefined radius.
     * Although the request in the service class is using the postForObject() method, this is a GET request apparently.
     * @return an array objects containing details about each charging site
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/nearme")
    public GeneralResponse searchNearMe() throws IOException {
        ChargingSite[] sitesNearMe = openChargeMapService.searchNearMe();
        GeneralResponse response = new GeneralResponse(sitesNearMe);
        return response;
    }
}