package org.ershoupingtai.service.adminService;

import org.ershoupingtai.pojo.UserInfo;
import org.ershoupingtai.mapper.adminMapper.UserInfoMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private final UserInfoMapper userInfoMapper;
    private final UserLoginMapper userLoginMapper;
    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(UserInfoMapper userInfoMapper, UserLoginMapper userLoginMapper, JdbcTemplate jdbcTemplate) {
        this.userInfoMapper = userInfoMapper;
        this.userLoginMapper = userLoginMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserInfo> getUsersWithLogin() {
        return userInfoMapper.findAllWithLogin();
    }

    public void deleteUser(Integer id) {
        userInfoMapper.deleteByUserId(id);
        userLoginMapper.deleteByUserId(id);
    }

    public Integer getAdminChatUserId() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT TOP 1 UserId FROM dbo.UserLogin WHERE UserName = ?",
                    Integer.class,
                    "1111111111"
            );
        } catch (Exception ex) {
            return null;
        }
    }
}