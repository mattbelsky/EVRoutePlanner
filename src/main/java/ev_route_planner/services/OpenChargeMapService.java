package ev_route_planner.services;

import ev_route_planner.mappers.RoutePlannerMapper;
import ev_route_planner.model.geolocation.Geolocation;
import ev_route_planner.model.geolocation.WifiAccessPoints;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenChargeMapService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RoutePlannerMapper routePlannerMapper;

    /**
     * Gets the user's approximate location via a request to the Google Geolocation API.
     * @return the Geolocation object containing the results of the the POST request to Google's API
     */
    public Geolocation locateDevice() {

        // The Google Geolocation API endpoint with API key
        String key = routePlannerMapper.getKey(1);
        String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=" + key;

        // The array of WifiAccessPoint objects containing data on available wifi networks is given an acceptable format
        // for a postForObject() request.
        LinkedMultiValueMap<String, WifiAccessPoints[]> request = new LinkedMultiValueMap<>();
        return restTemplate.postForObject(url, request, Geolocation.class);
    }

    /**
     * Searches for charging sites by country and returns a list of relevant ChargingSite objects.
     * @param countryCode -- the ISO country code
     * @param maxResults -- the maximum number of results to return for the given country
     * @return an array of ChargingSite objects
     */
    public ChargingSite[] searchByCountry(String countryCode, int maxResults) {

        String query = "https://api.openchargemap.io/v2/poi/?output=json&countrycode=" + countryCode +
                "&maxresults=" + maxResults + "&compact=true&verbose=false";
        return restTemplate.getForObject(query, ChargingSite[].class);
    }

    /**
     * Searches for charging sites by coordinates (lat/long) within a specified distance from it as described in miles
     * or kilometers.
     * @param latitude
     * @param longitude
     * @param distance -- the distance from the coordinates to search
     * @param distanceUnit -- 1 = km, 2 = mi.
     * @param levelID -- the charging station level: 1 = 120V, 2 = 240V, 3 = DC fast charging
     *                (https://pluginamerica.org/understanding-electric-vehicle-charging/)
     * @param maxResults -- the maximum number of results to return for the given coordinates
     * @return an array of ChargingSite objects
     */
    public ChargingSite[] searchByLatLong(double latitude, double longitude, double distance, int distanceUnit,
                                                             int levelID, int maxResults) {
        String fullQuery = "https://api.openchargemap.io/v2/poi/?output=json" +
                "&latitude=" + latitude +
                "&longitude=" + longitude +
                "&distance=" + distance +
                "&distanceunit=" + distanceUnit +
                "&levelid=" + levelID +
                "&maxresults=" + maxResults + "&compact=true&verbose=false";
        return restTemplate.getForObject(fullQuery, ChargingSite[].class);
    }

    /**
     * Searches for charging charging stations near the user.
     * @return an array of Charging Site objects
     */
    public ChargingSite[] searchNearMe() {

        // Gets the user's device's location and prepares the latitude & longitude arguments for insertion into the query.
        Geolocation userLocation = locateDevice();
        double latitude = userLocation.getLocation().getLat();
        double longitude = userLocation.getLocation().getLng();

        // distance, distanceUnit, and maxResults are predefined here
        return searchByLatLong(latitude, longitude, 1500, 2, 3, 100);
    }
}