package ev_route_planner.services;

import ev_route_planner.model.open_charge_map.ChargingSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Callable;

public class QueryTask implements Callable<ChargingSite[]> {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    RestTemplate restTemplate;
    private double latitude;
    private double longitude;
    private double distance;
    private int distanceUnit;
    private int levelId;
    private int maxResults;

    public QueryTask(RestTemplate restTemplate, double latitude, double longitude, double distance, int distanceUnit, int levelId, int maxResults) {

        this.restTemplate = restTemplate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.distanceUnit = distanceUnit;
        this.levelId = levelId;
        this.maxResults = maxResults;
    }

    @Override
    public ChargingSite[] call() {

        String fullQuery = "https://api.openchargemap.io/v2/poi/?output=json" +
        "&latitude=" + latitude +
        "&longitude=" + longitude +
        "&distance=" + distance +
        "&distanceunit=" + distanceUnit +
        "&levelid=" + levelId +
        "&maxresults=" + maxResults + "&compact=true&verbose=false";
        logger.info("Querying OCM via Callable on thread " + Thread.currentThread().getName() + "...");
        ChargingSite[] sites = restTemplate.getForObject(fullQuery, ChargingSite[].class);
        logger.info("Query completed via Callable on thread " + Thread.currentThread().getName());
//        logger.info("OpenChargeMaps API successfully queried for coordinates (" + latitude + ", " + longitude + ") via Callable on thread "
//                + Thread.currentThread().getName() + ".");
        return sites;
    }
}
