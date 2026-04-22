package org.ershoupingtai.mapper.adminMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.pojo.UserLogin;

@Mapper
public interface UserLoginMapper extends BaseMapper<UserLogin> {

    @Select("SELECT * FROM UserLogin WHERE UserName = #{username} AND UserPassword = #{password}")
    UserLogin findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}
