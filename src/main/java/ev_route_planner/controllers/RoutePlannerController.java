package ev_route_planner.controllers;

import ev_route_planner.exceptions.ControllerAdviceClass;
import ev_route_planner.exceptions.KeyDoesNotExistException;
import ev_route_planner.exceptions.RateLimitException;
import ev_route_planner.exceptions.RouteNotFoundException;
import ev_route_planner.model.GeneralResponse;
import ev_route_planner.model.directions.OverviewPolyline;
import ev_route_planner.model.open_charge_map.ChargingSite;
import ev_route_planner.services.RoutePlannerService;
import ev_route_planner.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

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

    @Autowired
    ControllerAdviceClass controllerAdviceClass;

    /**
     * The Google Directions API is queried with a set of starting and ending coordinates, which plans the optimal route
     * and returns an encoded polyline. The polyline is decoded into an array of coordinates, each of which is used to
     * query the OpenChargeMaps API for EV charging sites nearby. Finally, the repeated sites are eliminated and the
     * remainder are compiled into an array of charging sites along the route.
     * @param startLat
     * @param startLng
     * @param endLat
     * @param endLng
     * @param apiKey a key required to access this function -- because multiple server calls can be expensive
     * @return an array of ChargingSites along the route
     */
    @RequestMapping("/go")
    public GeneralResponse getPolyline(@RequestParam(value = "startlat") double startLat,
                                       @RequestParam(value = "startlng") double startLng,
                                       @RequestParam(value = "endlat") double endLat,
                                       @RequestParam(value = "endlng") double endLng,
                                       @RequestParam(value = "apikey") String apiKey)
            throws RouteNotFoundException, KeyDoesNotExistException, RateLimitException {
        if (!userService.keyExists(apiKey)) throw new KeyDoesNotExistException();
        if (userService.apiCallsExceeded(apiKey)) throw new RateLimitException();
        ArrayList<ChargingSite> sites = routePlannerService.findRoute(startLat, startLng, endLat, endLng);
        GeneralResponse response = new GeneralResponse(sites);
        userService.addApiCall(apiKey);
        return response;
    }


}
