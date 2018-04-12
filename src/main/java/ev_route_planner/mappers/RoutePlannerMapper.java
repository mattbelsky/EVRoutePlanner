package ev_route_planner.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoutePlannerMapper {

    // Ryan: one more thing I'd add to the query (and table) is an isActive boolean field, as keys can often be disabled temporarily etc
    String GET_KEY = "SELECT `key` FROM `ev-route-planner`.`api-keys` WHERE id = #{id};";

    @Select(GET_KEY)
    public String getKey(int id);
}
