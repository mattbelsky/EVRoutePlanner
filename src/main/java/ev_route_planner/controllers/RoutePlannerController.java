package ev_route_planner.controllers;

import ev_route_planner.model.directions.OverviewPolyline;
import ev_route_planner.model.open_charge_map.ChargingSite;
import ev_route_planner.services.RoutePlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/routeplanner")
public class RoutePlannerController {

    @Autowired
    RoutePlannerService routePlannerService;

    // Allows trip to be planned through the URL
    @RequestMapping("/go")
    public ArrayList<ChargingSite> getPolyline(@RequestParam(value = "startlat") double startLat,
                                               @RequestParam(value = "startlng") double startLng,
                                               @RequestParam(value = "endlat") double endLat,
                                               @RequestParam(value = "endlng") double endLng) {
        return routePlannerService.planRoute(startLat, startLng, endLat, endLng);
    }
}
