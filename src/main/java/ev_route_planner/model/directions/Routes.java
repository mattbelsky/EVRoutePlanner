package ev_route_planner.model.directions;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Routes {

    OverviewPolyline overview_polyline;

    public OverviewPolyline getOverview_polyline() {
        return overview_polyline;
    }

    public void setOverview_polyline(OverviewPolyline overview_polyline) {
        this.overview_polyline = overview_polyline;
    }
}
