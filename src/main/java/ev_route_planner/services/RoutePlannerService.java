package ev_route_planner.services;

import ev_route_planner.exceptions.RouteNotFoundException;
import ev_route_planner.model.RouteQueryData;
import ev_route_planner.model.directions.Directions;
import ev_route_planner.model.geolocation.Location;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.concurrent.*;

@Service
public class RoutePlannerService {

    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM
    private static final int DISTANCE_ONE_DEGREE_LATITUDE = 111; // Approx distance between one degree difference in latitude in km.

    private double[] distBtwnOneLngAtEachLat;
    public ArrayList<ChargingSite[]> listSitesUnfiltered = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(RoutePlannerService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    OpenChargeMapService openChargeMapService;

    @Autowired
    QueryTask queryTask;

    @Autowired
    ThreadPoolTaskExecutor executor;

    /**
     * The Google Directions API is queried with a set of starting and ending coordinates, which plans the optimal route
     * and returns an encoded polyline. The polyline is decoded in another method into an array of coordinates, each of
     * which is filtered according to its distance from the previous coordinate and is used to query the OpenChargeMaps API
     * for EV charging sites nearby.
     * @param routeQueryData -- contains all the data needed to query for charging stations along a route
     * @return an ArrayList of ChargingSite objects found along the route
     */
    public ArrayList<ChargingSite[]> getChargingSites(RouteQueryData routeQueryData) throws RouteNotFoundException {

        distBtwnOneLngAtEachLat = buildListDistances(); // Result should already be cached.
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

        // Removes all coordinates from the list within the specified distance from the previous set. Pre-filtering ensures
        // that fewer calls are made to the OpenChargeMap API and results in a much faster response from this application.
        routeCoords = filterLocationsByDistance(routeCoords, distance, distanceUnit);

        // Finds an array of charging sites within a specified distance of each coordinate set and adds the arrays to an ArrayList.
        for (int i = 0; i < routeCoords.size(); i++) {

            double latitude = routeCoords.get(i).getLat();
            double longitude = routeCoords.get(i).getLng();

//            logger.info("Executing async task from main thread...");
//            CompletableFuture<ChargingSite[]> future = queryTask.call(latitude, longitude, distance, distanceUnit, levelId, maxResults);
//            logger.info("Async task executed. Attempting to build a list from the results of execution #" + (i + 1) + "...");
//
//            buildListSitesUnfiltered(future);
//            logger.info("List building function called.");

            executor.execute(() -> {
                String fullQuery = "https://api.openchargemap.io/v2/poi/?output=json" +
                        "&latitude=" + latitude +
                        "&longitude=" + longitude +
                        "&distance=" + distance +
                        "&distanceunit=" + distanceUnit +
                        "&levelid=" + levelId +
                        "&maxresults=" + maxResults + "&compact=true&verbose=false";

                logger.info("Querying...");
                ChargingSite[] sites = restTemplate.getForObject(fullQuery, ChargingSite[].class);
                logger.info("Query completed.");
                listSitesUnfiltered.add(sites);
                logger.info("Sites added to list.");
            });
        }

//        ArrayList<ChargingSite> sitesFiltered = removeRepeatingElements(listSitesUnfiltered);
        while (executor.getActiveCount() > 1) {
            try {
                Thread.sleep(50l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return listSitesUnfiltered;
    }

    /* Adds the result of the asynchronous task to a global (for now) list. Still taking too long though. Based on timestamps,
     * the external API call does not seem to execute until the CompletableFuture's get() is called. The calls on the separate
     * threads are thus not executing simultaneously but sequentially and no time is saved with multithreading. Figure this out!
     */
    public void buildListSitesUnfiltered(CompletableFuture<ChargingSite[]> future) {

        ChargingSite[] sites = new ChargingSite[0];

        try {
            logger.info("Getting query result with future.get()...");
            // API call takes ~ 0.3 seconds. get() is blocking, and API call does not seem to execute until this method is called. Why?
            sites = future.get();
            logger.info("Query result retrieved.");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (sites.length == 0)
            logger.info("Array size = 0. Not going to attempt to add anything to the list.\n");
        if (sites.length > 0) {
            logger.info("Attempting to add result with length " + sites.length + " to the list...");
            listSitesUnfiltered.add(sites);
            logger.info("Result successfully added\n");
        }
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

    // Filters each set of coordinates based on whether its latitude or longitude exceeds the max lats or lngs of the previous.
    // Essentially, adds a set to the list if it falls outside a box whose sides' lengths are each double the distance param.
    private ArrayList<Location> filterLocationsByDistance(ArrayList<Location> locations, double distance, int unit) {

        ArrayList<Location> filteredLocations = new ArrayList<>();
        if (unit == 2) // 1 = km, 2 = miles -- default is km
            distance *= 1.6;

        for (int i = 0, j = i + 1; i < locations.size() - 1; i = j) {

            Location iLoc = locations.get(i);
            filteredLocations.add(iLoc);

            // Gets the position in the list of the next location outside the required range.
            double iLat = iLoc.getLat();
            double iLng = iLoc.getLng();
            double[] latBounds = getMaxMinLats(iLat, distance);
            double[] lngBounds = getMaxMinLngs(iLat, iLng, distance);
//            double maxSumNE = Math.abs(latBounds[0]) + Math.abs(lngBounds[0]);
//            double maxSumNW = Math.abs(latBounds[0]) + Math.abs(lngBounds[1]);
//            double maxSumSW = Math.abs(latBounds[1]) + Math.abs(lngBounds[1]);
//            double maxSumSE = Math.abs(latBounds[1]) + Math.abs(lngBounds[0]);

            while (j < locations.size() - 1) {
                Location jLoc = locations.get(j);
                double jLat = jLoc.getLat();
                double jLng = jLoc.getLng();
//                double sumLatLng = Math.abs(jLat) + Math.abs(jLng);
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
    private ArrayList<ChargingSite> removeRepeatingElements(ArrayList<ChargingSite[]> sitesAlongRouteRepeatingElements) {

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
