package ev_route_planner.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoutePlannerMapper {

    @Select("SELECT `apiKey` FROM `ev_route_planner`.`users` WHERE id = #{id};")
    public String getKey(int id);
}
