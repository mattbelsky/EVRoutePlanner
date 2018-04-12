package ev_route_planner.services;

import ev_route_planner.mappers.UserMapper;
import ev_route_planner.model.user.ApiCall;
import ev_route_planner.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    // Adds a new user to the database.
    public User registerUser(String alias, String apiKey) {
        User user = new User();
        user.setAlias(alias);
        user.setApiKey(apiKey);

        // Ryan: there's a possibility of an exception here, should the network be down or the DB be offline
        // would be good to be prepared for that by having this method throw an exception that can be caught
        // by @ControllerAdvice
        userMapper.addNewUser(user);
        return userMapper.findByAlias(alias);
    }

    // Checks if a user known by this alias already exists.
    public boolean userExists(String alias) {
        boolean userExists;
        try {
            userExists = (userMapper.findByAlias(alias) != null);
        } catch (NullPointerException e) {
            userExists = false;
        }
        // Ryan: may also want to catch "Exception" here in a second catch just in case, same reason as above
        return userExists;
    }

    // Checks if a specified API key has exceeded the rate limit.
    public boolean apiCallsExceeded(String apiKey) {
        boolean callsExceeded = false;
        int limitPerMinute = 0;
        int limitPerDay = 0;
        ApiCall[] calls = null;
        // the current date and time at the server's location
        String currentDateTime = LocalDateTime.now().toString();
        // Queries database for a list of API calls using specified key
        // The table for calls, not user, is queried here, thus the need to handle a NullPointerException here.
        // After testing, not sure why this doesn't throw an exception when nothing to return.
        // Debug is not so helpful. Spring sorcery involved, I assume.
        try {
            calls = userMapper.getCallsByKey(apiKey);
            if (calls.length == 0) throw new NullPointerException(); // Added this as no NullPointException is thrown.
        } catch (NullPointerException e) {
            // Do nothing.
            // Ryan: why are you throwing this exception and then not doing anything? not sure I follow.
        }
        int countCallsPerMinute = 0;
        int countCallsPerDay = 0;
        // Compares the date and time of each dateTime field from the database to the current one -- limit exceeded?
        /*  QUESTION: While it seems simpler to get date and time from localDateTimeObj.toString().substring(), is it
            better/safer to get it from localDateTimeObj.getMonth(), .getMin(), etc. even though it's more verbose?
         */

        // Ryan: I think either way is fine.
        // Ryan: that said, I think you might want to consider just querying the DB and use the count() SQL function
        // to count the calls they've made today, or made this minute. Know what I mean?
        // For instance, "SELECT count(id) FROM `ev-route-planner`.`api-calls` WHERE apiKey = #{apiKey} AND date > 2018-04-12"
        // or something along those lines. Know what I mean? If you can work that out it will be much simpler.
        for (ApiCall call : calls) {
            String currentDate = currentDateTime.substring(0, 10);
            String callDate = call.getDateTime().substring(0, 10);
            String currentTime = currentDateTime.substring(11, 16);
            String callTime = call.getDateTime().substring(11, 16);
            if (currentDate.equals(callDate)) {
                if (countCallsPerDay++ >= limitPerDay) return callsExceeded = true;
            }
            if (currentTime.equals(callTime)) {
                if (countCallsPerMinute++ >= limitPerMinute) return callsExceeded = true;
            }
        }
        return callsExceeded;
    }

    // Adds a new API call with key and time to the database.
    public ApiCall addApiCall(String apiKey) {
        // Ryan: if the varchar can't be compared with ">" you may need to switch column to DateTime
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//        Date now = new Date();
//        sdf.format(now);

        String currentDateTime = LocalDateTime.now().toString();
        userMapper.addApiCall(apiKey, currentDateTime);
        return userMapper.getCallByKeyAndDateTime(apiKey, currentDateTime);
    }

    // Checks if an API key is in the database.
    public boolean keyExists(String apiKey) {
        boolean keyExists;
        try {
            keyExists = (userMapper.findByApiKey(apiKey) != null);
            // Ryan: instead of catching NullPointerException in these various cases - as above in this class
            // I'd maybe just catch Exception - as it will catch any exception that occurs - and you can still set
            // keyExists, for instance, to false
        } catch (NullPointerException e) {
            keyExists = false;
        }
        return keyExists;
    }

    // Generates an API key for the user.
    public String generateAPIKey() {
        String apiKey = UUID.randomUUID().toString();
        apiKey = apiKey.replaceAll("-", "");
        return apiKey;
    }
}
