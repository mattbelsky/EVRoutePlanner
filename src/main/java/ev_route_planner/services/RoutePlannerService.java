package ev_route_planner.services;

import ev_route_planner.exceptions.RouteNotFoundException;
import ev_route_planner.mappers.RoutePlannerMapper;
import ev_route_planner.model.RouteQueryData;
import ev_route_planner.model.directions.Directions;
import ev_route_planner.model.directions.OverviewPolyline;
import ev_route_planner.model.geolocation.Location;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class RoutePlannerService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    OpenChargeMapService openChargeMapService;

    /**
     * The Google Directions API is queried with a set of starting and ending coordinates, which plans the optimal route
     * and returns an encoded polyline. The polyline is decoded in another method into an array of coordinates, each of
     * which is used to query the OpenChargeMaps API for EV charging sites nearby.
     * @param routeQueryData -- contains all the data needed to query for charging stations along a route
     * @return an ArrayList of ChargingSite objects found along the route
     */
    public ArrayList<ChargingSite> findRoute(RouteQueryData routeQueryData) throws RouteNotFoundException {

        /*
         * Initial defaults:
         *   distance = 1
         *   distanceUnit = 2 // 1 = km, 2 = miles
         *   levelId = 3
         *   maxResults = 3
         */
        double startLat = routeQueryData.getStartLat();
        double startLng = routeQueryData.getStartLng();
        double endLat = routeQueryData.getEndLat();
        double endLng = routeQueryData.getEndLng();
        double distance = routeQueryData.getDistance();
        int distanceUnit = routeQueryData.getDistanceUnit();
        int levelId = routeQueryData.getLevelId();
        int maxResults = routeQueryData.getMaxResults();
        String key = routeQueryData.getApiKey();

        String query = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLat + "," + startLng +
                "&destination=" + endLat + "," + endLng +
                "&key=" + key;
        Directions directions = restTemplate.getForObject(query, Directions.class);

        // Gets, decodes, and compiles the encoded polyline into an ArrayList of coordinates.
        String overviewPolyline;
        try {
            overviewPolyline = directions.getRoutes()[0]
                    .getOverview_polyline()
                    .getPoints();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RouteNotFoundException();
        }
        ArrayList<Location> coordsAlongRoute = decodePolyline(overviewPolyline);

        // Finds an array of charging sites within a specified distance of each coordinate set and compiles the arrays
        // into an ArrayList.
        ArrayList<ChargingSite[]> sitesAlongRouteRepeatingElements = new ArrayList();
        for (Location point : coordsAlongRoute) {
            ChargingSite[] sitesNearCoords = openChargeMapService.searchByLatLong(point.getLat(), point.getLng(),
                    distance, distanceUnit, levelId, maxResults);
            sitesAlongRouteRepeatingElements.add(sitesNearCoords);
        }
        ArrayList<ChargingSite> sitesAlongRoute = removeRepeatingElements(sitesAlongRouteRepeatingElements);

        return sitesAlongRoute;
    }

    /**
     * Removes the repeating elements in an ArrayList of arrays of ChargingSite objects.
     * @param sitesAlongRouteRepeatingElements -- the ArrayList of ChargingSite arrays
     * @return an ArrayList of sites along the route
     */
    public ArrayList<ChargingSite> removeRepeatingElements(ArrayList<ChargingSite[]> sitesAlongRouteRepeatingElements) {

        // the ArrayList that will be returned
        ArrayList<ChargingSite> sitesAlongRoute = new ArrayList();

        // Adds each element of each array of charging site objects to the ArrayList that will be returned.
        for (ChargingSite[] sitesNearCoords : sitesAlongRouteRepeatingElements) {
            for (ChargingSite site : sitesNearCoords) {
                sitesAlongRoute.add(site);
            }
        }

        // Compares each element of the ArrayList to be returned and removes any repeating elements.
        for (int i = 0; i < sitesAlongRoute.size() - 1; i++) {
            for (int j = i + 1; j < sitesAlongRoute.size(); j++) {
                if (sitesAlongRoute.get(i) == sitesAlongRoute.get(j)) {
                    sitesAlongRoute.remove(j);
                }
            }
        }

        return sitesAlongRoute;
    }

    /**
     * Decodes the encoded polyline describing the geographical route into an ArrayList of coordinates.
     * @param encoded -- the encoded polyline
     * @return the decoded ArrayList of coordinates
     */
    public ArrayList<Location> decodePolyline(String encoded) {

        // Replaces every case of "\\\\" by removing 2 slashes, thereby avoiding an IndexOutOfBoundsException.
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
