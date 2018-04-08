package ev_route_planner.exceptions;

import ev_route_planner.exceptions.RouteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ControllerAdviceClass {

    @ExceptionHandler(RouteNotFoundException.class)
    public @ResponseBody RouteNotFoundException routeNotFound(RouteNotFoundException e) {
        RouteNotFoundException error = new RouteNotFoundException();
        error.setMessage("A route cannot be found between these points.");
        error.setStatus(204);
        return error;
    }

    @ExceptionHandler(UserExistsException.class)
    public @ResponseBody UserExistsException userExists(UserExistsException e) {
        UserExistsException error = new UserExistsException();
        error.setMessage("This user already exists.");
        return error;
    }

    @ExceptionHandler(KeyDoesNotExistException.class)
    public @ResponseBody KeyDoesNotExistException keyDoesNotExistException(KeyDoesNotExistException e) {
        KeyDoesNotExistException error = new KeyDoesNotExistException();
        error.setMessage("Invalid key.");
        return error;
    }

    @ExceptionHandler(RateLimitException.class)
    public @ResponseBody RateLimitException rateLimitException(RateLimitException e) {
        RateLimitException error = new RateLimitException();
        error.setMessage("Too many API calls. Please try again later.");
        return error;
    }
}
