package ev_route_planner.model;

import javax.validation.constraints.NotNull;

/**
 * Contains the data needed to query the API for a route.
 */
public class RouteQueryData {

    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private double distance;
    private int distanceUnit;
    private int levelId;
    private int maxResults;
    private String apiKey;

    public RouteQueryData(@NotNull double startLat,
                          @NotNull double startLng,
                          @NotNull double endLat,
                          @NotNull double endLng,
                          @NotNull double distance,
                          @NotNull int distanceUnit,
                          @NotNull int levelId,
                          @NotNull int maxResults,
                          @NotNull String apiKey) {
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.distance = distance;
        this.distanceUnit = distanceUnit;
        this.levelId = levelId;
        this.maxResults = maxResults;
        this.apiKey = apiKey;
    }

    public RouteQueryData() {
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLng() {
        return startLng;
    }

    public double getEndLat() {
        return endLat;
    }

    public double getEndLng() {
        return endLng;
    }

    public double getDistance() {
        return distance;
    }

    public int getDistanceUnit() {
        return distanceUnit;
    }

    public int getLevelId() {
        return levelId;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public String getApiKey() {
        return apiKey;
    }
}
