package org.ershoupingtai.admin.controller;

import org.ershoupingtai.admin.mapper.UserInfoMapper;
import org.ershoupingtai.admin.mapper.UserLoginMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserController {

    private final UserInfoMapper userInfoMapper;
    private final UserLoginMapper userLoginMapper;

    public AdminUserController(UserInfoMapper userInfoMapper, UserLoginMapper userLoginMapper) {
        this.userInfoMapper = userInfoMapper;
        this.userLoginMapper = userLoginMapper;
    }

    @GetMapping("/admin/users")
    public String userList(Model model) {
        model.addAttribute("users", userInfoMapper.findAllWithLogin());
        return "admin/users";
    }

    @PostMapping("/admin/users/delete")
    public String deleteUser(@RequestParam("id") Integer id) {
        userInfoMapper.deleteById(id);
        userLoginMapper.deleteById(id);
        return "redirect:/admin/users";
    }
}
