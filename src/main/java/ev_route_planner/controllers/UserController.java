package ev_route_planner.controllers;

import ev_route_planner.exceptions.UserExistsException;
import ev_route_planner.model.user.User;
import ev_route_planner.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    // Ryan: typically, in a RESTful API, we don't want any verbs like "GET", "POST", "ADD", "CREATE" etc in our URLs
    // since we can use HTTP methods GET, PUT, POST, DELETE etc - it keeps our endpoints cleaner, easier to predict and work with
    @GetMapping("/getakey")
    public User registerUser(@RequestParam(value = "alias") String alias) throws UserExistsException {
        if (userService.userExists(alias)) throw new UserExistsException();
        String apiKey = userService.generateAPIKey();
        return userService.registerUser(alias, apiKey);
    }
}
