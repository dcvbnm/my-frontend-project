package org.ershoupingtai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface userloginmapper {

    @Select("SELECT COUNT(1) FROM dbo.UserLogin WHERE UserName = #{account} OR CAST(UserId AS VARCHAR(50)) = #{account}")
    Integer existsByAccount(@Param("account") String account);
}
