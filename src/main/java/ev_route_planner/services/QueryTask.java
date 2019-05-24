package ev_route_planner.services;

import ev_route_planner.model.open_charge_map.ChargingSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
public class QueryTask {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    RestTemplate restTemplate;

    @Async
    public CompletableFuture<ChargingSite[]> call(double latitude, double longitude, double distance, int distanceUnit,
                                                  int levelId, int maxResults) {

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

        return CompletableFuture.completedFuture(sites);
    }
}
