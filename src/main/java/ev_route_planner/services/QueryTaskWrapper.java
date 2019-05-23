package ev_route_planner.services;

import ev_route_planner.model.open_charge_map.ChargingSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class QueryTaskWrapper implements Runnable {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    RestTemplate restTemplate;
    private double latitude;
    private double longitude;
    private double distance;
    private int distanceUnit;
    private int levelId;
    private int maxResults;
    public ChargingSite[] sites; // the result to be extracted
    public ThreadPoolTaskExecutor executor;

    public QueryTaskWrapper(ThreadPoolTaskExecutor executor, RestTemplate restTemplate, double latitude, double longitude,
                            double distance, int distanceUnit, int levelId, int maxResults) {

        this.executor = executor;
        this.restTemplate = restTemplate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.distanceUnit = distanceUnit;
        this.levelId = levelId;
        this.maxResults = maxResults;
    }

    @Override
    public void run() {

        QueryTask queryTask = new QueryTask(restTemplate, latitude, longitude, distance, distanceUnit, levelId, maxResults);
        FutureTask<ChargingSite[]> future = (FutureTask<ChargingSite[]>) (executor.submit(queryTask));
        try {
            logger.info("Querying OCM via Runnable on thread " + Thread.currentThread().getName() + "...");
            boolean done = false;
            while (!done) {
                done = future.isDone();
            }
            sites = future.get();
            logger.info("Query result retrieved via get() in Runnable on thread " + Thread.currentThread().getName());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
