package ev_route_planner.controllers;

import ev_route_planner.exceptions.KeyDoesNotExistException;
import ev_route_planner.exceptions.RateLimitException;
import ev_route_planner.exceptions.RouteNotFoundException;
import ev_route_planner.model.RouteQueryData;
import ev_route_planner.model.open_charge_map.ChargingSite;
import ev_route_planner.services.RoutePlannerService;
import ev_route_planner.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Finds a list of EV charging sites along a specified route.
 */
@RestController
@RequestMapping("/routeplanner")
public class RoutePlannerController {

    @Autowired
    RoutePlannerService routePlannerService;

    @Autowired
    UserService userService;

    @Value("${googlemaps.key}")
    String googleMapsApiKey;

    /**
     * The Google Directions API is queried with a set of starting and ending coordinates, which plans the optimal route
     * and returns an encoded polyline. The polyline is decoded into an array of coordinates, each of which is used to
     * query the OpenChargeMaps API for EV charging sites nearby. Finally, the repeated sites are eliminated and the
     * remainder are compiled into an array of charging sites along the route.
     * @param startLat
     * @param startLng
     * @param endLat
     * @param endLng
     * @param distance
     * @param distanceUnit -- 1 = km, 2 = miles
     * @param levelId -- 1, 2, or 3
     * @return a response wrapper containing an array list of ChargingSite objects containing details about each charging site
     * @throws RouteNotFoundException
     * @throws KeyDoesNotExistException
     * @throws RateLimitException
     */
    @GetMapping("/go")
    public ArrayList<ChargingSite> getChargingSites(@RequestParam("start_lat") double startLat,
                                                    @RequestParam("start_lng") double startLng,
                                                    @RequestParam("end_lat") double endLat,
                                                    @RequestParam("end_lng") double endLng,
                                                    @RequestParam("distance") double distance,
                                                    @RequestParam("distance_unit") int distanceUnit,
                                                    @RequestParam("level_id") int levelId,
                                                    @RequestParam("max_results") int maxResults)
            throws RouteNotFoundException, KeyDoesNotExistException, RateLimitException {


        RouteQueryData routeQueryData = new RouteQueryData(
                startLat,
                startLng,
                endLat,
                endLng,
                distance,
                distanceUnit,
                levelId,
                maxResults,
                googleMapsApiKey
        );

//        // API key & rate limit validation -- needs to query for user key, not google maps api key
//        if (!userService.keyExists(apiKey)) throw new KeyDoesNotExistException();
//        if (userService.apiCallsExceeded(apiKey)) throw new RateLimitException();

        CompletableFuture<ArrayList<ChargingSite>> future = routePlannerService.getChargingSites(routeQueryData);
        ArrayList<ChargingSite> sites = null;
        try {
            sites = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

//        // Increments the total number of API calls made
//        userService.addApiCall(apiKey);

        return sites;
    }

    @GetMapping("/polyline")
    public String getRoutePolyline(@RequestParam("start_lat") double startLat,
                                   @RequestParam("start_lng") double startLng,
                                   @RequestParam("end_lat") double endLat,
                                   @RequestParam("end_lng") double endLng) throws RouteNotFoundException {

        RouteQueryData routeQueryData = new RouteQueryData(
                startLat,
                startLng,
                endLat,
                endLng,
                0,
                0,
                0,
                0,
                googleMapsApiKey
        );
        return routePlannerService.getRoutePolyline(routeQueryData);
    }
}
