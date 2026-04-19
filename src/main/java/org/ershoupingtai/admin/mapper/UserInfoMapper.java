package org.ershoupingtai.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.ershoupingtai.admin.entity.UserInfo;

import java.util.List;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    @Select("SELECT u.UserId, u.UserName, i.Avatar, i.College, i.Campus, i.Phone, i.Address, i.Score, i.CreatDate " +
            "FROM UserLogin u LEFT JOIN UserInfo i ON u.UserId = i.UserId ORDER BY u.UserId")
    List<UserInfo> findAllWithLogin();
}
