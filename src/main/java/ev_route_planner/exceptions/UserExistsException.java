package ev_route_planner.exceptions;

public class UserExistsException extends Exception {

    int status;
    String message;

    // Ryan: just FYI, you can use the "message" variable in the Exception class and skip it in all your custom exceptions

    /*

       public UserExistsException(String message){
            super(message);
       }

       // and it will all still work as you expect. You can do this in all your custom exception classes.
     */

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
