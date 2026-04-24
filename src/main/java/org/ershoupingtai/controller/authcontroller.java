package org.ershoupingtai.controller;

import org.ershoupingtai.common.Result;
import org.ershoupingtai.config.LoginInterceptor;
import org.ershoupingtai.service.userloginservice;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class authcontroller {

    private final userloginservice userLoginService;

    public authcontroller(userloginservice userLoginService) {
        this.userLoginService = userLoginService;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        Object loginUser = request.getSession().getAttribute(LoginInterceptor.LOGIN_USER_KEY);
        if (loginUser != null) {
            return "redirect:/";
        }
        return "login";
    }

    @ResponseBody
    @PostMapping("/api/auth/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String account = body == null ? null : body.get("account");
        String password = body == null ? null : body.get("password");

        if (!userLoginService.canLogin(account, password)) {
            return Result.fail("账号不存在或密码错误");
        }

        String loginAccount = account.trim();
        request.getSession().setAttribute(LoginInterceptor.LOGIN_USER_KEY, loginAccount);

        Map<String, String> data = new HashMap<>();
        data.put("account", loginAccount);
        return Result.success(data);
    }

    @ResponseBody
    @PostMapping("/api/auth/logout")
    public Result<Void> logout(HttpServletRequest request) {
        request.getSession().removeAttribute(LoginInterceptor.LOGIN_USER_KEY);
        return Result.success();
    }
}
