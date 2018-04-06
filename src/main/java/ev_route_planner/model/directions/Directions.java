package ev_route_planner.model.directions;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Directions {

    Routes[] routes;

    public Routes[] getRoutes() {
        return routes;
    }

    public void setRoutes(Routes[] routes) {
        this.routes = routes;
    }
}
