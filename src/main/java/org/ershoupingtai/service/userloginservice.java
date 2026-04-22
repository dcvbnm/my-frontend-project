package org.ershoupingtai.service;

import org.ershoupingtai.mapper.userloginmapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class userloginservice {

    private final userloginmapper userLoginMapper;

    public userloginservice(userloginmapper userLoginMapper) {
        this.userLoginMapper = userLoginMapper;
    }

    public boolean canLogin(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return false;
        }
        if (!"123456".equals(password)) {
            return false;
        }
        Integer count = userLoginMapper.existsByAccount(account.trim());
        return count != null && count > 0;
    }
}
