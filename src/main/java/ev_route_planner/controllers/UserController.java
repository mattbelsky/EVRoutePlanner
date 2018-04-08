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

    @GetMapping("/getakey")
    public User registerUser(@RequestParam(value = "alias") String alias) throws UserExistsException {
        if (userService.userExists(alias)) throw new UserExistsException();
        String apiKey = userService.generateAPIKey();
        return userService.registerUser(alias, apiKey);
    }
}
