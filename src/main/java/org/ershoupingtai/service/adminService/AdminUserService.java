package org.ershoupingtai.service.adminService;

import org.ershoupingtai.pojo.UserInfo;
import org.ershoupingtai.mapper.adminMapper.UserInfoMapper;
import org.ershoupingtai.mapper.adminMapper.UserLoginMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserService {

    private final UserInfoMapper userInfoMapper;
    private final UserLoginMapper userLoginMapper;

    public AdminUserService(UserInfoMapper userInfoMapper, UserLoginMapper userLoginMapper) {
        this.userInfoMapper = userInfoMapper;
        this.userLoginMapper = userLoginMapper;
    }

    public List<UserInfo> getUsersWithLogin() {
        return userInfoMapper.findAllWithLogin();
    }

    public void deleteUser(Integer id) {
        userInfoMapper.deleteById(id);
        userLoginMapper.deleteById(id);
    }
}