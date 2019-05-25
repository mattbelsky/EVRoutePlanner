package ev_route_planner.services;

import ev_route_planner.exceptions.RouteNotFoundException;
import ev_route_planner.model.RouteQueryData;
import ev_route_planner.model.directions.Directions;
import ev_route_planner.model.geolocation.Location;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static ev_route_planner.Constants.CHARGING_SITES_EXECUTOR;
import static ev_route_planner.Constants.QUERY_EXECUTOR;

@Service
public class RoutePlannerService {

    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM
    private static final int DISTANCE_ONE_DEGREE_LATITUDE = 111; // Approx distance between one degree difference in latitude in km.

    double[] distBtwnOneLngAtEachLat;
    Logger logger = LoggerFactory.getLogger(RoutePlannerService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    OpenChargeMapService openChargeMapService;

    @Autowired
    @Qualifier(QUERY_EXECUTOR)
    ThreadPoolTaskExecutor executor;

    /**
     * The Google Directions API is queried with a set of starting and ending coordinates, which plans the optimal route
     * and returns an encoded polyline. The polyline is decoded in another method into an array of coordinates, each of
     * which is filtered according to its distance from the previous coordinate and is used to query the OpenChargeMaps API
     * for EV charging sites nearby.
     * @param routeQueryData -- contains all the data needed to query for charging stations along a route
     * @return an ArrayList of ChargingSite objects found along the route
     */
    @Async(CHARGING_SITES_EXECUTOR)
    public CompletableFuture<ArrayList<ChargingSite>> getChargingSites(RouteQueryData routeQueryData) throws RouteNotFoundException {

        logger.info("getChargingSites() executing...");
        ArrayList<ChargingSite[]> listSitesUnfiltered = new ArrayList<>();
        logger.info("Unfiltered list size : " + listSitesUnfiltered.size());
        /*
         * Initial defaults:
         *   distance = 1
         *   distanceUnit = 2 // 1 = km, 2 = miles
         *   levelId = 3
         *   maxResults = 3
         */
        double distance = routeQueryData.getDistance();
        int distanceUnit = routeQueryData.getDistanceUnit();
        int levelId = routeQueryData.getLevelId();
        int maxResults = routeQueryData.getMaxResults();

        // Gets, decodes, and transforms the encoded polyline into a list of coordinate wrapper objects.
        String polyline = getRoutePolyline(routeQueryData);
        ArrayList<Location> routeCoords = decodePolyline(polyline);

        /* Measures a box around each coordinate set whose dimensions are specified by the distance parameter and removes
         * all coordinates that fall within that box. Repeats with the next coordinate set. Pre-filtering ensures that fewer
         * calls are made to the OpenChargeMap API.
         */
        routeCoords = filterLocationsByDistance(routeCoords, distance, distanceUnit);

        // Finds an array of charging sites within a specified distance of each coordinate set and adds the arrays to a list.
        for (int i = 0; i < routeCoords.size(); i++) {

            double latitude = routeCoords.get(i).getLat();
            double longitude = routeCoords.get(i).getLng();

            // Queries the external API for this set of coordinates in an anonymous Runnable managed by a ThreadPoolTaskExecutor
            // in order to speed up this method's response time.
            executor.execute(() -> {
                logger.info("Querying...");
                ChargingSite[] sites = openChargeMapService.searchByLatLong(latitude, longitude, distance, distanceUnit,
                        levelId, maxResults);
                logger.info("Query response retrieved.");

                if (sites.length > 0) {
                    logger.info("Attempting to add result with length " + sites.length + " to the list...");
                    listSitesUnfiltered.add(sites);
                    logger.info("Result successfully added.");
                } else
                    logger.info("Array size = 0. Not going to attempt to add anything to the list.");
            });
        }

        logger.info("Executor active count before blocking: " + executor.getActiveCount());

        // My crude solution for the case where active count = 0 but threads have not yet executed. Forces the "sites-get-x"
        // thread to sleep very briefly in case no "ocm-query-x" thread is currently active. Seems to work.
        if (executor.getActiveCount() == 0) {
            try {
                Thread.sleep(50l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Blocks while threads are still executing. Edge case seemingly solved above.
        while (executor.getActiveCount() > 0) {}

        logger.info("Executor active count after blocking: " + executor.getActiveCount());
        logger.info("Unfiltered list size: " + listSitesUnfiltered.size());
        ArrayList<ChargingSite> sitesFiltered = removeRepeatingElements(listSitesUnfiltered);
        logger.info("Filtered list size: " + sitesFiltered.size());
        logger.info("Returning result.");
        return CompletableFuture.completedFuture(sitesFiltered);
    }

    /**
     * Queries the Google Directions API for a route based on the input data and returns the encoded polyline detailing
     * the route.
     * @param routeQueryData
     * @return the encoded polyline which, when decoded, contains a list of coordinates detailing the route
     * @throws RouteNotFoundException
     */
    public String getRoutePolyline(RouteQueryData routeQueryData) throws RouteNotFoundException {

        double startLat = routeQueryData.getStartLat();
        double startLng = routeQueryData.getStartLng();
        double endLat = routeQueryData.getEndLat();
        double endLng = routeQueryData.getEndLng();
        String key = routeQueryData.getApiKey();

        String query = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLat + "," + startLng +
                "&destination=" + endLat + "," + endLng +
                "&key=" + key;
        Directions directions = restTemplate.getForObject(query, Directions.class);
        try {
            logger.info("Google Directions API successfully queried.");
            return directions.getRoutes()[0]
                    .getOverview_polyline()
                    .getPoints();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RouteNotFoundException();
        }
    }

    /**
     * Measures a box around each coordinate set whose dimensions are specified by the distance parameter, and removes all
     * coordinates that fall within that box. Repeats with the next coordinate set. Pre-filtering ensures that fewer calls
     * are made to the OpenChargeMap API.
     * @param locations -- the list of coordinates produced by decoding the polyline describing the route found by Google's
     *                  Directions API
     * @param distance -- the distance from a coordinate set to the edge of the box
     * @param unit -- 1 = km, 2 = miles
     * @return the new smaller list of coordinates post-filtering
     */
    ArrayList<Location> filterLocationsByDistance(ArrayList<Location> locations, double distance, int unit) {

        ArrayList<Location> filteredLocations = new ArrayList<>();
        if (unit == 2)
            distance *= 1.6;

        for (int i = 0, j = i + 1; i < locations.size() - 1; i = j) {

            Location iLoc = locations.get(i);
            filteredLocations.add(iLoc);

            // Gets the position in the list of the next location outside the required range.
            double iLat = iLoc.getLat();
            double iLng = iLoc.getLng();
            double[] latBounds = getMaxMinLats(iLat, distance);
            double[] lngBounds = getMaxMinLngs(iLat, iLng, distance);

            while (j < locations.size() - 1) {
                Location jLoc = locations.get(j);
                double jLat = jLoc.getLat();
                double jLng = jLoc.getLng();
                j++;
                // If lat or lng is out of range...
                if ((jLat > latBounds[0] || jLat < latBounds[1]) || (jLng > lngBounds[0] || jLng < lngBounds[1]))
                    break;
            }
        }

        Location last = locations.get(locations.size() - 1);
        filteredLocations.add(last);
        logger.info("Coordinates filtered out within a box around the original coordinates.");
        return filteredLocations;
    }

    private double[] getMaxMinLats(double lat, double distance) {

        double north = lat + (distance / DISTANCE_ONE_DEGREE_LATITUDE);
        double south = lat - (distance / DISTANCE_ONE_DEGREE_LATITUDE);
        if (north > 90)
            north = 90;
        if (south < -90)
            south = -90;
        return new double[] { north, south };
    }

    private double[] getMaxMinLngs(double lat, double lng, double distance) {

        distBtwnOneLngAtEachLat = buildListDistances(); // Result should already be cached.
        double[] maxMinLats = getMaxMinLats(lat, distance);
        int nearestLatNorth = (int) (maxMinLats[0]);
        int nearestLatSouth = (int) (maxMinLats[1]);
        double east = lng + (distance / distBtwnOneLngAtEachLat[nearestLatNorth]);
        double west = lng - (distance / distBtwnOneLngAtEachLat[nearestLatSouth]);
        east = setLngBounds(east);
        west = setLngBounds(west);
        return new double[] { east, west };
    }

    /**
     * Ensures that longitude values remain valid if the input exceeds +-180 degrees.
     * @param lng
     * @return the valid longitude value
     */
    private double setLngBounds(double lng) {

        if (lng > 180) {
            lng = 180 - lng % 180;
            if (lng != 180)
                lng *= -1;
        } else if (lng <= -180)
            lng = 180 - (lng * -1) % 180;
        return lng;
    }

    /**
     * Creates an array containing the distance between one degree longitude at each degree latitude (0-90).
     */
    @Cacheable("distances")
    public double[] buildListDistances() {

        double distances[] = new double[91];
        for (int i = 0; i <= 90; i++) {
            distances[i] = calcDistance(i);
        }
        return distances;
    }

    /**
     * Using the haversine formula for great circle distances, calculates the distance between one degree difference in
     * longitude at the supplied latitude. This method is intended to be executed for each degree latitude in order to
     * create a list of distances that will allow for quick distance approximation between and around coordinates.
     * @param lat -- the degree latitude
     * @return the distance in km
     */
    private double calcDistance(double lat) {

        double dLat = Math.toRadians(lat - lat);
        double dLon = Math.toRadians(1);
        lat = Math.toRadians(lat);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat) * Math.cos(lat);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c;
    }

    /**
     * Removes the repeating elements in an ArrayList of arrays of ChargingSite objects.
     * @param sitesAlongRouteRepeatingElements -- the ArrayList of ChargingSite arrays
     * @return an ArrayList of sites along the route
     */
    ArrayList<ChargingSite> removeRepeatingElements(ArrayList<ChargingSite[]> sitesAlongRouteRepeatingElements) {

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
