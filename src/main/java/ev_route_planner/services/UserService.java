package ev_route_planner.services;

import ev_route_planner.mappers.UserMapper;
import ev_route_planner.model.user.ApiCall;
import ev_route_planner.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    /**
     * Adds a new user to the database.
     * @param alias -- the username
     * @param apiKey -- the new API key
     * @return
     */
    public User registerUser(String alias, String apiKey) {
        User user = new User();
        user.setAlias(alias);
        user.setApiKey(apiKey);
        userMapper.addNewUser(user);
        return userMapper.findByAlias(alias);
    }

    /**
     * Checks if a user known by this alias already exists.
     * @param alias -- the username
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(String alias) {
        boolean userExists;
        try {
            userExists = (userMapper.findByAlias(alias) != null);
        } catch (NullPointerException e) {
            userExists = false;
        }
        return userExists;
    }

    /**
     * Checks if a specified API key has exceeded the rate limit.
     * @param apiKey
     * @return true if the API rate limit has been exceeded, false otherwise
     */
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
        }
        int countCallsPerMinute = 0;
        int countCallsPerDay = 0;
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

    /**
     * Adds a new API call with key and time to the database.
     * @param apiKey
     * @return the API call data just entered
     */
    public ApiCall addApiCall(String apiKey) {
        String currentDateTime = LocalDateTime.now().toString();
        userMapper.addApiCall(apiKey, currentDateTime);
        return userMapper.getCallByKeyAndDateTime(apiKey, currentDateTime);
    }

    /**
     * Checks if an API key is in the database.
     * @param apiKey
     * @return true if the key exists, false otherwise
     */
    public boolean keyExists(String apiKey) {
        boolean keyExists;
        try {
            keyExists = (userMapper.findByApiKey(apiKey) != null);
        } catch (NullPointerException e) {
            keyExists = false;
        }
        return keyExists;
    }

    /**
     * Generates an API key for the user.
     * @return the new API key
     */
    public String generateAPIKey() {
        String apiKey = UUID.randomUUID().toString();
        apiKey = apiKey.replaceAll("-", "");
        return apiKey;
    }
}
