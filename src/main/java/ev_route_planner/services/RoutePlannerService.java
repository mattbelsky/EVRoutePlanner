package ev_route_planner.services;

import ev_route_planner.mappers.RoutePlannerMapper;
import ev_route_planner.model.directions.Directions;
import ev_route_planner.model.directions.OverviewPolyline;
import ev_route_planner.model.geolocation.Location;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoutePlannerService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    OpenChargeMapService openChargeMapService;

    @Autowired
    RoutePlannerMapper routePlannerMapper;

    /*  Ultimately should return to RoutePlannerController an array of ChargingSites
        Need to decode polyline into array of coords
        For each coord set, call OpenChargeMapService obj.searchByLatLong, specify a distance
        Need new method to compare arrays
            Create new ArrayList that appends each array onto itself
            If it contains identical ChargingSite objects, remove all but the first
     */

    public ArrayList<ChargingSite> planRoute(double startLat, double startLng, double endLat, double endLng) {

        /*  What can go wrong?
                Parameters not entered properly (ie incomplete, not doubles)
                No route exists
         */
        String key = routePlannerMapper.getKey(2);
        String query = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLat + "," + startLng +
                "&destination=" + endLat + "," + endLng +
                "&key=" + key;
        Directions directions = restTemplate.getForObject(query, Directions.class);

        // Decodes and compiles the encoded polyline into an ArrayList of coordinates.
        String overviewPolyline = directions.getRoutes()[0].getOverview_polyline().getPoints();
        ArrayList<Location> coordsAlongRoute = decodePolyline(overviewPolyline);

        // Finds an array of charging sites within a specified distance of each coordinate set and compiles the arrays
        // into an ArrayList.
        /************* THESE VARIABLES SHOULD ULTIMATELY BE SET SOMEWHERE ELSE ***********/
        ArrayList<ChargingSite[]> sitesAlongRouteRepeatingElements = new ArrayList();
        double distance = 1;
        int distanceUnit = 2; // miles

        /************* WANT 2 OR 3! NOT JUST ONE OR THE OTHER **************/
        int levelID = 3;
        int maxResults = 3;
        for (Location point : coordsAlongRoute) {
            ChargingSite[] sitesNearCoords = openChargeMapService.searchByLatLong(point.getLat(), point.getLng(),
                    distance, distanceUnit, levelID, maxResults);
            sitesAlongRouteRepeatingElements.add(sitesNearCoords);
        }

        // Compares arrays within ArrayList to ensure no elements with any array are repeated.
            /*  create new ArrayList master
                for each array in the ArrayList sitesRepeating
                    for each element in array
                        add to master
                for i = 0 through master < master.size - 1
                    for j = i + 1 through < master.size
                        if element = master.get(i)
                            master.remove(j)
             */
        ArrayList<ChargingSite> sitesAlongRoute = new ArrayList();
        for (ChargingSite[] sitesNearCoords : sitesAlongRouteRepeatingElements) {
            for (ChargingSite site : sitesNearCoords) {
                sitesAlongRoute.add(site);
            }
        }
        for (int i = 0; i < sitesAlongRoute.size() - 1; i++) {
            for (int j = i + 1; j < sitesAlongRoute.size(); j++) {
                if (sitesAlongRoute.get(i) == sitesAlongRoute.get(j)) {
                    sitesAlongRoute.remove(j);
                }
            }
        }

        return sitesAlongRoute;
    }

    // Decodes the encoded polyline describing the geographical route.
    public ArrayList<Location> decodePolyline(String encoded) {

        // Replaces every case of "\\\\" by removing 2 slashes.
        encoded = encoded.replaceAll("\\\\\\\\", "\\\\");
        ArrayList<Location> poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            Location point = new Location((((double) lat / 1E5)), ((double) lng / 1E5));
            poly.add(point);
        }

        return poly;
    }
}
