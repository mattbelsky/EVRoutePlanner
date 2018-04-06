package ev_route_planner.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoutePlannerMapper {

    String GET_KEY = "SELECT * FROM `ev-route-planner`.`api-keys` WHERE id = #{id};";

    @Select(GET_KEY)
    public String getKey(int id);
}
