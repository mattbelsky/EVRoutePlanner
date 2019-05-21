package ev_route_planner;

import ev_route_planner.services.RoutePlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements CommandLineRunner {

    @Autowired
    RoutePlannerService routePlannerService;

    @Override
    public void run(String... args) throws Exception {
        routePlannerService.buildListDistances();
    }
}
