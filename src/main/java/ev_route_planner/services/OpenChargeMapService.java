package ev_route_planner.services;

import ev_route_planner.mappers.OpenChargeMapMapper;
import ev_route_planner.mappers.RoutePlannerMapper;
import ev_route_planner.model.geolocation.Geolocation;
import ev_route_planner.model.geolocation.WifiAccessPoints;
import ev_route_planner.model.open_charge_map.ChargingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class OpenChargeMapService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    OpenChargeMapMapper openChargeMapMapper;

    @Autowired
    RoutePlannerMapper routePlannerMapper;

    // Scans for available wifi networks and captures their BSSID, channel, and signal information.
    // Apparently this method is completely unnecessary. No idea why though.
//    public WifiAccessPoints[] getWifiNetworks() throws IOException {
//        // The system command to execute
//        String command = "nmcli -f BSSID,CHAN,SIGNAL dev wifi list";
//        // Assigns a reference to the Process being executed
//        Process p = Runtime.getRuntime().exec(command);
//        // Gets the input stream of p and wraps it with a buffered reader
//        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//        // ArrayLists containing details on the available wifi networks
//        ArrayList<String> bssid = new ArrayList<String>();
//        ArrayList<String> signal = new ArrayList<String>();
//        ArrayList<String> channel = new ArrayList<String>();
//        String line;
//
//        // Sets the values of the ArrayLists
//        try {
//            while (r.readLine() != null) {
//                line = r.readLine();
//                bssid.add(line.substring(0, 17));
//                signal.add(line.substring(25, 27));
//                channel.add(line.substring(19, 21));
//            }
//        }
//        catch (NullPointerException e) {
//            // Do nothing.
//        }
//
//        // Creates an array of objects containing data on the wifi networks
//        WifiAccessPoints[] wifiAccessPoints = new WifiAccessPoints[bssid.size()];
//
//        // Sets the instance variables of each object in this array with the values from the ArrayLists
//        for (int i = 0; i < wifiAccessPoints.length; i++) {
//            wifiAccessPoints[i] = new WifiAccessPoints();
//            wifiAccessPoints[i].setMacAddress(bssid.get(i));
//            wifiAccessPoints[i].setSignalStrength(Integer.parseInt(signal.get(i).trim()));
//            wifiAccessPoints[i].setChannel(Integer.parseInt(channel.get(i).trim()));
//        }
//
//        return wifiAccessPoints;
//    }

    // Calls the Google Geolocation API which returns the user's approximate location based on detected wifi networks.
    // Although I wrote it, this method works through some sorcery I don't understand.
    public Geolocation locateDevice(/*WifiAccessPoints[] wap*/) throws IOException {

        // The Google Geolocation API endpoint with API key
        String key = routePlannerMapper.getKey(1);
        String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=" + key;

        // The array of WifiAccessPoint objects containing data on available wifi networks is given an acceptable format
        // for a postForObject() request.
        LinkedMultiValueMap<String, WifiAccessPoints[]> request = new LinkedMultiValueMap<>();
//        WifiAccessPoints[] wifiAccessPoints = getWifiNetworks();

        /*  Why does the code below cause an error?? And how does the above work without a WifiAccessPoints[]
            array being passed?   */
//        for (int i = 0; i < wifiAccessPoints.length; i++) {
//            request.add("wifiAccessPoints", wifiAccessPoints[i]);
//        }

        Geolocation deviceLocation = restTemplate.postForObject(url, request, Geolocation.class);

        return deviceLocation;
    }

    // Searches by country and returns an array of ChargingSite objects from that country
    public ChargingSite[] searchByCountry(String countryCode, int maxResults) {
        String query = "https://api.openchargemap.io/v2/poi/?output=json&countrycode=" + countryCode +
                "&maxresults=" + maxResults + "&compact=true&verbose=false";
        ChargingSite[] chargingSites = restTemplate.getForObject(query, ChargingSite[].class);
        return chargingSites;
    }

    // Searches by latitude & longitude & distance in miles or km from it
    public ChargingSite[] searchByLatLong(double latitude, double longitude, double distance, int distanceUnit,
                                          int levelID, int maxResults) {
        String fullQuery = "https://api.openchargemap.io/v2/poi/?output=json" +
                "&latitude=" + latitude +
                "&longitude=" + longitude +
                "&distance=" + distance +
                "&distanceunit=" + distanceUnit +
                "&levelid=" + levelID +
                "&maxresults=" + maxResults + "&compact=true&verbose=false";
        ChargingSite[] chargingSites = restTemplate.getForObject(fullQuery, ChargingSite[].class);
        return chargingSites;
    }

    // Searches for charging stations near the user
    public ChargingSite[] searchNearMe() throws IOException {

        // Gets the user's device's location and prepares the latitude & longitude arguments for insertion into the query.
        Geolocation userLocation = locateDevice();
        double latitude = userLocation.getLocation().getLat();
        double longitude = userLocation.getLocation().getLng();

        // The array to return
        // distance, distanceUnit, and maxResults are predefined here
        ChargingSite[] sitesNearUser = searchByLatLong(latitude, longitude, 1500, 2, 2, 100);
        return sitesNearUser;

//        return locateDevice(/*getWifiNetworks()*/); // previously needed this return type
    }
}
