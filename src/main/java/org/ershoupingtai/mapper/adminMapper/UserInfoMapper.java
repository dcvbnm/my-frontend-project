package org.ershoupingtai.mapper.adminMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.ershoupingtai.pojo.UserInfo;

import java.util.List;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    @Select("SELECT u.UserId, u.UserName, i.Avatar, i.College, i.Campus, i.Phone, i.Address, i.Score, i.CreatDate " +
            "FROM UserLogin u LEFT JOIN UserInfo i ON u.UserId = i.UserId ORDER BY u.UserId")
    List<UserInfo> findAllWithLogin();

    @Delete("DELETE FROM dbo.UserInfo WHERE UserId = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);
}
