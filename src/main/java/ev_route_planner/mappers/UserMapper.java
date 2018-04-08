package ev_route_planner.mappers;

import ev_route_planner.model.user.ApiCall;
import ev_route_planner.model.user.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;

@Mapper
public interface UserMapper {

    String SELECT_BY_ALIAS = "SELECT * FROM `ev-route-planner`.users WHERE alias = #{alias};";
    String ADD_NEW_USER  = "INSERT INTO `ev-route-planner`.`users` (`alias`, `apiKey`) VALUES (#{alias}, #{apiKey});";
    String SELECT_USER_BY_APIKEY = "SELECT * FROM `ev-route-planner`.users WHERE apiKey = #{apiKey};";
    String SELECT_CALL_BY_APIKEY = "SELECT * FROM `ev-route-planner`.`api-calls` WHERE apiKey = #{apiKey};";
    String ADD_NEW_CALL = "INSERT INTO `ev-route-planner`.`api-calls` (`apiKey`, `dateTime`) VALUES (#{arg0}, #{arg1});";
    String SELECT_CALL_BY_APIKEY_AND_DATETIME = "SELECT * FROM `ev-route-planner`.`api-calls` " +
            "WHERE apiKey = #{arg0} AND dateTime = #{arg1};";

    @Select(SELECT_BY_ALIAS)
    public User findByAlias(String alias);

    @Insert(ADD_NEW_USER)
    public int addNewUser(User user);

    @Select(SELECT_USER_BY_APIKEY)
    public User findByApiKey(String apiKey);

    @Select(SELECT_CALL_BY_APIKEY)
    public ApiCall[] getCallsByKey(String apiKey);

    @Insert(ADD_NEW_CALL)
    public int addApiCall(String apiKey, String currentDateTime);

    @Select(SELECT_CALL_BY_APIKEY_AND_DATETIME)
    public ApiCall getCallByKeyAndDateTime(String apiKey, String currentDateTime);
}
